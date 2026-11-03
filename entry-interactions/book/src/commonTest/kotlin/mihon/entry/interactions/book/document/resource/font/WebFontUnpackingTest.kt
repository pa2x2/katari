package mihon.entry.interactions.book.document.resource.font

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class WebFontUnpackingTest {
    @Test
    fun `WOFF and WOFF2 fonts unpack to the glyphs and tables of the TrueType font they were made from`() {
        val original = tables(resource("fixture.ttf"))
        listOf("fixture.woff", "fixture.woff2", "fixture-hmtx.woff2").forEach { name ->
            val unpacked = tables(assertNotNull(unpackWebFont(resource(name)), name))

            assertEquals(original.keys, unpacked.keys, name)
            original.forEach { (tag, data) ->
                when (tag) {
                    // Rebuilt glyphs may encode the same outlines differently; compared below.
                    "glyf", "loca" -> Unit
                    // checkSumAdjustment covers the whole file, and WOFF2 encoders set a flag bit.
                    "head" -> assertContentEquals(
                        data.copyOf().also { it.fill(0, 8, 12) }.also { it.fill(0, 16, 18) },
                        unpacked.getValue(tag).copyOf().also { it.fill(0, 8, 12) }.also { it.fill(0, 16, 18) },
                        "$name head",
                    )
                    else -> assertContentEquals(data, unpacked.getValue(tag), "$name $tag")
                }
            }
            assertEquals(glyphs(original), glyphs(unpacked), name)
        }
    }

    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/fonts/$name")).use { it.readBytes() }

    private fun tables(font: ByteArray): Map<String, ByteArray> {
        val reader = FontDataReader(font)
        reader.skip(4)
        val count = reader.u16()
        reader.skip(6)
        return List(count) {
            val tag = reader.bytes(4).decodeToString()
            reader.skip(4)
            val offset = reader.size32()
            val length = reader.size32()
            tag to font.copyOfRange(offset, offset + length)
        }.toMap()
    }

    /** Each glyph as what it draws: bounding box, contours and points, instructions, or composite records. */
    private fun glyphs(tables: Map<String, ByteArray>): List<String> {
        val longOffsets = FontDataReader(tables.getValue("head"), 50).s16() == 1
        val loca = FontDataReader(tables.getValue("loca"))
        val offsets = List(loca.remaining / if (longOffsets) 4 else 2) {
            if (longOffsets) loca.size32() else loca.u16() * 2
        }
        val glyf = tables.getValue("glyf")
        return offsets.zipWithNext { start, end ->
            if (start == end) return@zipWithNext "empty"
            val glyph = FontDataReader(glyf, start, end)
            val contours = glyph.s16()
            val box = List(4) { glyph.s16() }
            if (contours < 0) return@zipWithNext "composite $box ${glyph.bytes(glyph.remaining).trimmed().toList()}"
            val endPoints = List(contours) { glyph.u16() }
            val instructions = glyph.bytes(glyph.u16()).toList()
            val pointCount = endPoints.lastOrNull()?.plus(1) ?: 0
            val flags = mutableListOf<Int>()
            while (flags.size < pointCount) {
                val flag = glyph.u8()
                flags += flag
                if (flag and 0x08 != 0) repeat(glyph.u8()) { flags += flag }
            }
            fun coordinates(short: Int, same: Int): List<Int> {
                var value = 0
                return flags.map { flag ->
                    value += when {
                        flag and short != 0 -> glyph.u8().let { if (flag and same != 0) it else -it }
                        flag and same != 0 -> 0
                        else -> glyph.s16()
                    }
                    value
                }
            }
            val xs = coordinates(short = 0x02, same = 0x10)
            val ys = coordinates(short = 0x04, same = 0x20)
            val points = flags.indices.map { Triple(xs[it], ys[it], flags[it] and 0x01 != 0) }
            "simple $box $endPoints $instructions $points"
        }
    }

    // Glyph records are padded to an alignment, and encoders differ in how much padding they keep.
    private fun ByteArray.trimmed(): ByteArray = copyOf(indexOfLast { it != 0.toByte() } + 1)
}
