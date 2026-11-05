package mihon.entry.interactions.book.document.resource

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import mihon.core.common.platform.PlatformFacility
import mihon.entry.interactions.book.document.resource.font.unpackWebFont
import mihon.entry.interactions.book.preparation.BookResourceContentKind
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import java.io.File

internal val PROSE_IMAGE_RESOURCE_REQUIREMENT = BookResourceRequirement(
    acceptedMediaTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "image/svg+xml"),
    maxBytes = 12 * 1024 * 1024,
    contentKind = BookResourceContentKind.DOCUMENT_IMAGE,
)

internal val PROSE_FONT_RESOURCE_REQUIREMENT = BookResourceRequirement(
    acceptedMediaTypes = setOf(
        "font/ttf",
        "font/otf",
        "application/font-sfnt",
        "application/vnd.ms-opentype",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "font/woff",
        "font/woff2",
        "application/font-woff",
        "application/font-woff2",
        "application/x-font-woff",
    ),
    maxBytes = 4 * 1024 * 1024,
    contentKind = BookResourceContentKind.FONT,
)

/**
 * What reading a book's images and fonts needs from the platform: raster images, which both platforms read through
 * [readImageDecoderSize] and [decodeWithImageDecoder] before their own decoder takes the formats the image decoder
 * doesn't read, such as GIF; the drawing of vector images; and the platform's own font objects. Each throws for data
 * it can't use.
 */
@PlatformFacility
internal interface BookDocumentResourceDecoding {
    /** The size of a raster image; null when neither the image decoder nor the platform reads it. */
    fun readRasterSize(bytes: ByteArray): IntSize?

    /** Decodes a raster image at 1/[sampleSize] of its size, a power of two. */
    fun decodeRaster(bytes: ByteArray, sampleSize: Int): ProseImage

    /** The size [svg], a sanitized SVG document, declares for itself, in CSS pixels. */
    fun readSvgSize(svg: String): ProseImageHeader.Vector

    /** Draws [svg] scaled to [width] by [height] pixels. */
    fun renderSvg(svg: String, width: Int, height: Int): ProseImage

    /**
     * A family of the one font in [sfnt], a TrueType or OpenType font, falling back to the system's font for glyphs it
     * lacks, as a browser would.
     */
    fun fontFamily(sfnt: ByteArray): FontFamily
}

internal fun File.validateBookDocumentResource(
    mediaType: String?,
    requirement: BookResourceRequirement,
    decoding: BookDocumentResourceDecoding,
) {
    val normalizedMediaType = mediaType?.substringBefore(';')?.trim()?.lowercase()
    require(normalizedMediaType in requirement.acceptedMediaTypes) {
        "Required BOOK resource has unsupported media type $mediaType"
    }
    require(length() in 1..requirement.maxBytes.toLong()) {
        "Required BOOK resource exceeds its byte limit"
    }
    when (requirement.contentKind) {
        BookResourceContentKind.DOCUMENT_IMAGE -> decodeValidatedProseImage(
            bytes = readBytes(),
            mediaType = normalizedMediaType,
            targetWidthPx = VALIDATION_IMAGE_DIMENSION,
            targetHeightPx = VALIDATION_IMAGE_DIMENSION,
            decoding = decoding,
        ).release()
        BookResourceContentKind.FONT -> decoding.fontFamily(readBytes().let { unpackWebFont(it) ?: it })
    }
}

// Images are checked by decoding them as a figure about this wide would, every pixel read
private const val VALIDATION_IMAGE_DIMENSION = 1_024
