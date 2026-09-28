package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatisticsTrendPeriodSummaryTest {

    @Test
    fun `daily average only counts days since tracking started`() {
        val summary = summarizeTrendPeriod(
            listOf(
                week("2026-08-03", trackedFrom = null, manga = 0L),
                week("2026-08-10", trackedFrom = "2026-08-14", manga = 4 * HOUR),
                week("2026-08-17", trackedFrom = "2026-08-17", manga = 3 * HOUR, anime = HOUR),
            ),
        )

        summary.totalDurationMillis shouldBe 8 * HOUR
        summary.durationByType shouldBe mapOf(EntryType.MANGA to 7 * HOUR, EntryType.ANIME to HOUR)
        summary.trackedDays shouldBe 10L
        summary.dailyAverageMillis shouldBe 8 * HOUR / 10
    }

    @Test
    fun `busiest bucket is the one with the most time and absent when nothing was recorded`() {
        val busiest = week("2026-08-17", trackedFrom = "2026-08-17", manga = 5 * HOUR)

        summarizeTrendPeriod(listOf(week("2026-08-10", "2026-08-10", manga = HOUR), busiest)).busiest shouldBe busiest
        summarizeTrendPeriod(listOf(week("2026-08-10", "2026-08-10", manga = 0L))).busiest shouldBe null
    }

    private fun week(start: String, trackedFrom: String?, manga: Long, anime: Long = 0L): StatsTrendPoint {
        val startDate = LocalDate.parse(start)
        return StatsTrendPoint(
            startDate = startDate,
            endDate = startDate.plusDays(6L),
            durationByType = buildMap {
                if (manga > 0L) put(EntryType.MANGA, manga)
                if (anime > 0L) put(EntryType.ANIME, anime)
            },
            trackedStartDate = trackedFrom?.let(LocalDate::parse),
        )
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
