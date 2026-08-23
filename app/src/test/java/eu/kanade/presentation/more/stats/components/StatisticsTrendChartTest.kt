package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsTrendChartTest {

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
