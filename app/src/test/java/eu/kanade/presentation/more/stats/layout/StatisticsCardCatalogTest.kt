package eu.kanade.presentation.more.stats.layout

import eu.kanade.presentation.more.stats.data.StatsRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout

class StatisticsCardCatalogTest {

    @Test
    fun `sections keep saved order inside each section and render activity before library`() {
        val layout = StatisticsCardLayout.decode("progress,top_titles,media,summary;patterns")

        val sections = layout.visibleSections(isOverview = true, range = StatsRange.SEVEN_DAYS)

        sections.map { it.first } shouldBe listOf(StatisticsCardGroup.ACTIVITY, StatisticsCardGroup.LIBRARY)
        sections[0].second.take(2) shouldBe listOf(StatisticsCard.TOP_TITLES, StatisticsCard.SUMMARY)
        (StatisticsCard.PATTERNS in sections[0].second) shouldBe false
        sections[1].second shouldBe listOf(StatisticsCard.PROGRESS, StatisticsCard.MEDIA)
    }
}
