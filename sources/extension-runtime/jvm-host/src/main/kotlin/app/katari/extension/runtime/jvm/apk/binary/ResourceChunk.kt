package app.katari.extension.runtime.jvm.apk.binary

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One chunk of Android's compiled resource format (`ResChunk_header`), which binary XML files and `resources.arsc` are
 * made of: [type], then a header of [headerSize] bytes, then the body, [size] bytes in all from [start] in [buffer].
 */
internal class ResourceChunk(
    val buffer: ByteBuffer,
    val start: Int,
    val type: Int,
    val headerSize: Int,
    val size: Int,
) {
    /** The chunks laid one after another in this one's body, after its header. */
    fun children(): Sequence<ResourceChunk> = chunksIn(buffer, start + headerSize, start + size)

    fun u8(offset: Int): Int = buffer.get(start + offset).toInt() and 0xff

    fun u16(offset: Int): Int = buffer.getShort(start + offset).toInt() and 0xffff

    fun u32(offset: Int): Long = buffer.getInt(start + offset).toLong() and 0xffffffffL

    fun s32(offset: Int): Int = buffer.getInt(start + offset)

    companion object {
        const val STRING_POOL = 0x0001
        const val TABLE = 0x0002
        const val XML = 0x0003
        const val XML_START_ELEMENT = 0x0102
        const val XML_END_ELEMENT = 0x0103
        const val XML_RESOURCE_MAP = 0x0180
        const val TABLE_PACKAGE = 0x0200
        const val TABLE_TYPE = 0x0201

        private const val HEADER_SIZE = 8

        /** The chunk a whole file is, such as an `AndroidManifest.xml` or a `resources.arsc`. */
        fun of(bytes: ByteArray): ResourceChunk {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return checkNotNull(chunksIn(buffer, 0, bytes.size).firstOrNull()) { "Not a compiled resource file" }
        }

        private fun chunksIn(buffer: ByteBuffer, from: Int, until: Int): Sequence<ResourceChunk> = sequence {
            var offset = from
            while (offset + HEADER_SIZE <= until) {
                val type = buffer.getShort(offset).toInt() and 0xffff
                val headerSize = buffer.getShort(offset + 2).toInt() and 0xffff
                val size = buffer.getInt(offset + 4)
                if (headerSize < HEADER_SIZE || size < headerSize || size > until - offset) {
                    throw IllegalArgumentException("Malformed resource chunk at $offset")
                }
                yield(ResourceChunk(buffer, offset, type, headerSize, size))
                offset += size
            }
        }
    }
}
