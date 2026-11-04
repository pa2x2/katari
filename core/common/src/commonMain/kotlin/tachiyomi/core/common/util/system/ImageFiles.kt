package tachiyomi.core.common.util.system

import java.io.InputStream

/**
 * Whether [name] is an image's, by its extension or else by the first bytes of what [openStream] opens. On Android
 * this is `ImageUtil.isImage`.
 */
expect fun isImageFile(name: String?, openStream: (() -> InputStream)? = null): Boolean

/**
 * The extension an image file is named with, from the format the first bytes [stream] reads show, or `null` when they
 * aren't an image's. On Android this is `ImageUtil.findImageType`.
 */
expect fun imageFileExtension(stream: InputStream): String?

/** The extension for an image of type [mime], or else of what [openStream] opens, or `jpg` when neither tells. */
fun imageFileExtension(mime: String?, openStream: () -> InputStream): String =
    IMAGE_MIME_EXTENSIONS[mime] ?: openStream().use(::imageFileExtension) ?: "jpg"

// As Android's ImageUtil.ImageType lists them
private val IMAGE_MIME_EXTENSIONS = mapOf(
    "image/avif" to "avif",
    "image/gif" to "gif",
    "image/heif" to "heif",
    "image/jpeg" to "jpg",
    "image/jxl" to "jxl",
    "image/png" to "png",
    "image/webp" to "webp",
)
