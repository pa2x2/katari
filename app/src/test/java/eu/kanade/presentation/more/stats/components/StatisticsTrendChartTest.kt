package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsTrendChartTest {

    @Test
    fun `point centers align to chart interval positions`() {
        trendPointCenterX(index = 0, width = 140f, pointCount = 7, horizontalInset = 10f) shouldBe 10f
        trendPointCenterX(index = 1, width = 140f, pointCount = 7, horizontalInset = 10f) shouldBe 30f
        trendPointCenterX(index = 5, width = 140f, pointCount = 7, horizontalInset = 10f) shouldBe 110f
        trendPointCenterX(index = 6, width = 140f, pointCount = 7, horizontalInset = 10f) shouldBe 130f
    }

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
    fun `vertical scale exposes granular rounded duration intervals`() {
        buildTrendDurationAxis(18L * 60_000L) shouldBe TrendDurationAxis(
            maximumMillis = 20L * 60_000L,
            ticksDescending = listOf(20L, 15L, 10L, 5L, 0L).map { it * 60_000L },
        )
        buildTrendDurationAxis(32L * 60_000L) shouldBe TrendDurationAxis(
            maximumMillis = 40L * 60_000L,
            ticksDescending = listOf(40L, 30L, 20L, 10L, 0L).map { it * 60_000L },
        )
        buildTrendDurationAxis(329L * 60_000L) shouldBe TrendDurationAxis(
            maximumMillis = 360L * 60_000L,
            ticksDescending = listOf(360L, 270L, 180L, 90L, 0L).map { it * 60_000L },
        )
        buildTrendDurationAxis(0L) shouldBe TrendDurationAxis(
            maximumMillis = 3L * 60_000L,
            ticksDescending = listOf(3L, 2L, 1L, 0L).map { it * 60_000L },
        )
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

    @Test
    fun `settled scrolling retains the exact fractional position`() {
        settleTrendOffset(
            offsetPx = 34f,
            pointSpacingPx = 10f,
            olderBucketCount = 10,
            newerBucketCount = 10,
        ) shouldBe SettledTrendOffset(bucketShift = 3, residualOffsetPx = 4f)

        settleTrendOffset(
            offsetPx = -26f,
            pointSpacingPx = 10f,
            olderBucketCount = 10,
            newerBucketCount = 10,
        ) shouldBe SettledTrendOffset(bucketShift = -3, residualOffsetPx = 4f)
    }

    @Test
    fun `sub-bucket scrolling stays where the user stopped`() {
        settleTrendOffset(
            offsetPx = 4f,
            pointSpacingPx = 10f,
            olderBucketCount = 10,
            newerBucketCount = 10,
        ) shouldBe SettledTrendOffset(bucketShift = 0, residualOffsetPx = 4f)
    }
}
