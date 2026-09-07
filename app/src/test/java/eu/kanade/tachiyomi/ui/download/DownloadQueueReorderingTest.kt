package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.entry.EntryType
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
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeState
import mihon.entry.interactions.download.EntryDownloadState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class, InternalVoyagerApi::class)
class DownloadQueueReorderingTest {
    @Test
    fun `sorting paused queue publishes new row order to screen`() = verifyReorder(listOf(3L, 2L, 1L)) { model ->
        model.reorderQueue({ it.payload.chapterNumber }, reverse = true)
    }

    @Test
    fun `moving paused download to top publishes new row order to screen`() = verifyReorder(
        listOf(3L, 1L, 2L),
    ) { model ->
        val menu = mockk<MenuItem> { every { itemId } returns R.id.move_to_top }
        model.listener.onMenuItemClick(3, menu)
    }

    private fun verifyReorder(expected: List<Long>, action: (DownloadQueueScreenModel) -> Unit) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val initial = (1L..3L).map { id ->
                EntryDownloadQueueItem(
                    identity = EntryDownloadIdentity(1, EntryType.BOOK, 2, 3, id),
                    state = EntryDownloadState.QUEUE,
                    title = "Book",
                    subtitle = "Chapter $id",
                    dateUpload = 0,
                    chapterNumber = id.toDouble(),
                    progress = 0,
                    progressMax = 100,
                )
            }
            fun state(items: List<EntryDownloadQueueItem>) = EntryDownloadRuntimeState(
                queue = listOf(EntryDownloadQueueGroup(3, "Source", EntryType.BOOK, items)),
                isPaused = true,
            )
            val runtimeState = MutableStateFlow(state(initial))
            val runtime = mockk<EntryDownloadRuntimeFeature> {
                every { this@mockk.state } returns runtimeState
                every { reorderQueue(any()) } answers {
                    runtimeState.value = state(firstArg())
                }
            }
            val key = "queue-reorder-regression"
            val model = ScreenModelStore.getOrPut(key, null) { DownloadQueueScreenModel(runtime) }
            val header = model.state.value.single()
            // FlexibleAdapter retains header references but separately stores its flattened rows.
            model.adapter = mockk {
                every { headerItems } returns listOf(header)
                every { getItem(3) } returns header.subItems[2]
            }
            var renderedOrder = emptyList<Long>()
            val rendering = backgroundScope.launch(dispatcher) {
                model.state.collect { headers ->
                    renderedOrder = headers.flatMap { it.subItems }.map { it.payload.childId }
                }
            }
            assertEquals(listOf(1L, 2L, 3L), renderedOrder)
            action(model)
            assertEquals(expected, runtimeState.value.queue.single().items.map { it.childId }, "backend order")
            assertEquals(expected, renderedOrder, "screen must receive changed order")
            rendering.cancel()
        } finally {
            ScreenModelStore.onDisposeNavigator("queue-reorder-regression")
            Dispatchers.resetMain()
        }
    }
}
