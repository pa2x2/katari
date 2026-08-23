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
) {
    val total: Int = notStarted + inProgress + caughtUp + completed
}

data class StatsLibrary(
    val totalTitles: Int,
    val titlesByType: Map<EntryType, Int>,
    val progress: StatsProgress?,
    val progressByType: Map<EntryType, StatsProgress>,
    val coverageByType: Map<EntryType, StatsLibraryCoverage>,
    val insightsByType: Map<EntryType, StatsLibraryInsights>,
)

data class StatsLibraryInsights(
    val topGenre: String?,
    val categoryCount: Int,
    val sourceCount: Int,
)

data class StatsLibraryCoverage(
    val offline: StatsOfflineCoverage?,
    val tracking: StatsTrackingCoverage,
)

data class StatsOfflineCoverage(
    val partlyOfflineTitles: Int,
    val fullyOfflineTitles: Int,
)

sealed interface StatsTrackingCoverage {
    data object Unsupported : StatsTrackingCoverage
    data object NotConnected : StatsTrackingCoverage
    data class Connected(val trackedTitles: Int, val totalTitles: Int) : StatsTrackingCoverage
}

data class StatsTrendPoint(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationByType: Map<EntryType, Long>,
) {
    val totalDurationMillis: Long = durationByType.values.sum()
}

data class StatsTopTitle(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
)

data class StatsActivity(
    val totalDurationMillis: Long,
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
    val topTitles: List<StatsTopTitle>,
    val trackingStartedAtEpochMillis: Long?,
    val earlierDurationMillis: Long,
    val earlierDurationByType: Map<EntryType, Long>,
)
