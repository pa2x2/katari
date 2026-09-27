package mihon.text.recognition.runtime.geometry

import mihon.text.recognition.api.image.ImageRect

/**
 * Orders page elements the way a reader scans them: rows from top to bottom, and within a row from right to left
 * for right-to-left layouts or from left to right otherwise.
 *
 * An element joins the current row when its vertical center lies within the row's vertical extent, which keeps
 * bubbles of one panel strip together even when their tops are not aligned.
 */
internal fun <T> inReadingOrder(
    elements: List<T>,
    bounds: (T) -> ImageRect,
    rightToLeft: Boolean,
): List<T> {
    val rows = mutableListOf<MutableList<T>>()
    var rowTop = 0
    var rowBottom = 0
    elements.sortedBy { bounds(it).top }.forEach { element ->
        val rect = bounds(element)
        val center = (rect.top + rect.bottom) / 2
        if (rows.isNotEmpty() && center in rowTop..rowBottom) {
            rows.last() += element
            rowBottom = maxOf(rowBottom, rect.bottom)
        } else {
            rows += mutableListOf(element)
            rowTop = rect.top
            rowBottom = rect.bottom
        }
    }
    return rows.flatMap { row ->
        if (rightToLeft) row.sortedByDescending { bounds(it).right } else row.sortedBy { bounds(it).left }
    }
}
