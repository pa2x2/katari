package tachiyomi.domain.statistics.model

import eu.kanade.tachiyomi.source.entry.EntryType

data class StatisticsActivityBucket(
    val type: EntryType,
    val localDate: String,
    val durationMillis: Long,
)

data class StatisticsCompletionBucket(
    val type: EntryType,
    val localDate: String,
    val count: Long,
)

data class StatisticsActivityTimeline(
    val activity: List<StatisticsActivityBucket>,
    val completions: List<StatisticsCompletionBucket>,
)

data class StatisticsTopEntry(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
)

data class StatisticsEarlierActivity(
    val type: EntryType,
    val durationMillis: Long,
)

data class StatisticsSessionSummary(
    val type: EntryType,
    val sessionCount: Long,
    val averageDurationMillis: Long,
    val longestDurationMillis: Long,
)

data class StatisticsEarlierActivityDetails(
    val totals: List<StatisticsEarlierActivity>,
    val topEntries: List<StatisticsTopEntry>,
)

data class StatisticsActivitySnapshot(
    val profileId: Long,
    val trackingStartedAtEpochMillis: Long?,
    val activity: List<StatisticsActivityBucket>,
    val completions: List<StatisticsCompletionBucket>,
    val topEntries: List<StatisticsTopEntry>,
    val earlierActivity: List<StatisticsEarlierActivity>,
    val sessions: List<StatisticsSessionSummary> = emptyList(),
)
