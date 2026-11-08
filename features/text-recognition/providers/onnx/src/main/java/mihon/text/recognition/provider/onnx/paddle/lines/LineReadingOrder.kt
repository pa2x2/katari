package mihon.text.recognition.provider.onnx.paddle.lines

/**
 * Orders the lines of one text region: vertical columns from right to left, horizontal lines from top to bottom and,
 * within a row, from left to right. Lines share a row when they overlap by at least half the height of the lower one.
 *
 * @param bounds axis-aligned bounds of a line as left, top, right, bottom.
 */
internal fun <T> linesInReadingOrder(lines: List<T>, bounds: (T) -> DoubleArray, vertical: Boolean): List<T> {
    if (vertical) return lines.sortedByDescending { bounds(it).let { box -> box[0] + box[2] } }
    val rows = mutableListOf<MutableList<T>>()
    lines.sortedBy { bounds(it)[1] }.forEach { line ->
        val box = bounds(line)
        val row = rows.lastOrNull()?.takeIf { row ->
            val previous = bounds(row.last())
            val overlap = minOf(previous[3], box[3]) - maxOf(previous[1], box[1])
            overlap >= SAME_ROW_OVERLAP * minOf(previous[3] - previous[1], box[3] - box[1])
        }
        if (row != null) row += line else rows += mutableListOf(line)
    }
    return rows.flatMap { row -> row.sortedBy { bounds(it)[0] } }
}

private const val SAME_ROW_OVERLAP = 0.5
