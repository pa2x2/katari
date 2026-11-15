package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsTrendChartGeometryTest {

    @Test
    fun `month labels too wide for their bars are thinned out from the latest one back`() {
        // Six labels like "Dec 2025" across a narrow chart: 50 px apart, 60 px wide each.
        val centers = (0 until 6).map { 20f + it * 50f }
        val widths = List(6) { 60f }

        trendLabelsThatFit(centers, widths, gap = 8f) shouldBe listOf(1, 3, 5)
        trendLabelsThatFit(centers, List(6) { 30f }, gap = 8f) shouldBe (0 until 6).toList()
    }
}
