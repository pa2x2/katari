package tachiyomi.domain.statistics.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatisticsCardLayoutTest {
    @Test
    fun `new cards append without resetting saved order or hidden choices`() {
        val layout = StatisticsCardLayout.decode("progress,summary,progress,retired;summary,retired")
        layout.order.take(2) shouldBe listOf(StatisticsCard.PROGRESS, StatisticsCard.SUMMARY)
        layout.order.toSet() shouldBe StatisticsCard.entries.toSet()
        layout.hidden shouldBe setOf(StatisticsCard.SUMMARY)
        StatisticsCardLayout.decode(layout.encode()) shouldBe layout
    }
}
