package app.katari.extension.runtime.jvm.apk.binary

/**
 * The strings of a compiled resource file (`ResStringPool`), by index, in UTF-8 or UTF-16 as the pool's flag says.
 * Strings are decoded when first asked for.
 */
internal class ResourceStringPool(private val chunk: ResourceChunk) {

    private val count = chunk.u32(8).toInt()
    private val isUtf8 = chunk.u32(16) and UTF8_FLAG != 0L
    private val stringsStart = chunk.u32(20).toInt()
    private val decoded = arrayOfNulls<String>(count)

    init {
        require(chunk.type == ResourceChunk.STRING_POOL) { "Not a string pool" }
    }

    /** The string at [index]; `null` for the "no string" index (`0xffffffff`) or one outside the pool. */
    operator fun get(index: Long): String? {
        if (index !in 0 until count) return null
        val i = index.toInt()
        decoded[i]?.let { return it }
        val offset = stringsStart + chunk.u32(chunk.headerSize + i * 4).toInt()
        return (if (isUtf8) utf8At(offset) else utf16At(offset)).also { decoded[i] = it }
    }

    private fun utf8At(offset: Int): String {
        // The length in UTF-16 units, then in bytes, each one byte or, with the high bit set, two
        val byteLengthAt = offset + lengthSize8(offset)
        val bytesAt = byteLengthAt + lengthSize8(byteLengthAt)
        val byteLength = length8(byteLengthAt)
        return ByteArray(byteLength) { chunk.buffer.get(chunk.start + bytesAt + it) }.decodeToString()
    }

    private fun lengthSize8(offset: Int): Int = if (chunk.u8(offset) and 0x80 != 0) 2 else 1

    private fun length8(offset: Int): Int {
        val first = chunk.u8(offset)
        return if (first and 0x80 != 0) ((first and 0x7f) shl 8) or chunk.u8(offset + 1) else first
    }

    private fun utf16At(offset: Int): String {
        // The length in UTF-16 units, one unit or, with the high bit set, two
        val first = chunk.u16(offset)
        val (length, charsAt) = if (first and 0x8000 != 0) {
            (((first and 0x7fff) shl 16) or chunk.u16(offset + 2)) to offset + 4
        } else {
            first to offset + 2
        }
        return String(CharArray(length) { chunk.u16(charsAt + it * 2).toChar() })
    }

    private companion object {
        const val UTF8_FLAG = 0x100L
    }
}
