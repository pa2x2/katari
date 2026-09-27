package mihon.text.recognition.provider.onnx.paddle.lines

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Text lines in the per-pixel text probability map of PaddleOCR's DB detector, found as PaddleOCR's own
 * post-processing finds them: every connected area above [threshold] becomes its minimum-area rectangle, rectangles
 * whose mean probability is below [boxThreshold] are dropped, and the rest grow by the margin the detector shrank
 * text by during training ([unclipRatio]).
 *
 * Unlike bands of rows or columns, these lines keep side-by-side blocks apart, follow slanted text, and carry their
 * own orientation. Returned rectangles are in map pixels.
 */
internal fun detectTextLines(
    probability: FloatArray,
    width: Int,
    height: Int,
    threshold: Float = TEXT_THRESHOLD,
    boxThreshold: Float = BOX_THRESHOLD,
    unclipRatio: Double = UNCLIP_RATIO,
): List<RotatedRectangle> {
    require(probability.size == width * height)
    return components(probability, width, height, threshold)
        .asSequence()
        .take(MAXIMUM_CANDIDATES)
        .mapNotNull { outline ->
            val rectangle = minimumAreaRectangle(outline)
            if (rectangle.shortSide < MINIMUM_SIZE) return@mapNotNull null
            if (meanProbability(probability, width, height, rectangle) < boxThreshold) return@mapNotNull null
            val area = rectangle.width * rectangle.height
            val perimeter = 2 * (rectangle.width + rectangle.height)
            rectangle.expanded(area * unclipRatio / perimeter).takeIf { it.shortSide >= MINIMUM_SIZE + 2 }
        }
        .toList()
}

/**
 * The pixel outlines of the 8-connected areas above [threshold]. Each outline holds the outer corners of every row's
 * leftmost and rightmost pixel, which span the same convex hull as the whole area.
 */
private fun components(
    probability: FloatArray,
    width: Int,
    height: Int,
    threshold: Float,
): List<List<RotatedRectangle.Point>> {
    val visited = BooleanArray(probability.size)
    val stack = IntArray(probability.size)
    val outlines = mutableListOf<List<RotatedRectangle.Point>>()
    for (seed in probability.indices) {
        if (visited[seed] || probability[seed] <= threshold) continue
        val rowStart = mutableMapOf<Int, Int>()
        val rowEnd = mutableMapOf<Int, Int>()
        var size = 0
        stack[size++] = seed
        visited[seed] = true
        while (size > 0) {
            val index = stack[--size]
            val x = index % width
            val y = index / width
            rowStart[y] = minOf(rowStart[y] ?: x, x)
            rowEnd[y] = maxOf(rowEnd[y] ?: x, x)
            for (dy in -1..1) {
                val ny = y + dy
                if (ny !in 0 until height) continue
                for (dx in -1..1) {
                    val nx = x + dx
                    if (nx !in 0 until width) continue
                    val neighbour = ny * width + nx
                    if (!visited[neighbour] && probability[neighbour] > threshold) {
                        visited[neighbour] = true
                        stack[size++] = neighbour
                    }
                }
            }
        }
        outlines += rowStart.flatMap { (y, start) ->
            val end = rowEnd.getValue(y) + 1.0
            listOf(
                RotatedRectangle.Point(start.toDouble(), y.toDouble()),
                RotatedRectangle.Point(end, y.toDouble()),
                RotatedRectangle.Point(start.toDouble(), y + 1.0),
                RotatedRectangle.Point(end, y + 1.0),
            )
        }
    }
    return outlines
}

/** Mean probability of the map pixels whose centres lie inside [rectangle]. */
private fun meanProbability(probability: FloatArray, width: Int, height: Int, rectangle: RotatedRectangle): Double {
    val bounds = rectangle.bounds()
    var sum = 0.0
    var count = 0
    for (y in floor(bounds[1]).toInt().coerceAtLeast(0) until ceil(bounds[3]).toInt().coerceAtMost(height)) {
        for (x in floor(bounds[0]).toInt().coerceAtLeast(0) until ceil(bounds[2]).toInt().coerceAtMost(width)) {
            if (rectangle.contains(x + 0.5, y + 0.5)) {
                sum += probability[y * width + x]
                count++
            }
        }
    }
    return if (count == 0) 0.0 else sum / count
}

private const val TEXT_THRESHOLD = 0.3f
private const val BOX_THRESHOLD = 0.6f
private const val UNCLIP_RATIO = 1.5
private const val MINIMUM_SIZE = 3.0
private const val MAXIMUM_CANDIDATES = 1000
