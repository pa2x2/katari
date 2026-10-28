package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsTrendPoint

internal fun buildDisplayedNavigationTrend(
    points: List<StatsTrendPoint>,
    navigationPoints: List<StatsTrendPoint>,
): List<StatsTrendPoint> {
    val windowPoints = points.associateBy(StatsTrendPoint::bucketStartDate)
    // Navigation loads complete surrounding months. The selected window's edge months can
    // be partial, so their bars and scale must use the same totals as the selection details.
    return navigationPoints.map { windowPoints[it.bucketStartDate] ?: it }
}
