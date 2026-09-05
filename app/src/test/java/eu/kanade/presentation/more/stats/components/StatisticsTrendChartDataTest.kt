package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatisticsTrendChartDataTest {

    @Test
    fun `partial month bars use selected window totals while retaining surrounding navigation`() {
        val august = StatsTrendPoint(
            startDate = LocalDate.parse("2026-08-01"),
            endDate = LocalDate.parse("2026-08-31"),
            durationByType = mapOf(EntryType.BOOK to 300_000L),
        )
        val september = StatsTrendPoint(
            startDate = LocalDate.parse("2026-09-01"),
            endDate = LocalDate.parse("2026-09-30"),
            durationByType = mapOf(EntryType.BOOK to 600_000L),
        )
        val partialAugust = august.copy(
            endDate = LocalDate.parse("2026-08-05"),
            durationByType = mapOf(EntryType.BOOK to 60_000L),
        )

        buildDisplayedNavigationTrend(
            points = listOf(partialAugust),
            navigationPoints = listOf(august, september),
        ) shouldBe listOf(partialAugust, september)

        val partialSeptember = september.copy(
            startDate = LocalDate.parse("2026-09-06"),
            durationByType = mapOf(EntryType.BOOK to 120_000L),
        )
        buildDisplayedNavigationTrend(
            points = listOf(partialSeptember),
            navigationPoints = listOf(august, september),
        ) shouldBe listOf(august, partialSeptember)
    }
}
