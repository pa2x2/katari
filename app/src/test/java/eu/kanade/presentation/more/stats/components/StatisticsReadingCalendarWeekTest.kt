package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatisticsReadingCalendarWeekTest {

    @Test
    fun `columns start on the locale week start and leave days outside the calendar empty`() {
        val weeks = readingCalendarWeeks(
            startDate = LocalDate.parse("2026-09-02"),
            endDate = LocalDate.parse("2026-09-16"),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        weeks.map { it.days[1] } shouldBe listOf(null, LocalDate.parse("2026-09-07"), LocalDate.parse("2026-09-14"))
        weeks.first().days.take(4) shouldBe listOf(null, null, null, LocalDate.parse("2026-09-02"))
        weeks.last().days.drop(3) shouldBe listOf(LocalDate.parse("2026-09-16"), null, null, null)
    }
}
