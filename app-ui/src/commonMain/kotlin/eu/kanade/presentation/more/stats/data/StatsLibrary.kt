package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryStatus

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
    val topGenres: List<StatsLabelCount>,
    val categoryCount: Int,
    /** Unread chapters or unwatched episodes, counted only for titles whose progress is known. */
    val unconsumedCount: Long,
    val titlesWithUnconsumed: Int,
    val downloadedCount: Long,
    val statusCounts: Map<EntryStatus, Int>,
    val topSources: List<StatsLabelCount>,
    val otherSourcesTitleCount: Int,
    val otherSourceCount: Int,
    /** Titles added in each month of [addedYear], January through the current month. */
    val addedByMonth: List<Int>,
    val addedYear: Int,
)

data class StatsLabelCount(
    val label: String,
    val count: Int,
)
