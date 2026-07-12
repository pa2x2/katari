package mihon.core.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HomeScreenTabsTest {

    @Test
    fun `default home tabs exclude profiles`() {
        defaultHomeScreenTabs() shouldBe setOf(
            HomeScreenTabs.Library.name,
            HomeScreenTabs.Updates.name,
            HomeScreenTabs.History.name,
            HomeScreenTabs.Browse.name,
            HomeScreenTabs.More.name,
        )
    }

    @Test
    fun `visible home tabs include profiles only when picker is shown`() {
        val tabs = setOf(HomeScreenTabs.Library, HomeScreenTabs.Profiles)
        val tabOrder = listOf(
            HomeScreenTabs.Profiles,
            HomeScreenTabs.More,
            HomeScreenTabs.Library,
        )

        resolveVisibleHomeScreenTabs(tabs, tabOrder = tabOrder, showProfilesTab = false) shouldBe listOf(
            HomeScreenTabs.More,
            HomeScreenTabs.Library,
        )

        resolveVisibleHomeScreenTabs(tabs, tabOrder = tabOrder, showProfilesTab = true) shouldBe listOf(
            HomeScreenTabs.Profiles,
            HomeScreenTabs.More,
            HomeScreenTabs.Library,
        )
    }

    @Test
    fun `sanitized home tabs preserve profiles in stored selection`() {
        sanitizeHomeScreenTabs(
            tabs = setOf(HomeScreenTabs.Library, HomeScreenTabs.Profiles),
            tabOrder = listOf(HomeScreenTabs.Profiles, HomeScreenTabs.Library, HomeScreenTabs.More),
        ) shouldBe listOf(
            HomeScreenTabs.Profiles,
            HomeScreenTabs.Library,
            HomeScreenTabs.More,
        )
    }

    @Test
    fun `sanitized home tab order appends missing tabs`() {
        sanitizeHomeScreenTabOrder(
            listOf(HomeScreenTabs.Browse, HomeScreenTabs.Library, HomeScreenTabs.Browse),
        ) shouldBe listOf(
            HomeScreenTabs.Browse,
            HomeScreenTabs.Library,
            HomeScreenTabs.Updates,
            HomeScreenTabs.History,
            HomeScreenTabs.More,
            HomeScreenTabs.Profiles,
        )
    }

    @Test
    fun `tab order preference round trips with defaults when blank`() {
        defaultHomeScreenTabOrder().toHomeScreenTabOrderPreferenceValue().toHomeScreenTabOrder() shouldBe
            defaultHomeScreenTabOrder()

        "".toHomeScreenTabOrder() shouldBe defaultHomeScreenTabOrder()
    }

    @Test
    fun `startup fallback prefers library when available`() {
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.Library, HomeScreenTabs.More),
        ) shouldBe HomeScreenTabs.Library
    }

    @Test
    fun `startup fallback uses next enabled tab when library is unavailable`() {
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.More, HomeScreenTabs.Profiles),
        ) shouldBe HomeScreenTabs.More
    }

    @Test
    fun `startup fallback skips profiles for content tabs when not included`() {
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Browse,
            enabledTabs = listOf(HomeScreenTabs.More),
        ) shouldBe HomeScreenTabs.More
    }

    @Test
    fun `startup fallback follows saved tab order`() {
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.Browse, HomeScreenTabs.More),
            tabOrder = listOf(HomeScreenTabs.More, HomeScreenTabs.Browse, HomeScreenTabs.Updates),
        ) shouldBe HomeScreenTabs.More
    }
}
