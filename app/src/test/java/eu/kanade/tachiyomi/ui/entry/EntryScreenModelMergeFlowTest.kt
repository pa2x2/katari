package eu.kanade.tachiyomi.ui.entry

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryScreenModelMergeFlowTest {

    @Test
    fun `owner refresh is not collapsed when entry and children remain equal`() = runTest {
        val entry = Entry.create().copy(id = 2L, source = 1L)
        val chapters = listOf(chapter(id = 101L, entryId = entry.id))
        val entryAndChaptersFlow = MutableSharedFlow<Pair<Entry, List<EntryChapter>>>(replay = 1)
        entryAndChaptersFlow.emit(entry to chapters)
        val downloadChangesFlow = MutableStateFlow(Unit)
        val downloadQueueFlow = MutableStateFlow(Unit)
        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            mergeAwareEntryAndChaptersFlow(
                entryAndChaptersFlow = entryAndChaptersFlow,
                downloadChangesFlow = downloadChangesFlow,
                downloadQueueFlow = downloadQueueFlow,
            )
                .take(2)
                .toList(emissions)
        }
        advanceUntilIdle()

        entryAndChaptersFlow.emit(entry to chapters)
        advanceUntilIdle()

        emissions shouldBe listOf(entry to chapters, entry to chapters)
        job.join()
    }

    private fun chapter(id: Long, entryId: Long): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            sourceOrder = 1L,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
