package eu.kanade.tachiyomi.ui.browse.feed

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class FeedViewModeTest {
    @Test
    fun `immersive mode is exposed only when feature makes it available`() {
        availableFeedViewModes(immersiveAvailable = true) shouldContainExactly
            listOf(FeedViewMode.Regular, FeedViewMode.Immersive)
        availableFeedViewModes(immersiveAvailable = false) shouldContainExactly
            listOf(FeedViewMode.Regular)
    }
}
