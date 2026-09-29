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
