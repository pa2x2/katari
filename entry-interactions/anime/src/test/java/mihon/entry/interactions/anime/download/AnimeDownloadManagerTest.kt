package mihon.entry.interactions.anime.download

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.anime.download.model.AnimeDownload
import org.junit.jupiter.api.Test

class AnimeDownloadManagerTest {

    @Test
    fun `removing a queued episode keeps the active download running`() {
        isActiveEpisodeBeingRemoved(
            activeEpisodeId = 1L,
            episodeIds = listOf(2L),
        ) shouldBe false
    }

    @Test
    fun `removing the active episode stops its download`() {
        isActiveEpisodeBeingRemoved(
            activeEpisodeId = 1L,
            episodeIds = listOf(1L, 2L),
        ) shouldBe true
    }

    @Test
    fun `queue mutations made during restore win without duplicating episodes`() {
        val restoredFirst = download(episodeId = 1L)
        val restoredReplaced = download(episodeId = 2L)
        val newlyQueuedReplacement = download(episodeId = 2L)
        val newlyQueuedThird = download(episodeId = 3L)

        val merged = mergeRestoredDownloads(
            restored = listOf(restoredFirst, restoredReplaced),
            current = listOf(newlyQueuedReplacement, newlyQueuedThird),
        )

        merged shouldContainExactly listOf(restoredFirst, newlyQueuedReplacement, newlyQueuedThird)
    }

    private fun download(episodeId: Long): AnimeDownload = mockk {
        every { episode.id } returns episodeId
    }
}
