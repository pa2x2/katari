package eu.kanade.tachiyomi.ui.browse.immersive

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryImmersiveHandle
import org.junit.jupiter.api.Test

class EntryImmersiveContentTest {

    @Test
    fun `immersive pager key is bundle saveable and includes entry identity`() {
        val mangaKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.MANGA))
        val animeKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.ANIME))

        mangaKey shouldBe "MANGA:3444"
        animeKey shouldBe "ANIME:3444"
    }

    @Test
    fun `entry poster remains available behind media transitions`() {
        val imagePages = EntryImmersiveHandle.ImagePages(
            entryType = EntryType.MANGA,
            chapterId = 1L,
            delegate = Unit,
        )

        shouldShowImmersivePoster(imagePages) shouldBe true
        shouldShowImmersivePoster(
            EntryImmersiveHandle.Playback(
                entryType = EntryType.ANIME,
                chapterId = 1L,
                stream = VideoStream(VideoRequest("https://example.invalid/video")),
                subtitles = emptyList(),
                resumePositionMs = 0L,
            ),
        ) shouldBe true
        shouldShowImmersivePoster(null) shouldBe true
    }

    @Test
    fun `pull refresh is only enabled at the settled first page while not zoomed`() {
        shouldEnableImmersivePullRefresh(settledPage = 0, isZoomed = false) shouldBe true
        shouldEnableImmersivePullRefresh(settledPage = 1, isZoomed = false) shouldBe false
        shouldEnableImmersivePullRefresh(settledPage = 0, isZoomed = true) shouldBe false
    }
}
