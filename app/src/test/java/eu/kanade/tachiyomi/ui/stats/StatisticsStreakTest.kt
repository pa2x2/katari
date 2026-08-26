package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import java.time.LocalDate
import java.util.Locale

class StatisticsStreakTest {

    @Test
    fun `latest streak remains active while today is incomplete`() {
        val timeline = timelineWithActivityOn("2026-08-23", "2026-08-24", "2026-08-25")

        timeline.streakEndingOn(
            endDate = LocalDate.parse("2026-08-26"),
            preserveThroughIncompleteEndDate = true,
        ) shouldBe 3
    }

    @Test
    fun `latest streak includes today after today qualifies`() {
        val timeline = timelineWithActivityOn("2026-08-24", "2026-08-25", "2026-08-26")

        timeline.streakEndingOn(
            endDate = LocalDate.parse("2026-08-26"),
            preserveThroughIncompleteEndDate = true,
        ) shouldBe 3
    }

    @Test
    fun `historical streak requires its completed end date to qualify`() {
        val timeline = timelineWithActivityOn("2026-08-23", "2026-08-24", "2026-08-25")

        timeline.streakEndingOn(
            endDate = LocalDate.parse("2026-08-26"),
            preserveThroughIncompleteEndDate = false,
        ) shouldBe 0
    }

    @Test
    fun `streak uses activity before the selected statistics range`() {
        val endDate = LocalDate.parse("2026-08-26")
        val streakTimeline = timelineWithActivityOn(
            "2026-08-17",
            "2026-08-18",
            "2026-08-19",
            "2026-08-20",
            "2026-08-21",
            "2026-08-22",
            "2026-08-23",
            "2026-08-24",
            "2026-08-25",
        )
        val result = buildWindowActivity(
            snapshot = StatisticsActivitySnapshot(
                profileId = 1L,
                trackingStartedAtEpochMillis = null,
                activity = streakTimeline.activity.filter { it.localDate >= "2026-08-20" },
                completions = emptyList(),
                topEntries = emptyList(),
                earlierActivity = emptyList(),
            ),
            window = StatsRange.SEVEN_DAYS.windowEndingOn(endDate, isLatest = true),
            types = listOf(EntryType.MANGA),
            locale = Locale.US,
            streakTimeline = streakTimeline,
        )

        result.currentStreakDays shouldBe 9
        result.currentStreakDaysByType[EntryType.MANGA] shouldBe 9
    }

    @Test
    fun `completion qualifies a day without one minute of timed activity`() {
        val timeline = StatisticsActivityTimeline(
            activity = emptyList(),
            completions = listOf(
                StatisticsCompletionBucket(EntryType.MANGA, "2026-08-25", 1L),
            ),
        )

        timeline.streakEndingOn(
            endDate = LocalDate.parse("2026-08-26"),
            type = EntryType.MANGA,
            preserveThroughIncompleteEndDate = true,
        ) shouldBe 1
    }

    private fun timelineWithActivityOn(vararg localDates: String) = StatisticsActivityTimeline(
        activity = localDates.map { localDate ->
            StatisticsActivityBucket(EntryType.MANGA, localDate, 60_000L)
        },
        completions = emptyList(),
    )
}
