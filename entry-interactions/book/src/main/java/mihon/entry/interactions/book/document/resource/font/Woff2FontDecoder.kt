package mihon.entry.interactions.book.document.resource.font

import org.brotli.dec.BrotliInputStream

/**
 * Unpacks WOFF 2.0: a table directory, then every table in one Brotli stream, with `glyf`, `loca` and optionally
 * `hmtx` in their transformed forms ([Woff2GlyfReconstruction]). Font collections aren't supported.
 */
internal object Woff2FontDecoder {
    fun decode(data: ByteArray, maxFontBytes: Int): ByteArray {
        val reader = FontDataReader(data)
        require(reader.u32() == WOFF2_SIGNATURE) { "Not a WOFF2 font" }
        val flavor = reader.u32()
        require(flavor != COLLECTION_FLAVOR) { "WOFF2 font collections aren't supported" }
        reader.skip(4) // length
        val numTables = reader.u16()
        require(numTables > 0) { "A WOFF2 font has no tables" }
        reader.skip(2) // reserved
        val totalSfntSize = reader.size32()
        require(totalSfntSize in 1..maxFontBytes) { "The unpacked font exceeds its byte limit" }
        val totalCompressedSize = reader.size32()
        reader.skip(HEADER_REMAINDER)
        val entries = List(numTables) { readEntry(reader) }
        val streamLength = entries.sumOf { it.streamLength.toLong() }
        require(streamLength <= maxFontBytes) { "The unpacked font exceeds its byte limit" }
        val stream = FontDataReader(decompress(reader.bytes(totalCompressedSize), streamLength.toInt()))
        val tableStreams = entries.associate { entry -> entry.tag to stream.bytes(entry.streamLength) }
        return SfntFontWriter.write(flavor, reconstructTables(entries, tableStreams))
    }

    private fun reconstructTables(entries: List<Entry>, streams: Map<Int, ByteArray>): List<SfntTable> {
        val transformed = entries.filter(Entry::transformed).map(Entry::tag).toSet()
        val glyfTransformed = GLYF_TAG in transformed
        require(glyfTransformed == (LOCA_TAG in transformed)) { "WOFF2 glyf and loca must be transformed together" }
        require(HMTX_TAG !in transformed || glyfTransformed) { "A transformed hmtx needs a transformed glyf" }
        val glyf = if (glyfTransformed) Woff2GlyfReconstruction.reconstruct(streams.getValue(GLYF_TAG)) else null
        return entries.map { entry ->
            val data = when {
                !entry.transformed -> streams.getValue(entry.tag)
                entry.tag == GLYF_TAG -> checkNotNull(glyf).glyf
                entry.tag == LOCA_TAG -> checkNotNull(glyf).loca
                entry.tag == HMTX_TAG -> Woff2GlyfReconstruction.reconstructHmtx(
                    transformed = streams.getValue(HMTX_TAG),
                    numberOfHMetrics = streams[HHEA_TAG]?.let { FontDataReader(it, HHEA_METRICS_OFFSET).u16() }
                        ?: throw IllegalArgumentException("A transformed hmtx needs hhea"),
                    xMins = checkNotNull(glyf).xMins,
                )
                else -> throw IllegalArgumentException("Unknown WOFF2 table transform")
            }
            val table = if (entry.tag == HEAD_TAG && glyf != null) data.withIndexToLocFormat(glyf.indexFormat) else data
            SfntTable(entry.tag, table)
        }
    }

    private fun readEntry(reader: FontDataReader): Entry {
        val flags = reader.u8()
        val tagIndex = flags and 0x3F
        val tag = if (tagIndex == EXPLICIT_TAG_INDEX) reader.u32().toInt() else fontTag(KNOWN_TAGS[tagIndex])
        val transformVersion = flags shr 6
        val originalLength = reader.uintBase128()
        // glyf and loca are transformed under version 0 and stored as they are under 3; every other table the
        // other way round.
        val transformed = if (tag == GLYF_TAG || tag == LOCA_TAG) transformVersion == 0 else transformVersion != 0
        val streamLength = if (transformed) reader.uintBase128() else originalLength
        require(tag != LOCA_TAG || !transformed || streamLength == 0) { "A transformed loca must be empty" }
        return Entry(tag, transformed, streamLength)
    }

    private fun decompress(compressed: ByteArray, length: Int): ByteArray =
        BrotliInputStream(compressed.inputStream()).use { input ->
            val output = ByteArray(length)
            var read = 0
            while (read < length) {
                val count = input.read(output, read, length - read)
                require(count > 0) { "The WOFF2 table data is truncated" }
                read += count
            }
            require(input.read() == -1) { "The WOFF2 table data is longer than its tables" }
            output
        }

    private fun ByteArray.withIndexToLocFormat(indexFormat: Int): ByteArray {
        require(size >= HEAD_INDEX_TO_LOC_FORMAT_OFFSET + 2) { "The head table is truncated" }
        return copyOf().also { head ->
            head[HEAD_INDEX_TO_LOC_FORMAT_OFFSET] = 0
            head[HEAD_INDEX_TO_LOC_FORMAT_OFFSET + 1] = indexFormat.toByte()
        }
    }

    private class Entry(val tag: Int, val transformed: Boolean, val streamLength: Int)

    private const val WOFF2_SIGNATURE = 0x774F4632L // wOF2
    private const val COLLECTION_FLAVOR = 0x74746366L // ttcf
    private const val HEADER_REMAINDER = 2 + 2 + 4 * 5
    private const val EXPLICIT_TAG_INDEX = 63
    private const val HHEA_METRICS_OFFSET = 34
    private const val HEAD_INDEX_TO_LOC_FORMAT_OFFSET = 50
    private val GLYF_TAG = fontTag("glyf")
    private val LOCA_TAG = fontTag("loca")
    private val HMTX_TAG = fontTag("hmtx")
    private val HHEA_TAG = fontTag("hhea")

    // The WOFF2 known-table list, indexed by the low six bits of an entry's flags.
    private val KNOWN_TAGS = listOf(
        "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post", "cvt ", "fpgm", "glyf", "loca", "prep",
        "CFF ", "VORG", "EBDT", "EBLC", "gasp", "hdmx", "kern", "LTSH", "PCLT", "VDMX", "vhea", "vmtx", "BASE",
        "GDEF", "GPOS", "GSUB", "EBSC", "JSTF", "MATH", "CBDT", "CBLC", "COLR", "CPAL", "SVG ", "sbix", "acnt",
        "avar", "bdat", "bloc", "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar", "gvar", "hsty", "just", "lcar",
        "mort", "morx", "opbd", "prop", "trak", "Zapf", "Silf", "Glat", "Gloc", "Feat", "Sill",
    )
}
