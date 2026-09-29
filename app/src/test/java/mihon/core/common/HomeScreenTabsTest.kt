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
}
