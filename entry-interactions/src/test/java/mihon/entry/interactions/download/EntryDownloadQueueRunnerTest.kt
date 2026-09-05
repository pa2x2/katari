package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDownloadQueueRunnerTest {
    @Test
    fun `adding an idle entry type starts it without restarting the active transfer`() = runTest {
        for ((activeType, addedType) in listOf(
            EntryType.ANIME to EntryType.MANGA,
            EntryType.MANGA to EntryType.BOOK,
            EntryType.BOOK to EntryType.ANIME,
        )) {
            val finishActive = CompletableDeferred<Unit>()
            val active = EntryDownloadQueueRunnerFixture(activeType) { finishActive.await() }
            val added = EntryDownloadQueueRunnerFixture(addedType)
            val interaction = downloadInteraction(active, added)
            active.enqueue(1)
            val worker = launch { interaction.runDownloadsUntilIdle() }
            runCurrent()

            interaction.queue(added.entry, listOf(added.chapter(2)), autoStart = true)
            runCurrent()

            added.completed shouldBe listOf(2L)
            active.attempted shouldBe listOf(1L)
            active.running.value shouldBe true
            finishActive.complete(Unit)
            worker.join()
            active.completed shouldBe listOf(1L)
            added.queue.value shouldBe emptyList()
        }
    }

    @Test
    fun `a drained type can receive a second batch while another type stays active`() = runTest {
        val finishAnime = CompletableDeferred<Unit>()
        val anime = EntryDownloadQueueRunnerFixture(EntryType.ANIME) { finishAnime.await() }
        val manga = EntryDownloadQueueRunnerFixture(EntryType.MANGA)
        val interaction = downloadInteraction(anime, manga)
        anime.enqueue(1)
        manga.enqueue(2)
        val worker = launch { interaction.runDownloadsUntilIdle() }
        runCurrent()
        manga.completed shouldBe listOf(2L)

        interaction.queue(manga.entry, listOf(manga.chapter(3)), autoStart = true)
        runCurrent()

        manga.completed shouldBe listOf(2L, 3L)
        anime.attempted shouldBe listOf(1L)
        finishAnime.complete(Unit)
        worker.join()
    }

    @Test
    fun `worker completion includes another type queued during the final transfer`() = runTest {
        val finishAnime = CompletableDeferred<Unit>()
        val manga = EntryDownloadQueueRunnerFixture(EntryType.MANGA)
        val anime = EntryDownloadQueueRunnerFixture(EntryType.ANIME) {
            finishAnime.await()
            manga.enqueue(2)
        }
        anime.enqueue(1)

        val worker = launch { downloadInteraction(anime, manga).runDownloadsUntilIdle() }
        runCurrent()
        finishAnime.complete(Unit)
        worker.join()

        anime.completed shouldBe listOf(1L)
        manga.completed shouldBe listOf(2L)
    }

    @Test
    fun `failed work stays failed across queue wakeups and successor workers`() = runTest {
        val finishAnime = CompletableDeferred<Unit>()
        val anime = EntryDownloadQueueRunnerFixture(EntryType.ANIME) { finishAnime.await() }
        val manga = EntryDownloadQueueRunnerFixture(EntryType.MANGA) { error("Transfer failed") }
        val book = EntryDownloadQueueRunnerFixture(EntryType.BOOK)
        val interaction = downloadInteraction(anime, manga, book)
        anime.enqueue(1)
        manga.enqueue(2)
        val worker = launch { interaction.runDownloadsUntilIdle() }
        runCurrent()

        interaction.queue(book.entry, listOf(book.chapter(3)), autoStart = true)
        runCurrent()
        finishAnime.complete(Unit)
        worker.join()
        interaction.runDownloadsUntilIdle()

        manga.attempted shouldBe listOf(2L)
        manga.queue.value.single().items.single().state shouldBe EntryDownloadState.ERROR
        book.completed shouldBe listOf(3L)
    }

    @Test
    fun `cancelling the shared worker preserves each active type for resume`() = runTest {
        val finish = CompletableDeferred<Unit>()
        val anime = EntryDownloadQueueRunnerFixture(EntryType.ANIME) { finish.await() }
        val manga = EntryDownloadQueueRunnerFixture(EntryType.MANGA) { finish.await() }
        val interaction = downloadInteraction(anime, manga)
        anime.enqueue(1)
        manga.enqueue(2)
        val worker = launch { interaction.runDownloadsUntilIdle() }
        runCurrent()

        worker.cancelAndJoin()

        for (fixture in listOf(anime, manga)) {
            fixture.running.value shouldBe false
            fixture.queue.value.single().items.single().state shouldBe EntryDownloadState.QUEUE
        }
        finish.complete(Unit)
        interaction.runDownloadsUntilIdle()
        anime.completed shouldBe listOf(1L)
        manga.completed shouldBe listOf(2L)
    }
}
