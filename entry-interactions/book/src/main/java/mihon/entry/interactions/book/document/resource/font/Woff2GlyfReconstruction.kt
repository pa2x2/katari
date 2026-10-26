package mihon.entry.interactions.book.document.resource.font

/**
 * Rebuilds `glyf`, `loca` and `hmtx` from their WOFF2 transforms (W3C WOFF 2.0, section 5).
 *
 * The transformed `glyf` splits glyphs into parallel streams: contour counts, point counts, flags, packed
 * coordinate triplets, composite records, explicit bounding boxes and instructions. Glyphs are written back in
 * the ordinary TrueType layout; coordinates use the short forms where they fit but repeated flags aren't folded,
 * so the result can be a little larger than the original font.
 */
internal object Woff2GlyfReconstruction {
    class Result(val glyf: ByteArray, val loca: ByteArray, val indexFormat: Int, val xMins: IntArray)

    fun reconstruct(transformed: ByteArray): Result {
        val header = FontDataReader(transformed)
        header.skip(2) // reserved
        val optionFlags = header.u16()
        val numGlyphs = header.u16()
        val indexFormat = header.u16()
        require(indexFormat == 0 || indexFormat == 1) { "Unknown loca format" }
        val sizes = IntArray(STREAM_COUNT) { header.size32() }
        val contourCounts = header.region(sizes[0])
        val pointCounts = header.region(sizes[1])
        val flags = header.region(sizes[2])
        val glyphs = header.region(sizes[3])
        val composites = header.region(sizes[4])
        val boxes = header.region(sizes[5])
        val instructions = header.region(sizes[6])
        val overlapBitmap = if (optionFlags and OVERLAP_SIMPLE_BITMAP != 0) header.bytes((numGlyphs + 7) / 8) else null
        val boxBitmap = boxes.bytes(4 * ((numGlyphs + 31) / 32))

        val output = FontDataWriter(transformed.size * 2)
        val offsets = IntArray(numGlyphs + 1)
        val xMins = IntArray(numGlyphs)
        for (glyph in 0 until numGlyphs) {
            val hasBox = boxBitmap.bit(glyph)
            when (val contours = contourCounts.s16()) {
                0 -> require(!hasBox) { "An empty glyph has a bounding box" }
                -1 -> {
                    require(hasBox) { "A composite glyph has no bounding box" }
                    xMins[glyph] = writeComposite(output, composites, glyphs, instructions, boxes)
                }
                else -> {
                    require(contours > 0) { "Glyph contour count is invalid" }
                    xMins[glyph] = writeSimple(
                        output = output,
                        contours = contours,
                        pointCounts = pointCounts,
                        flags = flags,
                        glyphs = glyphs,
                        instructions = instructions,
                        box = if (hasBox) boxes else null,
                        overlaps = overlapBitmap?.bit(glyph) == true,
                    )
                }
            }
            output.padTo(4)
            offsets[glyph + 1] = output.size
        }

        val loca = FontDataWriter((numGlyphs + 1) * if (indexFormat == 0) 2 else 4)
        offsets.forEach { offset -> if (indexFormat == 0) loca.u16(offset / 2) else loca.u32(offset.toLong()) }
        require(indexFormat == 1 || output.size / 2 <= 0xFFFF) { "Glyphs don't fit the short loca format" }
        return Result(output.toByteArray(), loca.toByteArray(), indexFormat, xMins)
    }

