package tachiyomi.domain.statistics.recap

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

/**
 * The raw activity a recap of one period is built from. Timed rows follow the same session threshold as the
 * Statistics dashboard, so a recap's totals match the dashboard's for the same days.
 */
data class StatisticsRecapActivity(
    val segments: List<StatisticsRecapSegment>,
    /** Every entry that has a segment in the period. */
    val entries: List<StatisticsRecapEntry>,
    val completions: List<StatisticsRecapCompletion>,
    /** Entries the source marks finished, with no unread items left, whose last item was finished in the period. */
    val finishedEntryIds: Set<Long>,
    /** The first day each of [entries] was ever active, including before the period. */
    val firstActiveDateByEntry: Map<Long, String>,
    /** The first day the profile was ever active; null when it never was. */
    val profileFirstActiveDate: String?,
)

/** One timed segment of an entry, ordered by [startedAtEpochMillis]. */
data class StatisticsRecapSegment(
    val entryId: Long,
    /** Null when the item was deleted, its number wasn't recognised, or the segment wasn't tied to one. */
    val itemNumber: Double?,
    val localDate: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val timeZoneId: String,
)

data class StatisticsRecapEntry(
    val id: Long,
    val type: EntryType,
    val title: String,
    val cover: EntryCover,
    /** As the source wrote them; casing and spelling differ between sources. */
    val genres: List<String>,
    val sourceId: Long,
)

/** A count of items finished by consumption, per entry and day. */
data class StatisticsRecapCompletion(
    val entryId: Long,
    val type: EntryType,
    val localDate: String,
    val count: Long,
)

/** Totals of a period a recap compares itself with. */
data class StatisticsRecapPeriodTotals(
    val durationMillis: Long,
    /** The entry with the most time; null when nothing was timed. */
    val topEntryId: Long?,
)
