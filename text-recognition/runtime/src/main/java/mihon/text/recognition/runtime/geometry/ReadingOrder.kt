package mihon.text.recognition.runtime.geometry

import mihon.text.recognition.api.image.ImageRect

/**
 * Orders page elements the way a reader scans panels: the page is cut recursively along empty gaps, first into
 * horizontal strips read from top to bottom, then into columns read from right to left for right-to-left layouts or
 * from left to right otherwise.
 *
 * Cutting along gaps keeps a column of bubbles beside a tall bubble together, where grouping by rows would interleave
 * them. Elements that no gap separates are read from top to bottom.
 */
internal fun <T> inReadingOrder(
    elements: List<T>,
    bounds: (T) -> ImageRect,
    rightToLeft: Boolean,
): List<T> {
    if (elements.size <= 1) return elements
    val strips = separated(elements) { bounds(it).top until bounds(it).bottom }
    if (strips.size > 1) return strips.flatMap { inReadingOrder(it, bounds, rightToLeft) }
    val columns = separated(elements) { bounds(it).left until bounds(it).right }
    if (columns.size > 1) {
        return (if (rightToLeft) columns.asReversed() else columns).flatMap { inReadingOrder(it, bounds, rightToLeft) }
    }
    return elements.sortedWith(
        compareBy<T> { bounds(it).top }.thenBy { if (rightToLeft) -bounds(it).right else bounds(it).left },
    )
}

/** Groups of [elements] whose [extent]s overlap, in increasing order; consecutive groups are separated by a gap. */
private fun <T> separated(elements: List<T>, extent: (T) -> IntRange): List<List<T>> {
    val groups = mutableListOf<MutableList<T>>()
    var end = Int.MIN_VALUE
    elements.sortedBy { extent(it).first }.forEach { element ->
        val range = extent(element)
        if (groups.isEmpty() || range.first > end) groups += mutableListOf<T>()
        groups.last() += element
        end = maxOf(end, range.last)
    }
    return groups
}
