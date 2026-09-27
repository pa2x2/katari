package mihon.text.recognition.runtime.geometry

import mihon.text.recognition.api.image.ImageRect

/**
 * Collapses detections of the same object that overlapping tiles reported more than once.
 *
 * Two rectangles describe the same object when their intersection covers most of the smaller one. The merged
 * rectangle is their union, which restores objects that one tile saw truncated at its edge. Candidates are visited in
 * [priority] order, so the most confident detection determines the surviving value.
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
        val index = merged.indexOfFirst { accepted ->
            sameGroup(accepted, candidate) && overlapOfSmaller(bounds(accepted), bounds(candidate)) >= threshold
        }
        if (index < 0) {
            merged += candidate
        } else {
            val accepted = merged[index]
            merged[index] = withBounds(accepted, bounds(accepted).union(bounds(candidate)))
        }
    }
    return merged
}

/** Fraction of the smaller rectangle covered by the intersection of [first] and [second]. */
internal fun overlapOfSmaller(first: ImageRect, second: ImageRect): Double {
    val intersection = first.intersect(second) ?: return 0.0
    return intersection.area.toDouble() / minOf(first.area, second.area)
}

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
        .map { container -> container to ((region.intersect(container)?.area ?: 0L).toDouble() / region.area) }
        .filter { (_, coverage) -> coverage >= threshold }
        .maxWithOrNull(compareBy<Pair<ImageRect, Double>>({ it.second }, { -it.first.area }))
        ?.first
}

private const val DEFAULT_OVERLAP_THRESHOLD = 0.6
private const val DEFAULT_CONTAINMENT_THRESHOLD = 0.5
