package tachiyomi.domain.history.model.activity

import eu.kanade.tachiyomi.source.entry.EntryType

data class HistoryActivityPage(
    val sessions: List<HistoryActivitySessionDetail>,
    val hasMore: Boolean,
)

data class HistoryActivitySessionDetail(
    val sessionId: String,
    val entryId: Long,
    val entryType: EntryType,
    val entryTitle: String,
    val localDate: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val completionCount: Long,
    val segments: List<HistoryActivitySegmentDetail>,
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