    fun reconstructHmtx(transformed: ByteArray, numberOfHMetrics: Int, xMins: IntArray): ByteArray {
        val numGlyphs = xMins.size
        require(numberOfHMetrics in 1..numGlyphs) { "hhea metric count is invalid" }
        val reader = FontDataReader(transformed)
        val flags = reader.u8()
        val advances = IntArray(numberOfHMetrics) { reader.u16() }
        val proportionalBearings = flags and PROPORTIONAL_LSB_OMITTED == 0
        val monospacedBearings = flags and MONOSPACED_LSB_OMITTED == 0
        // An omitted left side bearing equals the glyph's xMin, which is why the transform can drop it.
        val bearings = IntArray(numGlyphs) { glyph ->
            val stored = if (glyph < numberOfHMetrics) proportionalBearings else monospacedBearings
            if (stored) reader.s16() else xMins[glyph]
        }
        val output = FontDataWriter(numberOfHMetrics * 4 + (numGlyphs - numberOfHMetrics) * 2)
        for (glyph in 0 until numGlyphs) {
            if (glyph < numberOfHMetrics) output.u16(advances[glyph])
            output.u16(bearings[glyph])
        }
        return output.toByteArray()
    }

    private fun writeSimple(
        output: FontDataWriter,
        contours: Int,
        pointCounts: FontDataReader,
        flags: FontDataReader,
        glyphs: FontDataReader,
        instructions: FontDataReader,
        box: FontDataReader?,
        overlaps: Boolean,
    ): Int {
        var pointCount = 0
        val endPoints = IntArray(contours) {
            pointCount += pointCounts.uint255()
            require(pointCount <= 0xFFFF) { "A glyph has too many points" }
            pointCount - 1
        }
        val onCurve = BooleanArray(pointCount)
        val dxs = IntArray(pointCount)
        val dys = IntArray(pointCount)
        var x = 0
        var y = 0
        var xMin = Int.MAX_VALUE
        var yMin = Int.MAX_VALUE
        var xMax = Int.MIN_VALUE
        var yMax = Int.MIN_VALUE
        for (point in 0 until pointCount) {
            val flag = flags.u8()
            onCurve[point] = flag and 0x80 == 0
            decodeTriplet(flag and 0x7F, glyphs, dxs, dys, point)
            x += dxs[point]
            y += dys[point]
            xMin = minOf(xMin, x)
            yMin = minOf(yMin, y)
            xMax = maxOf(xMax, x)
            yMax = maxOf(yMax, y)
        }
        val instructionLength = glyphs.uint255()
        val program = instructions.bytes(instructionLength)
        if (box != null) {
            xMin = box.s16()
            yMin = box.s16()
            xMax = box.s16()
            yMax = box.s16()
        } else if (pointCount == 0) {
            xMin = 0
            yMin = 0
            xMax = 0
            yMax = 0
        }

        output.u16(contours)
        output.u16(xMin)
        output.u16(yMin)
        output.u16(xMax)
        output.u16(yMax)
        endPoints.forEach(output::u16)
        output.u16(instructionLength)
        output.bytes(program)
        for (point in 0 until pointCount) {
            var flag = if (onCurve[point]) ON_CURVE else 0
            flag = flag or coordinateFlag(dxs[point], X_SHORT, X_SAME_OR_POSITIVE)
            flag = flag or coordinateFlag(dys[point], Y_SHORT, Y_SAME_OR_POSITIVE)
            if (point == 0 && overlaps) flag = flag or OVERLAP_SIMPLE
            output.u8(flag)
        }
        dxs.forEach { writeCoordinate(output, it) }
        dys.forEach { writeCoordinate(output, it) }
        return xMin
    }

    private fun writeComposite(
        output: FontDataWriter,
        composites: FontDataReader,
        glyphs: FontDataReader,
        instructions: FontDataReader,
        boxes: FontDataReader,
    ): Int {
        val start = composites.position
        var hasInstructions = false
        do {
            val flags = composites.u16()
            hasInstructions = hasInstructions || flags and WE_HAVE_INSTRUCTIONS != 0
            val arguments = if (flags and ARG_1_AND_2_ARE_WORDS != 0) 4 else 2
            val transform = when {
                flags and WE_HAVE_A_SCALE != 0 -> 2
                flags and WE_HAVE_AN_X_AND_Y_SCALE != 0 -> 4
                flags and WE_HAVE_A_TWO_BY_TWO != 0 -> 8
                else -> 0
            }
            composites.skip(2 + arguments + transform) // glyph index, arguments, transform
        } while (flags and MORE_COMPONENTS != 0)
        val components = composites.bytesSince(start)
        val xMin = boxes.s16()
        output.u16(-1)
        output.u16(xMin)
        repeat(3) { output.u16(boxes.s16()) }
        output.bytes(components)
        if (hasInstructions) {
            val length = glyphs.uint255()
            output.u16(length)
            output.bytes(instructions.bytes(length))
        }
        return xMin
    }

