package eu.kanade.tachiyomi.ui.browse.feed

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class FeedViewModeTest {
    @Test
    fun `immersive mode is exposed only when source opts in`() {
        availableFeedViewModes(supportsImmersiveFeed = true) shouldContainExactly
            listOf(FeedViewMode.Regular, FeedViewMode.Immersive)
        availableFeedViewModes(supportsImmersiveFeed = false) shouldContainExactly
            listOf(FeedViewMode.Regular)
    }
}
