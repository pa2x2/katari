package eu.kanade.presentation.more.stats.data

sealed interface StatsData {

    data class Overview(
        val libraryEntryCount: Int,
        val completedEntryCount: Int?,
        val totalReadDuration: Long,
    ) : StatsData

    data class Titles(
        val globalUpdateItemCount: Int,
        val startedEntryCount: Int?,
        val localEntryCount: Int,
    ) : StatsData

    data class Chapters(
        val totalChapterCount: Int?,
        val readChapterCount: Int?,
        val downloadCount: Int,
    ) : StatsData

    data class Trackers(
        val trackedTitleCount: Int,
        val meanScore: Double,
        val trackerCount: Int,
    ) : StatsData
}
