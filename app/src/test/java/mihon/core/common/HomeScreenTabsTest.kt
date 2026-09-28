package mihon.core.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HomeScreenTabsTest {

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
            HomeScreenTabs.Translator,
            HomeScreenTabs.Statistics,
        )
    }

    @Test
    fun `startup fallback prefers library, then the first enabled tab in saved order`() {
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.Library, HomeScreenTabs.More),
        ) shouldBe HomeScreenTabs.Library
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.More, HomeScreenTabs.Profiles),
        ) shouldBe HomeScreenTabs.More
        resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Updates,
            enabledTabs = listOf(HomeScreenTabs.Browse, HomeScreenTabs.More),
            tabOrder = listOf(HomeScreenTabs.More, HomeScreenTabs.Browse, HomeScreenTabs.Updates),
        ) shouldBe HomeScreenTabs.More
    }
}