    // The packed coordinate encoding of WOFF 2.0 section 5.2: the flag picks how many bytes follow and how
    // their bits split between dx and dy, and its low bits give the signs.
    private fun decodeTriplet(flag: Int, glyphs: FontDataReader, dxs: IntArray, dys: IntArray, point: Int) {
        when {
            flag < 10 -> {
                dxs[point] = 0
                dys[point] = withSign(flag, ((flag and 14) shl 7) + glyphs.u8())
            }
            flag < 20 -> {
                dxs[point] = withSign(flag, (((flag - 10) and 14) shl 7) + glyphs.u8())
                dys[point] = 0
            }
            flag < 84 -> {
                val base = flag - 20
                val byte = glyphs.u8()
                dxs[point] = withSign(flag, 1 + (base and 0x30) + (byte shr 4))
                dys[point] = withSign(flag shr 1, 1 + ((base and 0x0C) shl 2) + (byte and 0x0F))
            }
            flag < 120 -> {
                val base = flag - 84
                dxs[point] = withSign(flag, 1 + ((base / 12) shl 8) + glyphs.u8())
                dys[point] = withSign(flag shr 1, 1 + (((base % 12) shr 2) shl 8) + glyphs.u8())
            }
            flag < 124 -> {
                val first = glyphs.u8()
                val second = glyphs.u8()
                dxs[point] = withSign(flag, (first shl 4) + (second shr 4))
                dys[point] = withSign(flag shr 1, ((second and 0x0F) shl 8) + glyphs.u8())
            }
            else -> {
                dxs[point] = withSign(flag, glyphs.u16())
                dys[point] = withSign(flag shr 1, glyphs.u16())
            }
        }
    }

    private fun withSign(sign: Int, value: Int): Int = if (sign and 1 != 0) value else -value

    private fun coordinateFlag(delta: Int, short: Int, sameOrPositive: Int): Int = when {
        delta == 0 -> sameOrPositive
        delta in -255..255 -> short or if (delta > 0) sameOrPositive else 0
        else -> 0
    }

    private fun writeCoordinate(output: FontDataWriter, delta: Int) {
        when {
            delta == 0 -> Unit
            delta in -255..255 -> output.u8(kotlin.math.abs(delta))
            else -> {
                require(delta in Short.MIN_VALUE..Short.MAX_VALUE) { "A glyph coordinate is out of range" }
                output.u16(delta)
            }
        }
    }

    private fun ByteArray.bit(index: Int): Boolean = (this[index shr 3].toInt() and (0x80 shr (index and 7))) != 0

    private const val STREAM_COUNT = 7
    private const val OVERLAP_SIMPLE_BITMAP = 0x0001
    private const val PROPORTIONAL_LSB_OMITTED = 0x01
    private const val MONOSPACED_LSB_OMITTED = 0x02

    private const val ON_CURVE = 0x01
    private const val X_SHORT = 0x02
    private const val Y_SHORT = 0x04
    private const val X_SAME_OR_POSITIVE = 0x10
    private const val Y_SAME_OR_POSITIVE = 0x20
    private const val OVERLAP_SIMPLE = 0x40

    private const val ARG_1_AND_2_ARE_WORDS = 0x0001
    private const val WE_HAVE_A_SCALE = 0x0008
    private const val MORE_COMPONENTS = 0x0020
    private const val WE_HAVE_AN_X_AND_Y_SCALE = 0x0040
    private const val WE_HAVE_A_TWO_BY_TWO = 0x0080
    private const val WE_HAVE_INSTRUCTIONS = 0x0100
}
