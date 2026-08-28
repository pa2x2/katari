package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatisticsTrendSelectionTest {

    @Test
    fun `completion-only point does not open timed activity`() {
        val point = point(durationMillis = 0L, completionCount = 1L)

        isTrendSelectionActionable(point, hasAlternateAction = false) shouldBe false
    }

    @Test
    fun `recorded duration opens timed activity`() {
        val point = point(durationMillis = 60_000L, completionCount = 0L)

        isTrendSelectionActionable(point, hasAlternateAction = false) shouldBe true
    }

    private fun point(durationMillis: Long, completionCount: Long): StatsTrendPoint = StatsTrendPoint(
        startDate = LocalDate.parse("2026-08-23"),
        endDate = LocalDate.parse("2026-08-23"),
        durationByType = mapOf(EntryType.MANGA to durationMillis),
        completionCountByType = mapOf(EntryType.MANGA to completionCount),
    )
}
