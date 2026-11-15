package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * One day of an activity list: what each title took that day, each chapter or episode once with the time of every
 * session added up, and the sessions themselves for when the day is opened up.
 */
@Immutable
data class HistoryActivityDay(
    val localDate: LocalDate,
    val durationMillis: Long,
    val startedAt: ZonedDateTime,
    val endedAt: ZonedDateTime,
    /** When time was spent, for the day's hours ribbon. */
    val spans: List<HistoryActivitySpan>,
    /** Most time first. */
    val titles: List<HistoryActivityDayTitle>,
    /** Latest first. */
    val sessions: List<HistoryActivityDaySession>,
)

/** A stretch of a day, in minutes since its midnight. */
@Immutable
data class HistoryActivitySpan(
    val type: EntryType,
    val startMinute: Int,
    val endMinute: Int,
)

@Immutable
data class HistoryActivityDayTitle(
    val entryId: Long,
    val entryType: EntryType,
    val entryTitle: String,
    val coverData: EntryCover,
    val durationMillis: Long,
    /** In the order they were first opened that day. */
    val parts: List<HistoryActivityPart>,
)

/**
 * A chapter or episode within a day or a session. [title] is null when the time was recorded without one, or the
 * chapter has since been removed.
 */
@Immutable
data class HistoryActivityPart(
    val chapterId: Long?,
    val title: String?,
    val durationMillis: Long,
    val finished: Boolean,
)

@Immutable
data class HistoryActivityDaySession(
    val sessionId: String,
    val startedAt: ZonedDateTime,
    val endedAt: ZonedDateTime,
    val durationMillis: Long,
    val parts: List<HistoryActivityPart>,
)

/**
 * Groups a page of sessions into its days, newest first. A session that runs past midnight counts in each day by the
 * segments recorded on it.
 */
fun groupHistoryActivityDays(sessions: List<HistoryActivitySessionDetail>): List<HistoryActivityDay> =
    sessions
        .flatMap { session -> session.segments.map { segment -> session to segment } }
        .groupBy { (_, segment) -> segment.localDate }
        .map { (localDate, daySegments) -> buildDay(LocalDate.parse(localDate), daySegments) }
        .sortedByDescending(HistoryActivityDay::localDate)

private fun buildDay(
    date: LocalDate,
    daySegments: List<Pair<HistoryActivitySessionDetail, HistoryActivitySegmentDetail>>,
): HistoryActivityDay {
    val finishedChapters = daySegments.asSequence()
        .map { (session, _) -> session }
        .distinctBy(HistoryActivitySessionDetail::sessionId)
        .flatMap { it.finishedChapters(date) }
        .toSet()
    val segments = daySegments.map { it.second }
    val titles = daySegments
        .groupBy { (session, _) -> session.entryId }
        .map { (_, entrySegments) ->
            val session = entrySegments.first().first
            HistoryActivityDayTitle(
                entryId = session.entryId,
                entryType = session.entryType,
                entryTitle = session.entryTitle,
                coverData = session.coverData,
                durationMillis = entrySegments.sumOf { it.second.durationMillis },
                parts = entrySegments.map { it.second }.toParts(finishedChapters),
            )
        }
        .sortedByDescending(HistoryActivityDayTitle::durationMillis)
    val sessions = daySegments
        .groupBy { (session, _) -> session.sessionId }
        .map { (sessionId, sessionSegments) ->
            val parts = sessionSegments.map { it.second }
            HistoryActivityDaySession(
                sessionId = sessionId,
                startedAt = parts.minOf { it.startedAtEpochMillis }.atZoneOf(parts.first()),
                endedAt = parts.maxOf { it.endedAtEpochMillis }.atZoneOf(parts.first()),
                durationMillis = parts.sumOf { it.durationMillis },
                parts = parts.toParts(sessionSegments.first().first.finishedChapters(date).toSet()),
            )
        }
        .sortedByDescending(HistoryActivityDaySession::startedAt)
    return HistoryActivityDay(
        localDate = date,
        durationMillis = segments.sumOf { it.durationMillis },
        startedAt = segments.minOf { it.startedAtEpochMillis }.atZoneOf(segments.first()),
        endedAt = segments.maxOf { it.endedAtEpochMillis }.atZoneOf(segments.first()),
        spans = daySegments
            .sortedBy { (_, segment) -> segment.startedAtEpochMillis }
            .map { (session, segment) -> segment.span(date, session.entryType) },
        titles = titles,
        sessions = sessions,
    )
}

private fun HistoryActivitySessionDetail.finishedChapters(date: LocalDate): Sequence<Long> =
    completions.asSequence().filter { it.localDate == date.toString() }.map { it.chapterId }

/**
 * Chapters a session finished count only on the day it was finished, so a chapter left the evening before and finished
 * after midnight is marked on the second day.
 */
private fun List<HistoryActivitySegmentDetail>.toParts(finishedChapters: Set<Long>): List<HistoryActivityPart> =
    sortedBy { it.startedAtEpochMillis }
        .groupBy { it.chapterId }
        .map { (chapterId, chapterSegments) ->
            HistoryActivityPart(
                chapterId = chapterId,
                title = chapterSegments.firstNotNullOfOrNull { it.chapterTitle?.takeIf(String::isNotBlank) },
                durationMillis = chapterSegments.sumOf { it.durationMillis },
                finished = chapterId != null && chapterId in finishedChapters,
            )
        }

private fun HistoryActivitySegmentDetail.span(date: LocalDate, type: EntryType): HistoryActivitySpan {
    val zone = zone()
    val midnight = date.atStartOfDay(zone)
    val minutes = { epochMillis: Long ->
        ChronoUnit.MINUTES.between(midnight, Instant.ofEpochMilli(epochMillis).atZone(zone))
            .coerceIn(0L, MINUTES_PER_DAY)
            .toInt()
    }
    return HistoryActivitySpan(type, minutes(startedAtEpochMillis), minutes(endedAtEpochMillis))
}

private fun Long.atZoneOf(segment: HistoryActivitySegmentDetail): ZonedDateTime =
    Instant.ofEpochMilli(this).atZone(segment.zone())

private fun HistoryActivitySegmentDetail.zone(): ZoneId =
    runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())

private const val MINUTES_PER_DAY = 24L * 60L
