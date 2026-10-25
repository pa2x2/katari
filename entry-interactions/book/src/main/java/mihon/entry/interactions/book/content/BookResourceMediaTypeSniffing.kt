package mihon.entry.interactions.book.content

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

/**
 * Media types recognised from a resource's first bytes, for resources whose source declared none.
 *
 * Image and font hosts often answer with `application/octet-stream` or a wrong `Content-Type`, so the bytes
 * decide for the image and font types a document can use; anything else falls back to the response's type.
 */
internal fun sniffBookResourceMediaType(head: ByteArray): String? = when {
    head.startsWith(JPEG_SIGNATURE) -> "image/jpeg"
    head.startsWith(PNG_SIGNATURE) -> "image/png"
    head.startsWith(GIF87_SIGNATURE) || head.startsWith(GIF89_SIGNATURE) -> "image/gif"
    head.startsWith(RIFF_SIGNATURE) && head.startsWith(WEBP_SIGNATURE, offset = 8) -> "image/webp"
    head.isAvifImage() -> "image/avif"
    head.startsWith(TRUETYPE_SIGNATURE) || head.startsWith(APPLE_TRUETYPE_SIGNATURE) -> "font/ttf"
    head.startsWith(OPENTYPE_SIGNATURE) -> "font/otf"
    head.isSvgDocument() -> "image/svg+xml"
    else -> null
}

/** Reads the sniffing window and returns a stream that still yields every byte from the start. */
internal fun InputStream.peekBookResourceHead(): Pair<ByteArray, InputStream> {
    val buffered = BufferedInputStream(this, maxOf(DEFAULT_BUFFER_SIZE, SNIFF_LENGTH))
    buffered.mark(SNIFF_LENGTH)
    val head = ByteArray(SNIFF_LENGTH)
    var read = 0
    while (read < SNIFF_LENGTH) {
        val count = buffered.read(head, read, SNIFF_LENGTH - read)
        if (count < 0) break
        read += count
    }
    buffered.reset()
    return head.copyOf(read) to buffered
}

internal fun File.readBookResourceHead(): ByteArray = inputStream().use { input ->
    val head = ByteArray(SNIFF_LENGTH)
    var read = 0
    while (read < SNIFF_LENGTH) {
        val count = input.read(head, read, SNIFF_LENGTH - read)
        if (count < 0) break
        read += count
    }
    head.copyOf(read)
}

// An HTML page can contain inline <svg> early on, so only a document whose root element is <svg> counts.
private fun ByteArray.isSvgDocument(): Boolean {
    var text = decodeToString().removePrefix("﻿").trimStart()
    while (true) {
        text = when {
            text.startsWith("<?") -> text.substringAfter("?>", "")
            text.startsWith("<!--") -> text.substringAfter("-->", "")
            text.startsWith("<!") -> text.substringAfter('>', "")
            else -> break
        }.trimStart()
    }
    return text.startsWith("<svg", ignoreCase = true) &&
        text.getOrNull(4)?.let { it == '>' || it == '/' || it.isWhitespace() } == true
}

// An ISO-BMFF file starts with its ftyp box; AVIF names avif (still) or avis (sequence) as its major
// brand or, after a generic one such as mif1, among the compatible brands. HEIC files share the box.
private fun ByteArray.isAvifImage(): Boolean {
    if (!startsWith(FTYP_SIGNATURE, offset = 4)) return false
    val boxSize = ((this[0].toInt() and 0xFF) shl 24) or ((this[1].toInt() and 0xFF) shl 16) or
        ((this[2].toInt() and 0xFF) shl 8) or (this[3].toInt() and 0xFF)
    // A size of 0 means the box runs to the end of the file.
    val brandsEnd = if (boxSize == 0) size else minOf(boxSize, size)
    // Major brand at 8, minor version at 12, compatible brands from 16.
    return (sequenceOf(8) + (16 until brandsEnd step 4)).any { offset ->
        offset + 4 <= brandsEnd && (startsWith(AVIF_BRAND, offset) || startsWith(AVIS_BRAND, offset))
    }
}

private fun ByteArray.startsWith(signature: ByteArray, offset: Int = 0): Boolean =
    size >= offset + signature.size && signature.indices.all { this[offset + it] == signature[it] }

private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private val JPEG_SIGNATURE = bytes(0xFF, 0xD8, 0xFF)
private val PNG_SIGNATURE = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val GIF87_SIGNATURE = "GIF87a".encodeToByteArray()
private val GIF89_SIGNATURE = "GIF89a".encodeToByteArray()
private val RIFF_SIGNATURE = "RIFF".encodeToByteArray()
private val WEBP_SIGNATURE = "WEBP".encodeToByteArray()
private val TRUETYPE_SIGNATURE = bytes(0x00, 0x01, 0x00, 0x00)
private val APPLE_TRUETYPE_SIGNATURE = "true".encodeToByteArray()
private val OPENTYPE_SIGNATURE = "OTTO".encodeToByteArray()
private val FTYP_SIGNATURE = "ftyp".encodeToByteArray()
private val AVIF_BRAND = "avif".encodeToByteArray()
private val AVIS_BRAND = "avis".encodeToByteArray()

// Long enough for an XML declaration, a comment and a doctype ahead of an SVG root.
private const val SNIFF_LENGTH = 1024
