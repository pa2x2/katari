package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class StatisticsDateWindowTest {

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
}
