package mihon.entry.interactions.book.document.resource

import mihon.core.common.platform.PlatformFacility
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

/** Whether a book's image or font is one the platform's book reader can draw. Both throw when it isn't. */
@PlatformFacility
internal interface BookDocumentResourceDecoding {
    /** [mediaType] is lowercase and without parameters. */
    fun requireDecodableImage(bytes: ByteArray, mediaType: String?)

    fun requireLoadableFont(file: File)
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
        BookResourceContentKind.DOCUMENT_IMAGE -> decoding.requireDecodableImage(readBytes(), normalizedMediaType)
        BookResourceContentKind.FONT -> decoding.requireLoadableFont(this)
    }
}
