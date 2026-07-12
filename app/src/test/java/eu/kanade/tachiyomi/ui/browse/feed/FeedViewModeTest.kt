package eu.kanade.tachiyomi.ui.browse.feed

import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryImmersiveFeedHandle
import org.junit.jupiter.api.Test

class FeedViewModeTest {
    @Test
    fun `immersive mode is exposed only when source opts in`() {
        availableFeedViewModes(supportsImmersiveFeed = true) shouldContainExactly
            listOf(FeedViewMode.Regular, FeedViewMode.Immersive)
        availableFeedViewModes(supportsImmersiveFeed = false) shouldContainExactly
            listOf(FeedViewMode.Regular)
    }

    @Test
    fun `immersive pager key is bundle saveable and includes entry identity`() {
        val mangaKey = immersiveFeedItemKey(FeedItemRef(id = 3444L, type = EntryType.MANGA))
        val animeKey = immersiveFeedItemKey(FeedItemRef(id = 3444L, type = EntryType.ANIME))

        mangaKey shouldBe "MANGA:3444"
        animeKey shouldBe "ANIME:3444"
    }

    @Test
    fun `entry poster remains available behind media transitions`() {
        val imagePages = EntryImmersiveFeedHandle.ImagePages(
            entryType = EntryType.MANGA,
            chapterId = 1L,
            delegate = Unit,
        )

        shouldShowImmersiveFeedPoster(imagePages) shouldBe true
        shouldShowImmersiveFeedPoster(
            EntryImmersiveFeedHandle.Playback(
                entryType = EntryType.ANIME,
                chapterId = 1L,
                stream = VideoStream(VideoRequest("https://example.invalid/video")),
                subtitles = emptyList(),
                resumePositionMs = 0L,
            ),
        ) shouldBe true
        shouldShowImmersiveFeedPoster(null) shouldBe true
    }
}
