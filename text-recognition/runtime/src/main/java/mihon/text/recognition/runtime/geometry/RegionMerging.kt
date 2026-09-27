package mihon.text.recognition.runtime.geometry

import mihon.text.recognition.api.image.ImageRect

/**
 * Collapses detections of the same object that overlapping tiles, or the detector itself, reported more than once.
 *
 * Two rectangles describe the same object when their intersection covers most of the smaller one. The merged
 * rectangle is their union, which restores objects that one tile saw truncated at its edge. A union can come to cover
 * another accepted rectangle it did not overlap before, so merging repeats until no two groups overlap. Candidates
 * are visited in [priority] order, so the most confident detection determines the surviving value.
 */
internal fun <T> mergeOverlapping(
    candidates: List<T>,
    bounds: (T) -> ImageRect,
    withBounds: (T, ImageRect) -> T,
    sameGroup: (T, T) -> Boolean = { _, _ -> true },
    priority: Comparator<T>,
    threshold: Double = DEFAULT_OVERLAP_THRESHOLD,
): List<T> {
    val merged = mutableListOf<T>()
    candidates.sortedWith(priority).forEach { candidate ->
        merged += candidate
        var grown = merged.lastIndex
        while (true) {
            val other = merged.indices.firstOrNull { index ->
                index != grown &&
                    sameGroup(merged[index], merged[grown]) &&
                    overlapOfSmaller(bounds(merged[index]), bounds(merged[grown])) >= threshold
            } ?: break
            // The earlier entry has the higher priority and keeps its value.
            val (kept, absorbed) = if (other < grown) other to grown else grown to other
            merged[kept] = withBounds(merged[kept], bounds(merged[kept]).union(bounds(merged[absorbed])))
            merged.removeAt(absorbed)
            grown = kept
        }
    }
    return merged
}

/** Fraction of the smaller rectangle covered by the intersection of [first] and [second]. */
internal fun overlapOfSmaller(first: ImageRect, second: ImageRect): Double {
    val intersection = first.intersect(second) ?: return 0.0
    return intersection.area.toDouble() / minOf(first.area, second.area)
}

/** Fraction of [region] that lies inside [area]. */
internal fun coverage(region: ImageRect, area: ImageRect): Double =
    (region.intersect(area)?.area ?: 0L).toDouble() / region.area

/**
 * The container that encloses most of [region], if any encloses at least [threshold] of it. Among equally enclosing
 * containers the tightest one wins.
 */
internal fun enclosingContainer(
    region: ImageRect,
    containers: List<ImageRect>,
    threshold: Double = DEFAULT_CONTAINMENT_THRESHOLD,
): ImageRect? {
    return containers
        .map { container -> container to coverage(region, container) }
        .filter { (_, coverage) -> coverage >= threshold }
        .maxWithOrNull(compareBy<Pair<ImageRect, Double>>({ it.second }, { -it.first.area }))
        ?.first
}

private const val DEFAULT_OVERLAP_THRESHOLD = 0.6
private const val DEFAULT_CONTAINMENT_THRESHOLD = 0.5
