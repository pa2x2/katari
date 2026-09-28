package eu.kanade.tachiyomi.ui.stats

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate
import java.util.Locale

class StatisticsReadingCalendarTest {

    @Test
    fun `calendar starts on the locale week start one year before today's week`() {
        val today = LocalDate.parse("2026-09-28")

        readingCalendarStart(today, Locale.UK) shouldBe LocalDate.parse("2025-09-29")
        readingCalendarStart(today, Locale.US) shouldBe LocalDate.parse("2025-09-28")
    }

    @Test
    fun `calendar keeps per-type time for days inside its span`() {
        val start = LocalDate.parse("2025-09-29")
        val today = LocalDate.parse("2026-09-28")
        val calendar = buildReadingCalendar(
            timeline = StatisticsActivityTimeline(
                activity = listOf(
                    StatisticsActivityBucket(EntryType.MANGA, "2025-09-28", 60_000L),
                    StatisticsActivityBucket(EntryType.MANGA, "2026-09-27", 60_000L),
                    StatisticsActivityBucket(EntryType.BOOK, "2026-09-27", 30_000L),
                ),
                completions = emptyList(),
            ),
            startDate = start,
            today = today,
        )

        calendar.durationByDate.keys shouldBe setOf(LocalDate.parse("2026-09-27"))
        calendar.durationOn(LocalDate.parse("2026-09-27"), null) shouldBe 90_000L
        calendar.durationOn(LocalDate.parse("2026-09-27"), EntryType.BOOK) shouldBe 30_000L
    }
}
