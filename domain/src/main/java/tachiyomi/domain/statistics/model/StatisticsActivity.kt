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

data class StatisticsTopEntry(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val durationMillis: Long,
)

data class StatisticsActivitySnapshot(
    val profileId: Long,
    val trackingStartedAtEpochMillis: Long?,
    val activity: List<StatisticsActivityBucket>,
    val completions: List<StatisticsCompletionBucket>,
    val topEntries: List<StatisticsTopEntry>,
)
