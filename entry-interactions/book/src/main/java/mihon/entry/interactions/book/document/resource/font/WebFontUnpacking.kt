package mihon.entry.interactions.book.document.resource.font

/**
 * Android's font loader reads TrueType and OpenType only, while web fonts mostly arrive as WOFF or WOFF2.
 * Returns the TrueType/OpenType font a WOFF or WOFF2 file wraps, or null for any other data.
 */
internal fun unpackWebFont(bytes: ByteArray): ByteArray? = when {
    bytes.startsWithTag("wOFF") -> WoffFontDecoder.decode(bytes, MAX_UNPACKED_FONT_BYTES)
    bytes.startsWithTag("wOF2") -> Woff2FontDecoder.decode(bytes, MAX_UNPACKED_FONT_BYTES)
    else -> null
}

private fun ByteArray.startsWithTag(tag: String): Boolean =
    size >= 4 && tag.indices.all { this[it] == tag[it].code.toByte() }

// A 4 MiB web font unpacks to well under this; the cap stops a crafted file from claiming gigabytes.
private const val MAX_UNPACKED_FONT_BYTES = 16 * 1024 * 1024
