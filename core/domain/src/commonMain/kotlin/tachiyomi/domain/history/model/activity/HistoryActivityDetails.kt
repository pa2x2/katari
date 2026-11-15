package tachiyomi.domain.history.model.activity

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

/**
 * Whole days of activity, newest first: every session with a segment on one of the page's days, with only its segments
 * and completions on those days. A session that runs past midnight is in both days' pages, each with its own part.
 */
data class HistoryActivityPage(
    val sessions: List<HistoryActivitySessionDetail>,
    /** Days of the page, newest first, `yyyy-MM-dd`. */
    val localDates: List<String>,
    val hasMore: Boolean,
)

data class HistoryActivitySessionDetail(
    val sessionId: String,
    val entryId: Long,
    val entryType: EntryType,
    val entryTitle: String,
    val coverData: EntryCover,
    val segments: List<HistoryActivitySegmentDetail>,
    val completions: List<HistoryActivityCompletion>,
)

data class HistoryActivitySegmentDetail(
    val chapterId: Long?,
    val chapterTitle: String?,
    val localDate: String,
    val timeZoneId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
)

/** A chapter the session finished by reading or watching it to its end, on [localDate]. */
data class HistoryActivityCompletion(
    val chapterId: Long,
    val localDate: String,
)
