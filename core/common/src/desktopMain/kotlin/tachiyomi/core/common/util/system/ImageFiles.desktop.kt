package tachiyomi.core.common.util.system

import java.io.IOException
import java.io.InputStream

/** Recognises the formats Android's `ImageUtil.ImageType` lists, by the same extensions and by their signatures. */
actual fun isImageFile(name: String?, openStream: (() -> InputStream)?): Boolean {
    if (name == null) return false

    val extension = name.substringAfterLast('.')
    return extension in IMAGE_EXTENSIONS || openStream?.let { open -> open().use(::hasImageSignature) } == true
}

private fun hasImageSignature(stream: InputStream): Boolean {
    val header = try {
        stream.readNBytes(SIGNATURE_BYTES)
    } catch (_: IOException) {
        return false
    }
    return header.startsWith(JPEG) ||
        header.startsWith(PNG) ||
        header.startsWith(GIF) ||
        header.startsWith(JXL_CODESTREAM) ||
        header.startsWith(JXL_CONTAINER) ||
        (header.startsWith(RIFF) && header.startsWith(WEBP, offset = 8)) ||
        (header.startsWith(FTYP, offset = 4) && HEIF_BRANDS.any { header.startsWith(it, offset = 8) })
}

private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean =
    size >= offset + prefix.size && prefix.indices.all { this[offset + it] == prefix[it] }

private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private val IMAGE_EXTENSIONS = setOf("avif", "gif", "heif", "jpg", "jxl", "png", "webp")

private const val SIGNATURE_BYTES = 16

private val JPEG = bytes(0xFF, 0xD8, 0xFF)
private val PNG = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val GIF = "GIF8".toByteArray()
private val JXL_CODESTREAM = bytes(0xFF, 0x0A)
private val JXL_CONTAINER = bytes(0x00, 0x00, 0x00, 0x0C, 0x4A, 0x58, 0x4C, 0x20, 0x0D, 0x0A, 0x87, 0x0A)
private val RIFF = "RIFF".toByteArray()
private val WEBP = "WEBP".toByteArray()
private val FTYP = "ftyp".toByteArray()

// The ISO base media file brands of AVIF and HEIF images and image sequences
private val HEIF_BRANDS = listOf("avif", "avis", "heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1")
    .map(String::toByteArray)
