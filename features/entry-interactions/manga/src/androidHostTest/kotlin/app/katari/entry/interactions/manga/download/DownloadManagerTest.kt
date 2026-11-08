package app.katari.entry.interactions.manga.download

import app.katari.entry.interactions.download.EntryDownloadWorkController
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class DownloadManagerTest {

    @Test
    fun `runtime cancellation pauses manga work`() = runTest {
        val started = CompletableDeferred<Unit>()
        val downloader = mockk<Downloader>(relaxed = true) {
            coEvery { awaitInitialized() } returns Unit
            every { queueState } returns MutableStateFlow(emptyList())
            every { start() } returns true
            coEvery { awaitIdle() } coAnswers {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        val manager = manager(downloader)
        val runtime = launch { manager.runDownloadsUntilIdle() }
        started.await()

        runtime.cancelAndJoin()

        verify(exactly = 1) { downloader.pause() }
    }

    private fun TestScope.manager(downloader: Downloader): DownloadManager {
        return DownloadManager(
            context = mockk(relaxed = true),
            provider = mockk(),
            cache = mockk(),
            sourceManager = mockk<SourceManager>(),
            downloader = downloader,
            pendingDeleter = mockk<DownloadPendingDeleter>(),
            workController = mockk<EntryDownloadWorkController>(relaxed = true),
            scope = backgroundScope,
        )
    }
}
