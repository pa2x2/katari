package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.LocalDate

data class StatsActivity(
    val window: StatsActivityWindow,
    val totalDurationMillis: Long,
    val totalDurationByType: Map<EntryType, Long>,
    val currentStreakDays: Int,
    val currentStreakDaysByType: Map<EntryType, Int>,
    val completionCount: Long,
    val completionCountByType: Map<EntryType, Long>,
    val sessionCount: Long,
    val sessionCountByType: Map<EntryType, Long>,
    val averageSessionDurationMillis: Long,
    val averageSessionDurationByType: Map<EntryType, Long>,
    val longestSessionDurationMillis: Long,
    val longestSessionDurationByType: Map<EntryType, Long>,
    val activeDays: Int,
    val activeDaysByType: Map<EntryType, Int>,
    val trend: List<StatsTrendPoint>,
    val navigationTrend: List<StatsTrendPoint>,
    val topTitles: List<StatsTopTitle>,
    val trackingStartedAtEpochMillis: Long?,
    val trackingStartDate: LocalDate?,
    val earlierDurationMillis: Long,
    val earlierDurationByType: Map<EntryType, Long>,
    val trendGranularity: StatsTrendGranularity = StatsTrendGranularity.DAY,
    val allRangeMonthlyTrend: List<StatsTrendPoint> = emptyList(),
    /** Same-length window just before this one; null when it isn't fully tracked or the range is All. */
    val previousWindow: StatsActivityWindow? = null,
    val previousTotalDurationMillis: Long? = null,
    val previousTotalDurationByType: Map<EntryType, Long> = emptyMap(),
    /** Days of the window on or after tracking started, used as the daily-average denominator. */
    val trackedDayCount: Int = 0,
    val longestStreakDays: Int = 0,
    val longestStreakDaysByType: Map<EntryType, Int> = emptyMap(),
    val rhythm: StatsActivityRhythm = StatsActivityRhythm.EMPTY,
    val rhythmByType: Map<EntryType, StatsActivityRhythm> = emptyMap(),
)
