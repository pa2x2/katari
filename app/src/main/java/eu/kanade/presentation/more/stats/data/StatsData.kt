package eu.kanade.presentation.more.stats.data

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import java.time.LocalDate

enum class StatsRange {
    SEVEN_DAYS,
    THIRTY_DAYS,
    ONE_YEAR,
    ALL,
}

data class StatsType(
    val type: EntryType,
    val displayName: StringResource,
    val icon: ImageVector,
    val accent: EntryStatisticsAccent,
    val consumedUnitLabel: StringResource,
)

data class StatsProgress(
    val notStarted: Int,
    val inProgress: Int,
    val caughtUp: Int,
    val completed: Int,
    val unavailable: Int = 0,
) {
    val total: Int = notStarted + inProgress + caughtUp + completed
    val libraryTotal: Int = total + unavailable
    val isPartial: Boolean = unavailable > 0
}

data class StatsLibrary(
    val totalTitles: Int,
    val titlesByType: Map<EntryType, Int>,
    val progress: StatsProgress?,
    val progressByType: Map<EntryType, StatsProgress>,
    val insightsByType: Map<EntryType, StatsLibraryInsights>,
)

data class StatsLibraryInsights(
    val topGenre: String?,
    val categoryCount: Int,
)

data class StatsTrendPoint(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationByType: Map<EntryType, Long>,
    val completionCountByType: Map<EntryType, Long> = emptyMap(),
    val trackedStartDate: LocalDate? = startDate,
    val bucketStartDate: LocalDate = startDate,
) {
    val totalDurationMillis: Long = durationByType.values.sum()
    val completionCount: Long = completionCountByType.values.sum()
    val isTracked: Boolean = trackedStartDate != null
}

enum class StatsTrendGranularity {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

data class StatsTopTitle(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
)

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
)
