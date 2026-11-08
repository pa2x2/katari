package app.katari.entry.interactions.book.document.resource

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.asImageBitmap
import app.katari.core.image.decode.ImageRegionDecoder
import app.katari.core.image.release
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A book's image decoded for drawing. Its pixels stay allocated until [release], which a figure calls once it no
 * longer shows the image.
 */
internal class ProseImage(val bitmap: ImageBitmap, private val onRelease: () -> Unit) {
    fun release() = onRelease()
}

/**
 * Decodes a book's image to fit [targetWidthPx] by [targetHeightPx], never larger than its own size and never more
 * pixels than a figure may hold. Throws for an image over the source limits or one that can't be decoded.
 */
internal fun decodeValidatedProseImage(
    bytes: ByteArray,
    mediaType: String? = null,
    targetWidthPx: Int,
    targetHeightPx: Int,
    decoding: BookDocumentResourceDecoding,
): ProseImage {
    require(bytes.isNotEmpty() && bytes.size <= PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes)
    require(targetWidthPx > 0 && targetHeightPx > 0)
    if (mediaType.isSvg()) {
        val svg = sanitizedSvg(bytes)
        val (width, height) = decoding.readSvgSize(svg).decodedSize(targetWidthPx, targetHeightPx)
        require(width.toLong() * height.toLong() <= MAX_DECODED_IMAGE_PIXELS)
        return decoding.renderSvg(svg, width, height)
    }
    val size = decoding.readRasterSize(bytes) ?: error("Unable to read prose image")
    requireWithinSourceLimits(size.width, size.height)
    val sampleSize = proseImageSampleSize(size.width, size.height, targetWidthPx, targetHeightPx)
    val image = decoding.decodeRaster(bytes, sampleSize)
    if (image.bitmap.width.toLong() * image.bitmap.height.toLong() > MAX_DECODED_IMAGE_PIXELS) {
        image.release()
        error("Decoded prose image exceeds its pixel limit")
    }
    return image
}

/**
 * An image's size as its header declares it, read without decoding any pixels, so a figure can take its final
 * size before its bitmap exists.
 */
internal sealed interface ProseImageHeader {
    /** The size [decodeValidatedProseImage] gives this image for the same target, give or take a pixel of rounding. */
    fun decodedSize(targetWidthPx: Int, targetHeightPx: Int): ProseImageSize

    data class Raster(val width: Int, val height: Int) : ProseImageHeader {
        override fun decodedSize(targetWidthPx: Int, targetHeightPx: Int): ProseImageSize {
            val sampleSize = proseImageSampleSize(width, height, targetWidthPx, targetHeightPx)
            return ProseImageSize((width / sampleSize).coerceAtLeast(1), (height / sampleSize).coerceAtLeast(1))
        }
    }

    /** A dimension the vector image leaves undeclared, as null, takes the target's. */
    data class Vector(val width: Float?, val height: Float?) : ProseImageHeader {
        override fun decodedSize(targetWidthPx: Int, targetHeightPx: Int): ProseImageSize {
            val sourceWidth = width ?: targetWidthPx.toFloat()
            val sourceHeight = height ?: targetHeightPx.toFloat()
            val scale = minOf(targetWidthPx / sourceWidth, targetHeightPx / sourceHeight).coerceAtMost(1f)
            return ProseImageSize(
                (sourceWidth * scale).toInt().coerceIn(1, targetWidthPx),
                (sourceHeight * scale).toInt().coerceIn(1, targetHeightPx),
            )
        }
    }
}

internal data class ProseImageSize(val width: Int, val height: Int)

