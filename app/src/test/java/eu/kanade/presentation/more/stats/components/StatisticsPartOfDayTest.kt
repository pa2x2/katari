package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsPartOfDayTest {

    @Test
    fun `late hours on both sides of midnight add up to night`() {
        val hourly = MutableList(24) { 0L }.apply {
            this[23] = 30L
            this[1] = 30L
            this[20] = 50L
        }

        dominantPartOfDay(hourly) shouldBe StatisticsPartOfDay.NIGHT
        peakHour(hourly) shouldBe 20
    }
}
