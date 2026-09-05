package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
}
