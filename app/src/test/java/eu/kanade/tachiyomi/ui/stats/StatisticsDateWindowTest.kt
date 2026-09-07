package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class StatisticsDateWindowTest {

    @Test
    fun `year navigation moves by calendar months and clamps to shorter months`() {
        val window = StatsRange.ONE_YEAR.windowEndingOn(LocalDate.parse("2024-03-31"))

        val older = window.shiftedByBuckets(1)
        older.endDate shouldBe LocalDate.parse("2024-02-29")
        older.startDate shouldBe LocalDate.parse("2023-03-01")

        window.shiftedByBuckets(-1).endDate shouldBe LocalDate.parse("2024-04-30")
        window.shiftedByBuckets(3).endDate shouldBe LocalDate.parse("2023-12-31")
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-03-31", "2024-03-31", "2026-05-31", "2026-03-30", "2026-09-07"])
    fun `month navigation restores the latest period after a shorter month`(date: String) {
        val today = LocalDate.parse(date)
        val original = StatsRange.ONE_YEAR.windowEndingOn(today, isLatest = true)
        val restored = original.shiftedByBuckets(1)
            .clampedTo(today.minusYears(3), today)
            .shiftedByBuckets(-1)
            .clampedTo(today.minusYears(3), today)

        restored.endDate shouldBe today
        restored.startDate shouldBe original.startDate
        restored.isLatest shouldBe true
    }

    @Test
    fun `repeated month steps retain the preferred day across short months`() {
        var window = StatsRange.ONE_YEAR.windowEndingOn(LocalDate.parse("2026-03-31"))
        for (expected in listOf("2026-02-28", "2026-01-31", "2025-12-31")) {
            window = window.shiftedByBuckets(1)
            window.endDate shouldBe LocalDate.parse(expected)
        }
        for (expected in listOf("2026-01-31", "2026-02-28", "2026-03-31")) {
            window = window.shiftedByBuckets(-1)
            window.endDate shouldBe LocalDate.parse(expected)
        }
    }

    @Test
    fun `range changes retain the month anchor while a day navigation selects a new day`() {
        val today = LocalDate.parse("2026-03-31")
        val february = StatsRange.ONE_YEAR.windowEndingOn(today).shiftedByBuckets(1)
        val sevenDays = StatsRange.SEVEN_DAYS.windowForSelection(february, today)
        val year = StatsRange.ONE_YEAR.windowForSelection(sevenDays, today)

        year.shiftedByBuckets(-1).endDate shouldBe today

        val daySelection = sevenDays.shiftedByBuckets(1)
        StatsRange.ONE_YEAR.windowForSelection(daySelection, today)
            .shiftedByBuckets(-1).endDate shouldBe LocalDate.parse("2026-03-27")
    }

    @Test
    fun `month navigation respects tracking bounds and Today establishes a fresh anchor`() {
        val today = LocalDate.parse("2026-04-30")
        val historical = StatsRange.ONE_YEAR.windowEndingOn(LocalDate.parse("2026-03-31"))
        val earliest = LocalDate.parse("2026-02-12")
        val bounded = historical.shiftedByBuckets(2).clampedTo(earliest, today)

        bounded.endDate shouldBe earliest
        bounded.shiftedByBuckets(-1).endDate shouldBe LocalDate.parse("2026-03-31")
        historical.shiftedByBuckets(-2).clampedTo(earliest, today).endDate shouldBe today

        val latest = StatsRange.ONE_YEAR.windowForSelection(null, today)
        latest.isLatest shouldBe true
        latest.shiftedByBuckets(1).endDate shouldBe LocalDate.parse("2026-03-30")
    }
}
