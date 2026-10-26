package mihon.entry.interactions.book.document.resource.font

/** Big-endian reads over one region of font data; reading past the region rejects the font as truncated. */
internal class FontDataReader(
    private val data: ByteArray,
    start: Int = 0,
    private val end: Int = data.size,
) {
    var position: Int = start
        private set

    init {
        require(start in 0..end && end <= data.size) { "Font data region is out of bounds" }
    }

    val remaining: Int get() = end - position

    fun u8(): Int {
        require(position < end) { "Font data is truncated" }
        return data[position++].toInt() and 0xFF
    }

    fun u16(): Int = (u8() shl 8) or u8()

    fun s16(): Int = u16().toShort().toInt()

    fun u32(): Long = (u16().toLong() shl 16) or u16().toLong()

    /** A 32-bit field that must fit in a non-negative Int, such as a length or an offset. */
    fun size32(): Int {
        val value = u32()
        require(value <= Int.MAX_VALUE) { "Font data length is out of range" }
        return value.toInt()
    }

    fun skip(count: Int) {
        require(count in 0..remaining) { "Font data is truncated" }
        position += count
    }

    fun bytes(count: Int): ByteArray {
        require(count in 0..remaining) { "Font data is truncated" }
        return data.copyOfRange(position, position + count).also { position += count }
    }

    /** The bytes read since [start], a position this reader passed earlier. */
    fun bytesSince(start: Int): ByteArray = data.copyOfRange(start, position)

    fun region(count: Int): FontDataReader {
        require(count in 0..remaining) { "Font data is truncated" }
        return FontDataReader(data, position, position + count).also { position += count }
    }

    /** WOFF2 `UIntBase128`: up to five bytes of seven bits, most significant first, without leading zeros. */
    fun uintBase128(): Int {
        var value = 0L
        repeat(5) { index ->
            val byte = u8()
            require(index != 0 || byte != 0x80) { "UIntBase128 has a leading zero" }
            value = (value shl 7) or (byte and 0x7F).toLong()
            require(value <= Int.MAX_VALUE) { "UIntBase128 overflows" }
            if (byte and 0x80 == 0) return value.toInt()
        }
        throw IllegalArgumentException("UIntBase128 is longer than five bytes")
    }

    /** WOFF2 `255UInt16`. */
    fun uint255(): Int = when (val code = u8()) {
        WORD_CODE -> u16()
        ONE_MORE_BYTE_CODE_1 -> LOWEST_U_CODE + u8()
        ONE_MORE_BYTE_CODE_2 -> LOWEST_U_CODE * 2 + u8()
        else -> code
    }

    private companion object {
        const val LOWEST_U_CODE = 253
        const val WORD_CODE = 253
        const val ONE_MORE_BYTE_CODE_2 = 254
        const val ONE_MORE_BYTE_CODE_1 = 255
    }
}

/** Growable big-endian output for rebuilt font tables. */
internal class FontDataWriter(initialCapacity: Int = 1024) {
    private var buffer = ByteArray(initialCapacity.coerceAtLeast(16))

    var size: Int = 0
        private set

    fun u8(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun u16(value: Int) {
        u8(value shr 8)
        u8(value)
    }

    fun u32(value: Long) {
        u16((value shr 16).toInt())
        u16(value.toInt())
    }

    fun bytes(value: ByteArray) {
        ensure(value.size)
        value.copyInto(buffer, size)
        size += value.size
    }

    fun padTo(alignment: Int) {
        while (size % alignment != 0) u8(0)
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun ensure(count: Int) {
        if (size + count <= buffer.size) return
        buffer = buffer.copyOf(maxOf(buffer.size * 2, size + count))
    }
}
