package tachiyomi.domain.statistics.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsCardLayoutTest {
    @Test
    fun `new cards append without resetting saved order or hidden choices`() {
        val layout = StatisticsCardLayout.decode("calendar,top_titles,calendar,retired;top_titles,retired")
        layout.order.take(2) shouldBe listOf(StatisticsCard.CALENDAR, StatisticsCard.TOP_TITLES)
        layout.order.toSet() shouldBe StatisticsCard.entries.toSet()
        layout.hidden shouldBe setOf(StatisticsCard.TOP_TITLES)
        StatisticsCardLayout.decode(layout.encode()) shouldBe layout
    }

    @Test
    fun `a merged card takes its first part's place and hides only when every part was hidden`() {
        val layout = StatisticsCardLayout.decode(
            "progress,summary,activity,calendar,top_titles,patterns,earlier,media,insights;summary,progress,media",
        )
        layout.order.take(3) shouldBe listOf(StatisticsCard.LIBRARY, StatisticsCard.ACTIVITY, StatisticsCard.CALENDAR)
        layout.hidden shouldBe setOf(StatisticsCard.LIBRARY)
    }
}
