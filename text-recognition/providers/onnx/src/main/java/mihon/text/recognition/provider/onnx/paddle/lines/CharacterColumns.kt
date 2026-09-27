package mihon.text.recognition.provider.onnx.paddle.lines

/**
 * Joins characters of one vertical column that the detector found one by one.
 *
 * Vertical CJK lettering leaves gaps between characters that are as wide as the ones between columns, so the
 * detector often reports each character of a column separately. A line recognizer cannot read an isolated character
 * reliably, so upright boxes of similar width that are stacked with at most [gap] of their width between them are
 * merged into one column. Boxes that are clearly horizontal lines and tilted boxes are left as they are.
 */
internal fun stackCharacterColumns(lines: List<RotatedRectangle>, gap: Double = COLUMN_GAP): List<RotatedRectangle> {
    val (upright, other) = lines.partition { it.isAxisAligned }
    val boxes = upright.map(RotatedRectangle::bounds).toMutableList()
    var merged = true
    while (merged) {
        merged = false
        search@ for (first in boxes.indices) {
            for (second in boxes.indices) {
                if (first != second && stackable(boxes[first], boxes[second], gap)) {
                    val a = boxes[first]
                    val b = boxes[second]
                    boxes[first] =
                        doubleArrayOf(minOf(a[0], b[0]), minOf(a[1], b[1]), maxOf(a[2], b[2]), maxOf(a[3], b[3]))
                    boxes.removeAt(second)
                    merged = true
                    break@search
                }
            }
        }
    }
    return boxes.map { RotatedRectangle.axisAligned(it[0], it[1], it[2], it[3]) } + other
}

private fun stackable(a: DoubleArray, b: DoubleArray, gap: Double): Boolean {
    val widthA = a[2] - a[0]
    val widthB = b[2] - b[0]
    val narrower = minOf(widthA, widthB)
    val wider = maxOf(widthA, widthB)
    if (narrower < SIMILAR_WIDTH * wider) return false
    if (minOf(a[2], b[2]) - maxOf(a[0], b[0]) < SHARED_WIDTH * narrower) return false
    if (a.isHorizontalLine() || b.isHorizontalLine()) return false
    val verticalGap = maxOf(a[1], b[1]) - minOf(a[3], b[3])
    return verticalGap <= gap * wider
}

private fun DoubleArray.isHorizontalLine() = this[2] - this[0] > HORIZONTAL_ASPECT * (this[3] - this[1])

private const val COLUMN_GAP = 0.8
private const val SIMILAR_WIDTH = 0.6
private const val SHARED_WIDTH = 0.6
private const val HORIZONTAL_ASPECT = 1.5
