package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.DefaultEntryInteractions
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDownloadRuntimeFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `resubscribing after observation stops cannot replay an obsolete queue`() = runTest {
        val item = queueItem()
        val queue = MutableStateFlow(
            listOf(EntryDownloadQueueGroup(item.sourceId, "Source", item.entryType, listOf(item))),
        )
        val processor = processor(queue)
        val feature = featureFor(backgroundScope, processor)
        feature.state.first().queue.single().items.single() shouldBe item

        advanceTimeBy(5_001)
        runCurrent()
        queue.value = emptyList()

        feature.state.first().queue shouldBe emptyList()
    }

    private fun featureFor(scope: CoroutineScope, processor: EntryDownloadProcessor): EntryDownloadRuntimeFeature {
        return DefaultEntryDownloadRuntimeFeature(
            applicableTypes = setOf(EntryType.BOOK),
            interaction = DefaultEntryInteractions(downloadProcessors = mapOf(EntryType.BOOK to processor)).download,
            scope = scope,
        )
    }

    private fun processor(queue: Flow<List<EntryDownloadQueueGroup>>): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { changes } returns emptyFlow()
            every { isInitializing } returns flowOf(false)
            every { isRunning } returns flowOf(true)
            every { queueState } returns queue
            every { events } returns emptyFlow()
            every { updates() } returns emptyFlow()
        }
    }

    private fun queueItem() = EntryDownloadQueueItem(
        identity = EntryDownloadIdentity(
            profileId = 1L,
            entryType = EntryType.BOOK,
            entryId = entry.id,
            sourceId = entry.source,
            childId = 11L,
        ),
        state = EntryDownloadState.QUEUE,
        title = entry.title,
        subtitle = "Child",
        dateUpload = 0L,
        chapterNumber = 1.0,
        progress = 0,
        progressMax = 1,
    )
}
