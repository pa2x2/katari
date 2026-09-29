package mihon.core.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HomeScreenTabsTest {

    @Test
    fun `sanitized home tab order drops duplicates and appends tabs missing from a saved order`() {
        val sanitized = sanitizeHomeScreenTabOrder(
            listOf(HomeScreenTabs.Browse, HomeScreenTabs.Library, HomeScreenTabs.Browse),
        )

        sanitized.take(2) shouldBe listOf(HomeScreenTabs.Browse, HomeScreenTabs.Library)
        sanitized.toSet() shouldBe HomeScreenTabs.entries.toSet()
        sanitized.size shouldBe HomeScreenTabs.entries.size
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
