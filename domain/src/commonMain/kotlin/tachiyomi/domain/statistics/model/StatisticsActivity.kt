package tachiyomi.domain.statistics.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

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
    val cover: EntryCover,
    /** Consumption completions inside the queried window; always zero for earlier (undated) activity. */
    val completionCount: Long = 0L,
)

/** One timed segment, kept with its recorded zone so local clock hours stay correct across travel. */
data class StatisticsActivitySegment(
    val type: EntryType,
    val localDate: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val timeZoneId: String,
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
    val trackingStartedAtEpochMillis: Long?,
)

data class StatisticsActivitySnapshot(
    val profileId: Long,
    val trackingStartedAtEpochMillis: Long?,
    val activity: List<StatisticsActivityBucket>,
    val completions: List<StatisticsCompletionBucket>,
    val topEntries: List<StatisticsTopEntry>,
    val earlierActivity: List<StatisticsEarlierActivity>,
    val sessions: List<StatisticsSessionSummary> = emptyList(),
    val segments: List<StatisticsActivitySegment> = emptyList(),
)
