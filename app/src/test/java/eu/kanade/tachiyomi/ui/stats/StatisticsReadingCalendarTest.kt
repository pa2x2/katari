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
}
