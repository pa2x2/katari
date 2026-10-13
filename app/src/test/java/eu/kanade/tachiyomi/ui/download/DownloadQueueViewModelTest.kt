package eu.kanade.tachiyomi.ui.download

import androidx.lifecycle.ViewModelStore
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.base.viewmodel.createViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.entry.interactions.download.EntryDownloadIdentity
import mihon.entry.interactions.download.EntryDownloadPhase
import mihon.entry.interactions.download.EntryDownloadPresentation
import mihon.entry.interactions.download.EntryDownloadProgress
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeState
import mihon.entry.interactions.download.EntryDownloadState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadQueueViewModelTest {
    @Test
    fun `progress and phase updates replace queued snapshots even before a row is bound`() = runTest {
        val viewModels = ViewModelStore()
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val queued = EntryDownloadQueueItem(
                identity = EntryDownloadIdentity(1, EntryType.BOOK, 2, 3, 4),
                state = EntryDownloadState.QUEUE,
                title = "Book",
                subtitle = "Read book",
                dateUpload = 0,
                chapterNumber = 1.0,
                progress = 0,
                progressMax = 100,
            )
            val runtimeState = MutableStateFlow(
                EntryDownloadRuntimeState(
                    queue = listOf(EntryDownloadQueueGroup(3, "Source", EntryType.BOOK, listOf(queued))),
                ),
            )
            val runtime = mockk<EntryDownloadRuntimeFeature> {
                every { state } returns runtimeState
            }
            val model = viewModels.createViewModel { DownloadQueueViewModel(runtime) }
            // The queue is only observed while the screen collects it
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
            val row = model.state.value.single().subItems.single()
            val transferring = queued.copy(
                state = EntryDownloadState.DOWNLOADING,
                progress = 42,
                presentation = EntryDownloadPresentation(
                    EntryDownloadPhase.TRANSFERRING,
                    EntryDownloadProgress.Percent(42),
                ),
            )

            runtimeState.value = runtimeState.value.copy(
                queue = listOf(EntryDownloadQueueGroup(3, "Source", EntryType.BOOK, listOf(transferring))),
            )

            assertEquals(42, row.model().progress)
            assertEquals(transferring.presentation, row.model().presentation)
            assertEquals(transferring, row.payloadAsDownloadQueueItem())

            runtimeState.value = runtimeState.value.copy(isRunning = true)
            assertEquals(42, model.state.value.single().subItems.single().model().progress)

            val finalizing = transferring.copy(presentation = EntryDownloadPresentation(EntryDownloadPhase.FINALIZING))
            runtimeState.value = runtimeState.value.copy(
                queue = listOf(EntryDownloadQueueGroup(3, "Source", EntryType.BOOK, listOf(finalizing))),
            )

            assertEquals(EntryDownloadPhase.FINALIZING, row.model().presentation.phase)
        } finally {
            viewModels.clear()
            Dispatchers.resetMain()
        }
    }
}
