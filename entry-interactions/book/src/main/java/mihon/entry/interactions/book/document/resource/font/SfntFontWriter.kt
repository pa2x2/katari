package mihon.entry.interactions.book.document.resource.font

/** One table of a TrueType/OpenType font, its tag as the four ASCII bytes read big-endian. */
internal class SfntTable(val tag: Int, val data: ByteArray)

/** Assembles a TrueType/OpenType font file from its tables, as Android's font loader expects one. */
internal object SfntFontWriter {
    fun write(flavor: Long, tables: List<SfntTable>): ByteArray {
        require(tables.isNotEmpty()) { "A font needs at least one table" }
        require(tables.distinctBy(SfntTable::tag).size == tables.size) { "A font table appears twice" }
        val sorted = tables.sortedBy { it.tag.toLong() and 0xFFFFFFFFL }
        val head = sorted.firstOrNull { it.tag == HEAD_TAG }?.data?.copyOf()?.also { data ->
            require(data.size >= HEAD_MIN_SIZE) { "The head table is truncated" }
            // Recomputed below over the finished file.
            data.fill(0, CHECKSUM_ADJUSTMENT_OFFSET, CHECKSUM_ADJUSTMENT_OFFSET + 4)
        }
        val tableData = sorted.map { if (it.tag == HEAD_TAG) checkNotNull(head) else it.data }

        val output = FontDataWriter(HEADER_SIZE + RECORD_SIZE * sorted.size + tableData.sumOf { it.size + 3 })
        var searchRange = 1
        var entrySelector = 0
        while (searchRange * 2 <= sorted.size) {
            searchRange *= 2
            entrySelector++
        }
        output.u32(flavor)
        output.u16(sorted.size)
        output.u16(searchRange * 16)
        output.u16(entrySelector)
        output.u16(sorted.size * 16 - searchRange * 16)
        var offset = HEADER_SIZE + RECORD_SIZE * sorted.size
        sorted.forEachIndexed { index, table ->
            val data = tableData[index]
            output.u32(table.tag.toLong() and 0xFFFFFFFFL)
            output.u32(checksum(data))
            output.u32(offset.toLong())
            output.u32(data.size.toLong())
            offset += (data.size + 3) and 3.inv()
        }
        tableData.forEach { data ->
            output.bytes(data)
            output.padTo(4)
        }
        val font = output.toByteArray()
        if (head != null) {
            val headOffset = HEADER_SIZE + RECORD_SIZE * sorted.size +
                tableData.take(sorted.indexOfFirst { it.tag == HEAD_TAG }).sumOf { (it.size + 3) and 3.inv() }
            val adjustment = (CHECKSUM_MAGIC - checksum(font)) and 0xFFFFFFFFL
            for (index in 0 until 4) {
                font[headOffset + CHECKSUM_ADJUSTMENT_OFFSET + index] = (adjustment shr (24 - index * 8)).toByte()
            }
        }
        return font
    }

    private fun checksum(data: ByteArray): Long {
        var sum = 0L
        var index = 0
        while (index < data.size) {
            var word = 0L
            for (byte in 0 until 4) {
                word = (word shl 8) or (data.getOrElse(index + byte) { 0 }.toLong() and 0xFF)
            }
            sum = (sum + word) and 0xFFFFFFFFL
            index += 4
        }
        return sum
    }

    private const val HEADER_SIZE = 12
    private const val RECORD_SIZE = 16
    private const val HEAD_MIN_SIZE = 54
    private const val CHECKSUM_ADJUSTMENT_OFFSET = 8
    private const val CHECKSUM_MAGIC = 0xB1B0AFBAL
}

internal fun fontTag(name: String): Int {
    require(name.length == 4)
    return name.fold(0) { tag, char -> (tag shl 8) or (char.code and 0xFF) }
}

internal val HEAD_TAG = fontTag("head")
