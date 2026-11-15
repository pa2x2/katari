package eu.kanade.tachiyomi.ui.history.activity

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.activity.HistoryActivityCompletion
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import java.time.ZonedDateTime

class HistoryActivityDaysTest {

    @Test
    fun `an episode watched over several sessions is one part of each day, checked where it was finished`() {
        val evening = session(
            id = "evening",
            segment(chapterId = 11L, localDate = "2026-12-13", start = "2026-12-13T17:25", minutes = 34L),
            segment(chapterId = 12L, localDate = "2026-12-13", start = "2026-12-13T17:59", minutes = 18L),
            completions = listOf(HistoryActivityCompletion(chapterId = 11L, localDate = "2026-12-13")),
        )
        // Runs past midnight: episode 12 is finished on the second day only.
        val late = session(
            id = "late",
            segment(chapterId = 12L, localDate = "2026-12-13", start = "2026-12-13T23:50", minutes = 10L),
            segment(chapterId = 12L, localDate = "2026-12-14", start = "2026-12-14T00:00", minutes = 15L),
            completions = listOf(HistoryActivityCompletion(chapterId = 12L, localDate = "2026-12-14")),
        )

        // Watched again the next morning without finishing: only the late session's part is checked.
        val morning = session(
            id = "morning",
            segment(chapterId = 12L, localDate = "2026-12-14", start = "2026-12-14T09:00", minutes = 5L),
            completions = emptyList(),
        )

        val days = groupHistoryActivityDays(listOf(evening, late, morning))

        days.map { it.localDate.toString() } shouldBe listOf("2026-12-14", "2026-12-13")
        days[1].titles.single().parts.map { Triple(it.chapterId, it.durationMillis / MINUTE, it.finished) } shouldBe
            listOf(Triple(11L, 34L, true), Triple(12L, 28L, false))
        days[1].sessions.map { it.sessionId } shouldBe listOf("late", "evening")
        days[0].titles.single().parts.map { Triple(it.chapterId, it.durationMillis / MINUTE, it.finished) } shouldBe
            listOf(Triple(12L, 20L, true))
        days[0].sessions.map { it.sessionId to it.parts.single().finished } shouldBe
            listOf("morning" to false, "late" to true)
        days[0].spans.first().startMinute shouldBe 0
        days[1].spans.last().endMinute shouldBe 24 * 60
    }

    private fun session(
        id: String,
        vararg segments: HistoryActivitySegmentDetail,
        completions: List<HistoryActivityCompletion>,
    ) = HistoryActivitySessionDetail(
        sessionId = id,
        entryId = 1L,
        entryType = EntryType.ANIME,
        entryTitle = "Sintel",
        coverData = EntryCover(entryId = 1L, sourceId = 1L, isFavorite = true, url = null, lastModified = 0L),
        segments = segments.toList(),
        completions = completions,
    )

    private fun segment(
        chapterId: Long,
        localDate: String,
        start: String,
        minutes: Long,
    ): HistoryActivitySegmentDetail {
        val startedAt = ZonedDateTime.parse("$start:00+01:00[$ZONE]")
        return HistoryActivitySegmentDetail(
            chapterId = chapterId,
            chapterTitle = "Episode $chapterId",
            localDate = localDate,
            timeZoneId = ZONE,
            startedAtEpochMillis = startedAt.toInstant().toEpochMilli(),
            endedAtEpochMillis = startedAt.plusMinutes(minutes).toInstant().toEpochMilli(),
            durationMillis = minutes * MINUTE,
        )
    }

    private companion object {
        const val ZONE = "Europe/Warsaw"
        const val MINUTE = 60_000L
    }
}
