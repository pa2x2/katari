package mihon.entry.interactions.book.document.resource.font

import java.util.zip.Inflater

/** Unpacks WOFF 1.0: a TrueType/OpenType font whose tables are each zlib-compressed. */
internal object WoffFontDecoder {
    fun decode(data: ByteArray, maxFontBytes: Int): ByteArray {
        val reader = FontDataReader(data)
        require(reader.u32() == WOFF_SIGNATURE) { "Not a WOFF font" }
        val flavor = reader.u32()
        reader.skip(4) // length
        val numTables = reader.u16()
        reader.skip(2) // reserved
        val totalSfntSize = reader.size32()
        require(totalSfntSize in 1..maxFontBytes) { "The unpacked font exceeds its byte limit" }
        reader.skip(HEADER_REMAINDER)
        var unpackedBytes = 0L
        val tables = List(numTables) {
            val tag = reader.u32().toInt()
            val offset = reader.size32()
            val compressedLength = reader.size32()
            val originalLength = reader.size32()
            reader.skip(4) // original checksum, recomputed when the font is assembled
            unpackedBytes += originalLength
            require(unpackedBytes <= maxFontBytes) { "The unpacked font exceeds its byte limit" }
            require(compressedLength <= originalLength) { "A WOFF table grows when compressed" }
            val compressed = FontDataReader(
                data,
                offset,
                Math.addExact(offset, compressedLength).coerceAtMost(data.size),
            )
                .bytes(compressedLength)
            SfntTable(tag, if (compressedLength == originalLength) compressed else inflate(compressed, originalLength))
        }
        return SfntFontWriter.write(flavor, tables)
    }

    private fun inflate(compressed: ByteArray, originalLength: Int): ByteArray {
        val inflater = Inflater()
        try {
            inflater.setInput(compressed)
            val output = ByteArray(originalLength)
            var written = 0
            while (written < originalLength) {
                val count = inflater.inflate(output, written, originalLength - written)
                if (count == 0) break
                written += count
            }
            require(written == originalLength) { "A WOFF table is truncated" }
            return output
        } finally {
            inflater.end()
        }
    }

    private const val WOFF_SIGNATURE = 0x774F4646L // wOFF
    private const val HEADER_REMAINDER = 2 + 2 + 4 * 5
}
