package eu.kanade.presentation.more.stats.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsDurationFormatterTest {

    private val units = StatisticsDurationUnits(
        hours = { "${it}h" },
        minutes = { "${it}m" },
        seconds = { "${it}s" },
        lessThanMinute = "<1 min",
    )

    private fun format(millis: Long, precision: StatisticsDurationPrecision) =
        formatStatisticsDuration(millis, precision, units)

    @Test
    fun `minute precision distinguishes no activity from sub-minute activity`() {
        format(0L, StatisticsDurationPrecision.MINUTES) shouldBe "0m"
        format(45_000L, StatisticsDurationPrecision.MINUTES) shouldBe "<1 min"
        format(61_000L, StatisticsDurationPrecision.MINUTES) shouldBe "1m"
    }

    @Test
    fun `second precision keeps seconds only below an hour`() {
        format(45_000L, StatisticsDurationPrecision.SECONDS) shouldBe "45s"
        format(252_000L, StatisticsDurationPrecision.SECONDS) shouldBe "4m 12s"
        format(3_725_000L, StatisticsDurationPrecision.SECONDS) shouldBe "1h 2m"
    }

    @Test
    fun `whole hours omit the empty minute component`() {
        format(7_200_000L, StatisticsDurationPrecision.MINUTES) shouldBe "2h"
    }
}
