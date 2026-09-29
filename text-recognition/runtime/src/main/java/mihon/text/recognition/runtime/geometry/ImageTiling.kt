package mihon.text.recognition.runtime.geometry

import mihon.text.recognition.api.image.ImageRect

/**
 * Splits [area] along its longer side into overlapping tiles whose aspect ratio does not exceed [maximumAspect].
 *
 * Detectors resize their whole input to a fixed square, so a tall webtoon strip read in one piece would shrink its
 * text below legibility. Overlap guarantees that every region smaller than the overlap lies whole in some tile.
 *
 * Tiles are never shorter than [minimumLength], the edge a component reads at full resolution: splitting a narrow
 * area below it would only stretch each tile further and cut text that fits into one tile apart.
 */
internal fun planTiles(
    area: ImageRect,
    minimumLength: Int = 1,
    maximumAspect: Double = DEFAULT_MAXIMUM_TILE_ASPECT,
    overlap: Double = DEFAULT_TILE_OVERLAP,
): List<ImageRect> {
    require(maximumAspect >= 1.0)
    require(overlap in 0.0..<1.0)
    val vertical = area.height >= area.width
    val shortSide = if (vertical) area.width else area.height
    val longSide = if (vertical) area.height else area.width
    val tileLength = maxOf((shortSide * maximumAspect).toInt(), minimumLength, 1)
    if (longSide <= tileLength) return listOf(area)

    val step = (tileLength * (1 - overlap)).toInt().coerceAtLeast(1)
    val starts = generateSequence(0) { it + step }
        .takeWhile { it + tileLength < longSide }
        .toList() + (longSide - tileLength)
    return starts.distinct().map { start ->
        if (vertical) {
            ImageRect(area.left, area.top + start, area.right, area.top + start + tileLength)
        } else {
            ImageRect(area.left + start, area.top, area.left + start + tileLength, area.bottom)
        }
    }
}

/** Largest power-of-two subsampling that keeps the shorter side of [region] at least [minimumEdge] pixels. */
internal fun sampleSizeForMinimumEdge(region: ImageRect, minimumEdge: Int): Int {
    val shortSide = minOf(region.width, region.height)
    var sampleSize = 1
    while (shortSide / (sampleSize * 2) >= minimumEdge) sampleSize *= 2
    return sampleSize
}

/** Maps a rectangle in a bitmap decoded from [source] with [sampleSize] back to source pixels, clipped to [source]. */
internal fun ImageRect.toSource(source: ImageRect, sampleSize: Int): ImageRect? {
    val mapped = ImageRect(
        left = source.left + left * sampleSize,
        top = source.top + top * sampleSize,
        right = (source.left + right * sampleSize).coerceAtLeast(source.left + left * sampleSize + 1),
        bottom = (source.top + bottom * sampleSize).coerceAtLeast(source.top + top * sampleSize + 1),
    )
    return mapped.intersect(source)
}

/** Grows the rectangle by [fraction] of its longer side on every edge, clipped to [limit]. */
internal fun ImageRect.padded(fraction: Double, limit: ImageRect): ImageRect {
    val padding = (maxOf(width, height) * fraction).toInt().coerceAtLeast(MINIMUM_PADDING)
    return ImageRect(
        left = (left - padding).coerceAtLeast(limit.left),
        top = (top - padding).coerceAtLeast(limit.top),
        right = (right + padding).coerceAtMost(limit.right),
        bottom = (bottom + padding).coerceAtMost(limit.bottom),
    )
}

private const val DEFAULT_MAXIMUM_TILE_ASPECT = 1.5
private const val DEFAULT_TILE_OVERLAP = 0.25
private const val MINIMUM_PADDING = 2
