package eu.kanade.presentation.more.stats.components

import kotlin.math.roundToInt

internal fun trendPointCenterX(
    index: Int,
    width: Float,
    pointCount: Int,
    horizontalInset: Float,
): Float {
    if (width <= 0f) return 0f
    if (pointCount <= 1) return width / 2f
    val inset = horizontalInset.coerceIn(0f, width / 2f)
    val chartWidth = (width - inset * 2f).coerceAtLeast(0f)
    return inset + chartWidth * index.coerceIn(0, pointCount - 1) / (pointCount - 1)
}

internal fun trendPointIndexForPosition(
    positionX: Float,
    width: Float,
    pointCount: Int,
    horizontalInset: Float,
): Int {
    if (pointCount <= 1 || width <= 0f) return 0
    val inset = horizontalInset.coerceIn(0f, width / 2f)
    val chartWidth = (width - inset * 2f).coerceAtLeast(1f)
    val fraction = ((positionX - inset) / chartWidth).coerceIn(0f, 1f)
    return (fraction * (pointCount - 1)).roundToInt()
}

internal fun trendTickIndices(pointCount: Int): List<Int> {
    if (pointCount <= 0) return emptyList()
    val tickCount = when {
        pointCount <= 7 -> pointCount
        pointCount <= 31 -> 6
        else -> 5
    }
    if (tickCount == 1) return listOf(0)
    return (0 until tickCount).map { tick ->
        (tick.toFloat() * (pointCount - 1) / (tickCount - 1)).roundToInt()
    }.distinct()
}

internal data class TrendDurationAxis(
    val maximumMillis: Long,
    val ticksDescending: List<Long>,
)

internal fun buildTrendDurationAxis(rawMaximumMillis: Long): TrendDurationAxis {
    val normalizedMaximum = rawMaximumMillis.coerceAtLeast(0L)
    val rawMinutes = if (normalizedMaximum == 0L) {
        1L
    } else {
        (normalizedMaximum - 1L) / 60_000L + 1L
    }
    val candidateSteps = buildList {
        addAll(listOf(1L, 2L, 5L, 10L, 15L, 20L, 30L, 45L, 60L, 90L, 120L, 180L, 240L, 360L, 480L, 720L))
        var dayMagnitude = 1_440L
        repeat(10) {
            listOf(1L, 2L, 5L).forEach { multiplier -> add(multiplier * dayMagnitude) }
            dayMagnitude *= 10L
        }
    }.distinct().sorted()
    val selected = candidateSteps
        .mapNotNull { step ->
            val requiredIntervals = ((rawMinutes - 1L) / step + 1L).coerceAtLeast(3L)
            if (requiredIntervals > 5L) return@mapNotNull null
            val maximum = step * requiredIntervals
            val intervalPenalty = kotlin.math.abs(requiredIntervals - 4L).toDouble()
            val headroomPenalty = (maximum - rawMinutes).toDouble() / rawMinutes
            DurationAxisCandidate(
                stepMinutes = step,
                intervals = requiredIntervals.toInt(),
                score = intervalPenalty + headroomPenalty,
            )
        }
        .minBy(DurationAxisCandidate::score)
    val ticks = (selected.intervals downTo 0).map { interval ->
        selected.stepMinutes * interval * 60_000L
    }
    return TrendDurationAxis(
        maximumMillis = ticks.first(),
        ticksDescending = ticks,
    )
}

private data class DurationAxisCandidate(
    val stepMinutes: Long,
    val intervals: Int,
    val score: Double,
)
