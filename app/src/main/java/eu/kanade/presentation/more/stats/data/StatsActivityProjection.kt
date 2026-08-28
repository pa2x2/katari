package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType

internal fun StatsActivity.forType(type: EntryType?): StatsActivity {
    if (type == null) return this
    return copy(
        totalDurationMillis = totalDurationByType[type] ?: 0L,
        totalDurationByType = mapOf(type to (totalDurationByType[type] ?: 0L)),
        completionCount = completionCountByType[type] ?: 0L,
        completionCountByType = mapOf(type to (completionCountByType[type] ?: 0L)),
        sessionCount = sessionCountByType[type] ?: 0L,
        sessionCountByType = mapOf(type to (sessionCountByType[type] ?: 0L)),
        averageSessionDurationMillis = averageSessionDurationByType[type] ?: 0L,
        averageSessionDurationByType = mapOf(type to (averageSessionDurationByType[type] ?: 0L)),
        longestSessionDurationMillis = longestSessionDurationByType[type] ?: 0L,
        longestSessionDurationByType = mapOf(type to (longestSessionDurationByType[type] ?: 0L)),
        activeDays = activeDaysByType[type] ?: 0,
        activeDaysByType = mapOf(type to (activeDaysByType[type] ?: 0)),
        trend = trend.map { point -> point.forType(type) },
        navigationTrend = navigationTrend.map { point -> point.forType(type) },
        allRangeMonthlyTrend = allRangeMonthlyTrend.map { point -> point.forType(type) },
        topTitles = topTitles.filter { it.type == type },
        earlierDurationMillis = earlierDurationByType[type] ?: 0L,
        earlierDurationByType = mapOf(type to (earlierDurationByType[type] ?: 0L)),
    )
}

private fun StatsTrendPoint.forType(type: EntryType): StatsTrendPoint = copy(
    durationByType = mapOf(type to (durationByType[type] ?: 0L)),
    completionCountByType = mapOf(type to (completionCountByType[type] ?: 0L)),
)
