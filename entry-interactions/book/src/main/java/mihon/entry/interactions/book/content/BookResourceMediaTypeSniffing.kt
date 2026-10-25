package mihon.entry.interactions.book.content

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

/**
 * Media types recognised from a resource's first bytes, for resources whose source declared none.
 *
 * Image hosts often answer with `application/octet-stream` or a wrong `Content-Type`, so the bytes
 * decide for the image types a document can show; anything else falls back to the response's type.
 */
internal fun sniffBookResourceMediaType(head: ByteArray): String? = when {
    head.startsWith(JPEG_SIGNATURE) -> "image/jpeg"
    head.startsWith(PNG_SIGNATURE) -> "image/png"
    head.startsWith(GIF87_SIGNATURE) || head.startsWith(GIF89_SIGNATURE) -> "image/gif"
    head.startsWith(RIFF_SIGNATURE) && head.startsWith(WEBP_SIGNATURE, offset = 8) -> "image/webp"
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

private fun ByteArray.startsWith(signature: ByteArray, offset: Int = 0): Boolean =
    size >= offset + signature.size && signature.indices.all { this[offset + it] == signature[it] }

private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private val JPEG_SIGNATURE = bytes(0xFF, 0xD8, 0xFF)
private val PNG_SIGNATURE = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val GIF87_SIGNATURE = "GIF87a".encodeToByteArray()
private val GIF89_SIGNATURE = "GIF89a".encodeToByteArray()
private val RIFF_SIGNATURE = "RIFF".encodeToByteArray()
private val WEBP_SIGNATURE = "WEBP".encodeToByteArray()

// Long enough for an XML declaration, a comment and a doctype ahead of an SVG root.
private const val SNIFF_LENGTH = 1024
