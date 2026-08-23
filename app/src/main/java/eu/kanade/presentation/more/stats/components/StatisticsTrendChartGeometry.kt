package eu.kanade.presentation.more.stats.components

import kotlin.math.roundToInt

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

internal fun niceTrendMaximum(rawMaximumMillis: Long): Long {
    val minutes = ((rawMaximumMillis.coerceAtLeast(0L) + 59_999L) / 60_000L).coerceAtLeast(1L)
    val commonMinutes = longArrayOf(
        1L,
        2L,
        5L,
        10L,
        15L,
        30L,
        45L,
        60L,
        90L,
        120L,
        180L,
        240L,
        360L,
        480L,
        720L,
        1_440L,
    )
    val ceilingMinutes = commonMinutes.firstOrNull { it >= minutes } ?: run {
        val days = (minutes + 1_439L) / 1_440L
        niceWholeNumberCeiling(days) * 1_440L
    }
    return ceilingMinutes * 60_000L
}

private fun niceWholeNumberCeiling(value: Long): Long {
    var magnitude = 1L
    while (value > magnitude * 10L) magnitude *= 10L
    return longArrayOf(1L, 2L, 5L, 10L)
        .map { it * magnitude }
        .first { it >= value }
}
