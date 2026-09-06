package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType

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
