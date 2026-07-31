package mihon.entry.interactions.book.document.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import mihon.entry.interactions.book.preparation.BookResourceContentKind
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt

internal val PROSE_IMAGE_RESOURCE_REQUIREMENT = BookResourceRequirement(
    acceptedMediaTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif"),
    maxBytes = 12 * 1024 * 1024,
    contentKind = BookResourceContentKind.RASTER_IMAGE,
)

internal val PROSE_FONT_RESOURCE_REQUIREMENT = BookResourceRequirement(
    acceptedMediaTypes = setOf(
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-opentype",
    ),
    maxBytes = 4 * 1024 * 1024,
    contentKind = BookResourceContentKind.FONT,
)

internal fun File.validateBookDocumentResource(
    mediaType: String?,
    requirement: BookResourceRequirement,
) {
    val normalizedMediaType = mediaType?.substringBefore(';')?.trim()?.lowercase()
    require(normalizedMediaType in requirement.acceptedMediaTypes) {
        "Required BOOK resource has unsupported media type $mediaType"
    }
    require(length() in 1..requirement.maxBytes.toLong()) {
        "Required BOOK resource exceeds its byte limit"
    }
    when (requirement.contentKind) {
        BookResourceContentKind.RASTER_IMAGE -> {
            val decoded = decodeValidatedProseImage(
                bytes = readBytes(),
                targetWidthPx = VALIDATION_IMAGE_DIMENSION,
                targetHeightPx = VALIDATION_IMAGE_DIMENSION,
            )
            decoded.recycle()
        }
        BookResourceContentKind.FONT -> createValidatedProseTypeface(this)
    }
}

internal fun decodeValidatedProseImage(
    bytes: ByteArray,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Bitmap {
    require(bytes.isNotEmpty() && bytes.size <= PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes)
    require(targetWidthPx > 0 && targetHeightPx > 0)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth in 1..MAX_IMAGE_SOURCE_DIMENSION)
    require(bounds.outHeight in 1..MAX_IMAGE_SOURCE_DIMENSION)
    val sourcePixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
    require(sourcePixels <= MAX_IMAGE_SOURCE_PIXELS)
    val sampleSize = proseImageSampleSize(
        sourceWidth = bounds.outWidth,
        sourceHeight = bounds.outHeight,
        targetWidth = targetWidthPx,
        targetHeight = targetHeightPx,
    )
    val bitmap = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: error("Unable to decode prose image")
    require(bitmap.width.toLong() * bitmap.height.toLong() <= MAX_DECODED_IMAGE_PIXELS)
    return bitmap
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

internal fun createValidatedProseTypeface(file: File): Typeface {
    require(file.length() in 1..PROSE_FONT_RESOURCE_REQUIREMENT.maxBytes.toLong())
    return Typeface.createFromFile(file)
}

private fun Int.nextPowerOfTwo(): Int {
    var value = 1
    while (value < this && value < 128) value *= 2
    return value
}

private const val MAX_IMAGE_SOURCE_DIMENSION = 32_768
private const val MAX_IMAGE_SOURCE_PIXELS = 64L * 1024L * 1024L
private const val MAX_DECODED_IMAGE_PIXELS = 4L * 1024L * 1024L
private const val VALIDATION_IMAGE_DIMENSION = 1_024
