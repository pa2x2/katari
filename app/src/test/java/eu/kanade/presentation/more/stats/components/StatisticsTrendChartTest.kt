package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsTrendChartTest {

    @Test
    fun `axis ticks expose every day in seven day range`() {
        trendTickIndices(7) shouldBe listOf(0, 1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `axis ticks stay readable for longer ranges`() {
        trendTickIndices(30) shouldBe listOf(0, 6, 12, 17, 23, 29)
        trendTickIndices(53) shouldBe listOf(0, 13, 26, 39, 52)
    }

    @Test
    fun `vertical scale rounds up to a readable duration`() {
        niceTrendMaximum(70L * 60_000L) shouldBe 90L * 60_000L
        niceTrendMaximum(91L * 60_000L) shouldBe 120L * 60_000L
        niceTrendMaximum(0L) shouldBe 60_000L
    }

    @Test
    fun `edge touch regions select first and last points`() {
        trendPointIndexForPosition(
            positionX = 0f,
            width = 100f,
            pointCount = 30,
            horizontalInset = 12f,
        ) shouldBe 0
        trendPointIndexForPosition(
            positionX = 88f,
            width = 100f,
            pointCount = 30,
            horizontalInset = 12f,
        ) shouldBe 29
        trendPointIndexForPosition(
            positionX = 100f,
            width = 100f,
            pointCount = 30,
            horizontalInset = 12f,
        ) shouldBe 29
    }
}
