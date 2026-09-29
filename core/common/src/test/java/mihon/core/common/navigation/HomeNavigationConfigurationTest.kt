package mihon.core.common.navigation

import io.kotest.matchers.shouldBe
import mihon.core.common.HomeScreenTabs
import org.junit.jupiter.api.Test

class HomeNavigationConfigurationTest {

    @Test
    fun `missing primary preference migrates the first four enabled tabs`() {
        val configuration = resolveHomeNavigationConfiguration(
            enabledTabs = HomeScreenTabs.entries.toSet(),
            tabOrder = listOf(
                HomeScreenTabs.Profiles,
                HomeScreenTabs.More,
                HomeScreenTabs.Library,
                HomeScreenTabs.Browse,
                HomeScreenTabs.History,
                HomeScreenTabs.Updates,
            ),
            primaryTabs = emptyList(),
        )

        configuration.primaryTabs shouldBe listOf(
            HomeScreenTabs.Profiles,
            HomeScreenTabs.More,
            HomeScreenTabs.Library,
            HomeScreenTabs.Browse,
        )
        configuration.overflowTabs shouldBe listOf(
            HomeScreenTabs.History,
            HomeScreenTabs.Updates,
            HomeScreenTabs.Translator,
            HomeScreenTabs.Statistics,
        )
    }

    @Test
    fun `reordering within a section accounts for the removed source position`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.Updates,
                HomeScreenTabs.History,
            ),
            overflowTabs = listOf(HomeScreenTabs.Browse, HomeScreenTabs.More),
            hiddenTabs = listOf(HomeScreenTabs.Profiles),
        )

        configuration.move(
            tab = HomeScreenTabs.Library,
            targetSection = HomeNavigationSection.Primary,
            targetIndex = 2,
        ) shouldBe HomeNavigationMoveResult.Moved(
            configuration.copy(
                primaryTabs = listOf(
                    HomeScreenTabs.Updates,
                    HomeScreenTabs.Library,
                    HomeScreenTabs.History,
                ),
            ),
        )
    }
}
