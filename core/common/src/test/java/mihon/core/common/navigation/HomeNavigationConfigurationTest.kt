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
        )
    }

    @Test
    fun `missing primary preference keeps five enabled tabs directly in the bar`() {
        val configuration = resolveHomeNavigationConfiguration(
            enabledTabs = setOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.Updates,
                HomeScreenTabs.History,
                HomeScreenTabs.Browse,
                HomeScreenTabs.More,
            ),
            tabOrder = HomeScreenTabs.entries,
            primaryTabs = emptyList(),
        )

        configuration.primaryTabs shouldBe listOf(
            HomeScreenTabs.Library,
            HomeScreenTabs.Updates,
            HomeScreenTabs.History,
            HomeScreenTabs.Browse,
            HomeScreenTabs.More,
        )
        configuration.overflowTabs shouldBe emptyList()
    }

    @Test
    fun `stored primary placement is sanitized against enabled tabs`() {
        val configuration = resolveHomeNavigationConfiguration(
            enabledTabs = setOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.History,
                HomeScreenTabs.More,
            ),
            tabOrder = listOf(
                HomeScreenTabs.History,
                HomeScreenTabs.Library,
                HomeScreenTabs.More,
            ),
            primaryTabs = listOf(
                HomeScreenTabs.Browse,
                HomeScreenTabs.Library,
                HomeScreenTabs.Library,
                HomeScreenTabs.More,
            ),
        )

        configuration.primaryTabs shouldBe listOf(
            HomeScreenTabs.Library,
            HomeScreenTabs.More,
        )
        configuration.overflowTabs shouldBe listOf(HomeScreenTabs.History)
        configuration.hiddenTabs shouldBe listOf(
            HomeScreenTabs.Updates,
            HomeScreenTabs.Browse,
            HomeScreenTabs.Profiles,
        )
    }

    @Test
    fun `temporarily hidden profiles action does not mutate saved placement`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(HomeScreenTabs.Profiles),
            overflowTabs = listOf(HomeScreenTabs.More, HomeScreenTabs.Library),
            hiddenTabs = listOf(HomeScreenTabs.Updates, HomeScreenTabs.History, HomeScreenTabs.Browse),
        )

        configuration.withVisibleProfiles(showProfilesTab = false) shouldBe configuration.copy(
            primaryTabs = listOf(HomeScreenTabs.More),
            overflowTabs = listOf(HomeScreenTabs.Library),
        )
        configuration.withVisibleProfiles(showProfilesTab = true) shouldBe configuration
    }

    @Test
    fun `primary preference round trips without inventing placements`() {
        val primaryTabs = listOf(
            HomeScreenTabs.Browse,
            HomeScreenTabs.Library,
            HomeScreenTabs.Browse,
            HomeScreenTabs.Updates,
            HomeScreenTabs.History,
            HomeScreenTabs.More,
        )

        primaryTabs.toHomeNavigationPrimaryTabsPreferenceValue().toHomeNavigationPrimaryTabs() shouldBe listOf(
            HomeScreenTabs.Browse,
            HomeScreenTabs.Library,
            HomeScreenTabs.Updates,
            HomeScreenTabs.History,
            HomeScreenTabs.More,
        )
        "".toHomeNavigationPrimaryTabs() shouldBe emptyList()
    }

    @Test
    fun `placing a fifth primary tab while overflow exists displaces the last primary tab`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.Updates,
                HomeScreenTabs.History,
                HomeScreenTabs.Browse,
            ),
            overflowTabs = listOf(HomeScreenTabs.More),
            hiddenTabs = listOf(HomeScreenTabs.Profiles),
        )

        configuration.move(
            tab = HomeScreenTabs.Profiles,
            targetSection = HomeNavigationSection.Primary,
            targetIndex = 1,
        ) shouldBe HomeNavigationMoveResult.Moved(
            HomeNavigationConfiguration(
                primaryTabs = listOf(
                    HomeScreenTabs.Library,
                    HomeScreenTabs.Profiles,
                    HomeScreenTabs.Updates,
                    HomeScreenTabs.History,
                ),
                overflowTabs = listOf(HomeScreenTabs.Browse, HomeScreenTabs.More),
                hiddenTabs = emptyList(),
            ),
        )
    }

    @Test
    fun `moving the only overflow tab into primary produces five direct items`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.Updates,
                HomeScreenTabs.History,
                HomeScreenTabs.Browse,
            ),
            overflowTabs = listOf(HomeScreenTabs.More),
            hiddenTabs = listOf(HomeScreenTabs.Profiles),
        )

        configuration.move(
            tab = HomeScreenTabs.More,
            targetSection = HomeNavigationSection.Primary,
            targetIndex = 4,
        ) shouldBe HomeNavigationMoveResult.Moved(
            configuration.copy(
                primaryTabs = configuration.primaryTabs + HomeScreenTabs.More,
                overflowTabs = emptyList(),
            ),
        )
    }

    @Test
    fun `adding a sixth direct item creates room for Menu`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(
                HomeScreenTabs.Library,
                HomeScreenTabs.Updates,
                HomeScreenTabs.History,
                HomeScreenTabs.Browse,
                HomeScreenTabs.More,
            ),
            overflowTabs = emptyList(),
            hiddenTabs = listOf(HomeScreenTabs.Profiles),
        )

        configuration.move(
            tab = HomeScreenTabs.Profiles,
            targetSection = HomeNavigationSection.Primary,
            targetIndex = 1,
        ) shouldBe HomeNavigationMoveResult.Moved(
            HomeNavigationConfiguration(
                primaryTabs = listOf(
                    HomeScreenTabs.Library,
                    HomeScreenTabs.Profiles,
                    HomeScreenTabs.Updates,
                    HomeScreenTabs.History,
                ),
                overflowTabs = listOf(HomeScreenTabs.Browse, HomeScreenTabs.More),
                hiddenTabs = emptyList(),
            ),
        )
    }

    @Test
    fun `mandatory navigation destinations cannot be removed`() {
        val configuration = HomeNavigationConfiguration(
            primaryTabs = listOf(HomeScreenTabs.Library),
            overflowTabs = listOf(HomeScreenTabs.More),
            hiddenTabs = HomeScreenTabs.entries.filterNot { it == HomeScreenTabs.Library || it == HomeScreenTabs.More },
        )

        configuration.move(
            tab = HomeScreenTabs.Library,
            targetSection = HomeNavigationSection.Hidden,
            targetIndex = 0,
        ) shouldBe HomeNavigationMoveResult.PrimaryRequired
        configuration.move(
            tab = HomeScreenTabs.More,
            targetSection = HomeNavigationSection.Hidden,
            targetIndex = 0,
        ) shouldBe HomeNavigationMoveResult.MoreRequired
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
