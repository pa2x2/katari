package app.katari.entry.interactions.book.document.resource

import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit

/**
 * Reads what the image decoder doesn't, such as GIF, and draws vector images with Skia, which draws everything else on
 * desktop, and builds fonts as Skia typefaces; Skia's text layout falls back to the system's fonts for glyphs a font
 * lacks.
 */
internal object SkiaBookDocumentResourceDecoding : BookDocumentResourceDecoding {
    override fun readRasterSize(bytes: ByteArray): IntSize? = readImageDecoderSize(bytes) ?: runCatching {
        Codec.makeFromData(Data.makeFromBytes(bytes)).use { IntSize(it.width, it.height) }
    }.getOrNull()

    override fun decodeRaster(bytes: ByteArray, sampleSize: Int): ProseImage =
        decodeWithImageDecoder(bytes, sampleSize) ?: decodeWithSkia(bytes, sampleSize)

    override fun readSvgSize(svg: String): ProseImageHeader.Vector = svgDocument(svg).use { document ->
        val root = checkNotNull(document.root) { "Vector image has no SVG root" }
        ProseImageHeader.Vector(root.width.toCssPixels(), root.height.toCssPixels())
    }

    override fun renderSvg(svg: String, width: Int, height: Int): ProseImage = svgDocument(svg).use { document ->
        val root = checkNotNull(document.root) { "Vector image has no SVG root" }
        // Sized as AndroidSVG sizes its document: the root takes the size, and its view box scales into it.
        root.width = SVGLength(width.toFloat())
        root.height = SVGLength(height.toFloat())
        document.setContainerSize(width.toFloat(), height.toFloat())
        val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(width, height)) }
        Canvas(bitmap).use { document.render(it) }
        bitmap.toProseImage()
    }

    override fun fontFamily(sfnt: ByteArray): FontFamily {
        // Compose loads its font lazily and would draw an unreadable one in the default face; Skia rejects it here.
        requireNotNull(FontMgr.default.makeFromData(Data.makeFromBytes(sfnt))) { "Unable to read font" }.close()
        return FontFamily(Font(identity = "book-font-${sfnt.contentHashCode()}-${sfnt.size}", data = sfnt))
    }
}

private fun decodeWithSkia(bytes: ByteArray, sampleSize: Int): ProseImage {
    val full = Codec.makeFromData(Data.makeFromBytes(bytes)).use { it.readPixels() }
    if (sampleSize == 1) return full.toProseImage()
    // Skia's codecs decode only at full size, so a sampled image is scaled down from it.
    val sampled = Bitmap().apply {
        allocPixels(ImageInfo.makeN32Premul(full.width / sampleSize, full.height / sampleSize))
    }
    try {
        Image.makeFromBitmap(full).use { image ->
            val pixels = checkNotNull(sampled.peekPixels()) { "Unable to scale prose image" }
            check(image.scalePixels(pixels, SamplingMode.LINEAR, false)) { "Unable to scale prose image" }
        }
    } catch (error: Exception) {
        sampled.close()
        throw error
    } finally {
        full.close()
    }
    return sampled.toProseImage()
}

private fun Bitmap.toProseImage() = ProseImage(asComposeImageBitmap(), ::close)

private fun svgDocument(svg: String) = SVGDOM(Data.makeFromBytes(svg.toByteArray()))

/** The length in CSS pixels, at 96 per inch as AndroidSVG reads it; null for a percentage, which has no size alone. */
private fun SVGLength.toCssPixels(): Float? {
    val pixels = when (unit) {
        SVGLengthUnit.NUMBER, SVGLengthUnit.PX -> value
        SVGLengthUnit.IN -> value * CSS_PIXELS_PER_INCH
        SVGLengthUnit.CM -> value * CSS_PIXELS_PER_INCH / 2.54f
        SVGLengthUnit.MM -> value * CSS_PIXELS_PER_INCH / 25.4f
        SVGLengthUnit.PT -> value * CSS_PIXELS_PER_INCH / 72f
        SVGLengthUnit.PC -> value * CSS_PIXELS_PER_INCH / 6f
        SVGLengthUnit.EMS -> value * DEFAULT_FONT_SIZE
        SVGLengthUnit.EXS -> value * DEFAULT_FONT_SIZE / 2f
        SVGLengthUnit.PERCENTAGE, SVGLengthUnit.UNKNOWN -> return null
    }
    return pixels.takeIf { it.isFinite() && it > 0f }
}

private const val CSS_PIXELS_PER_INCH = 96f
private const val DEFAULT_FONT_SIZE = 16f