/** Reads the header of an image [decodeValidatedProseImage] accepts, under the same source limits. */
internal fun readProseImageHeader(
    bytes: ByteArray,
    mediaType: String? = null,
    decoding: BookDocumentResourceDecoding,
): ProseImageHeader {
    require(bytes.isNotEmpty() && bytes.size <= PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes)
    if (mediaType.isSvg()) return decoding.readSvgSize(sanitizedSvg(bytes))
    val size = decoding.readRasterSize(bytes) ?: error("Unable to read prose image")
    requireWithinSourceLimits(size.width, size.height)
    return ProseImageHeader.Raster(size.width, size.height)
}

/**
 * The size of a raster image the image decoder the manga reader uses reads: every format a book may hold but GIF.
 * Null for any other format.
 */
internal fun readImageDecoderSize(bytes: ByteArray): IntSize? =
    ImageRegionDecoder.open(bytes.inputStream())?.use { IntSize(it.width, it.height) }

/** Decodes an image [readImageDecoderSize] reads at 1/[sampleSize] of its size; null for any other format. */
internal fun decodeWithImageDecoder(bytes: ByteArray, sampleSize: Int): ProseImage? =
    ImageRegionDecoder.open(bytes.inputStream())?.use { decoder ->
        val bitmap = decoder.decode(IntRect(0, 0, decoder.width, decoder.height), sampleSize)
            ?: error("Unable to decode prose image")
        ProseImage(bitmap.asImageBitmap(), bitmap::release)
    }

internal fun proseImageSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): Int {
    require(sourceWidth > 0 && sourceHeight > 0)
    require(targetWidth > 0 && targetHeight > 0)
    val sourcePixels = sourceWidth.toLong() * sourceHeight.toLong()
    val pixelScale = sqrt(sourcePixels.toDouble() / MAX_DECODED_IMAGE_PIXELS.toDouble())
    val widthScale = sourceWidth.toDouble() / targetWidth
    val heightScale = sourceHeight.toDouble() / targetHeight
    return ceil(maxOf(1.0, pixelScale, widthScale, heightScale))
        .toInt()
        .nextPowerOfTwo()
}

private fun String?.isSvg(): Boolean = this?.substringBefore(';')?.trim()?.lowercase() == "image/svg+xml"

private fun requireWithinSourceLimits(width: Int, height: Int) {
    require(width in 1..MAX_IMAGE_SOURCE_DIMENSION)
    require(height in 1..MAX_IMAGE_SOURCE_DIMENSION)
    require(width.toLong() * height.toLong() <= MAX_IMAGE_SOURCE_PIXELS)
}

/** The SVG document without scripts, animation, embedded media, or references to anything outside it. */
private fun sanitizedSvg(bytes: ByteArray): String {
    require(bytes.size <= MAX_SVG_BYTES) { "Vector image exceeds its byte limit" }
    val document = org.jsoup.Jsoup.parse(bytes.toString(Charsets.UTF_8), "", org.jsoup.parser.Parser.xmlParser())
    val root = document.selectFirst("svg") ?: error("Vector image has no SVG root")
    root.select("script, foreignObject, animate, animateMotion, animateTransform, set, audio, video").remove()
    root.getAllElements().forEach { element ->
        element.attributes().asList().forEach { attribute ->
            val key = attribute.key.lowercase()
            val value = attribute.value.trim()
            val localReference = value.startsWith("#")
            if (
                key.startsWith("on") ||
                (key in setOf("href", "xlink:href") && value.isNotEmpty() && !localReference) ||
                value.contains("javascript:", true) ||
                value.contains("url(", true)
            ) {
                element.removeAttr(attribute.key)
            }
        }
    }
    return root.outerHtml()
}

private fun Int.nextPowerOfTwo(): Int {
    var value = 1
    while (value < this && value < 128) value *= 2
    return value
}

private const val MAX_IMAGE_SOURCE_DIMENSION = 32_768
private const val MAX_IMAGE_SOURCE_PIXELS = 64L * 1024L * 1024L
private const val MAX_DECODED_IMAGE_PIXELS = 4L * 1024L * 1024L
private const val MAX_SVG_BYTES = 1024 * 1024
