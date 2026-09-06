package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.download.EntryDownloadWorkController
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode

class AnimeDownloadManagerTest {

    @Test
    fun `pending work check waits for persisted episodes to be restored`() = runTest {
        val restored = CompletableDeferred<List<AnimeDownload>>()
        val store = mockk<AnimeDownloadStore>(relaxed = true) {
            coEvery { restore() } coAnswers { restored.await() }
        }
        val manager = manager(mockk(relaxed = true), store)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { manager.hasPendingDownloads() }

        restored.complete(
            listOf(
                AnimeDownload(
                    anime = Entry.create().copy(id = 1L, type = EntryType.ANIME),
                    episode = EntryChapter.create().copy(id = 2L, entryId = 1L),
                    preferences = preferences(),
                ),
            ),
        )

        pending.await() shouldBe true
        manager.queueState.value.single().status shouldBe AnimeDownload.State.QUEUE
    }

    @Test
    fun `runtime wakeups leave failed episodes alone until an explicit retry`() = runTest {
        var attempts = 0
        val downloader = mockk<AnimeDownloader> {
            coEvery { download(any()) } coAnswers {
                attempts++
                AnimeDownloadFailure(AnimeDownloadFailure.Reason.NETWORK)
            }
        }
        val manager = manager(downloader)
        manager.queueEpisodes(
            anime = Entry.create().copy(id = 1L, type = EntryType.ANIME),
            episodes = listOf(EntryChapter.create().copy(id = 2L, entryId = 1L)),
            preferences = preferences(),
            autoStart = false,
        )

        manager.runDownloadsUntilIdle()
        manager.runDownloadsUntilIdle()

        attempts shouldBe 1
        manager.queueState.value.single().status shouldBe AnimeDownload.State.ERROR
        manager.hasPendingDownloads() shouldBe false
        manager.startDownloads()
        manager.runDownloadsUntilIdle()
        attempts shouldBe 2
    }

    @Test
    fun `runtime cancellation restores an active anime download to the queue`() = runTest {
        val downloadStarted = CompletableDeferred<Unit>()
        val downloader = mockk<AnimeDownloader> {
            coEvery { download(any()) } coAnswers {
                downloadStarted.complete(Unit)
                awaitCancellation()
            }
        }
        val manager = manager(downloader)
        manager.queueEpisodes(
            anime = Entry.create().copy(id = 1L),
            episodes = listOf(EntryChapter.create().copy(id = 2L, entryId = 1L)),
            preferences = preferences(),
            autoStart = false,
        )
        val runtime = launch { manager.runDownloadsUntilIdle() }
        downloadStarted.await()

        runtime.cancelAndJoin()

        manager.queueState.value.single().status shouldBe AnimeDownload.State.QUEUE
        manager.isRunning.value shouldBe false
    }

    @Test
    fun `cancelling an active anime download continues pending work`() = runTest {
        val downloadStarted = CompletableDeferred<Unit>()
        val downloader = mockk<AnimeDownloader> {
            coEvery { download(match { it.episode.id == 1L }) } coAnswers {
                downloadStarted.complete(Unit)
                awaitCancellation()
            }
            coEvery { download(match { it.episode.id == 2L }) } returns null
        }
        val manager = manager(downloader)
        manager.queueEpisodes(
            anime = Entry.create().copy(id = 1L),
            episodes = listOf(
                EntryChapter.create().copy(id = 1L, entryId = 1L),
                EntryChapter.create().copy(id = 2L, entryId = 1L),
            ),
            preferences = preferences(),
            autoStart = false,
        )
        val runtime = launch { manager.runDownloadsUntilIdle() }
        downloadStarted.await()

        manager.removeFromQueue(listOf(1L))
        runtime.join()

        manager.queueState.value shouldBe emptyList()
    }

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

    private fun manager(
        downloader: AnimeDownloader,
        store: AnimeDownloadStore = mockk(relaxed = true) {
            coEvery { restore() } returns emptyList()
        },
    ): AnimeDownloadManager {
        return AnimeDownloadManager(
            context = mockk(relaxed = true),
            cache = mockk { every { changes } returns MutableSharedFlow<Unit>() },
            provider = mockk(relaxed = true),
            downloader = downloader,
            sourceManager = mockk(relaxed = true),
            store = store,
            workController = mockk<EntryDownloadWorkController>(relaxed = true),
        )
    }

    private fun preferences() = DownloadPreferences(
        entryId = 1L,
        dubKey = null,
        streamKey = null,
        subtitleKey = null,
        qualityMode = VideoDownloadQualityMode.BALANCED,
        updatedAt = 0L,
    )
}
