package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDownloadRuntimeFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `runtime projects queue and child status`() = runTest {
        val queueItem = queueItem()
        val processor = processor(queueItem)
        val status = EntryDownloadStatus(EntryType.BOOK, queueItem.childId, EntryDownloadState.QUEUE)
        every { processor.getStatus(any(), any(), any(), any(), any(), any()) } returns status
        val feature = featureFor(backgroundScope, EntryDownloadCapability.bind(processor))

        feature.state.first().queue.single().items.shouldContainExactly(queueItem)
        feature.status(
            type = entry.type,
            childId = queueItem.childId,
            childName = "Child",
            childScanlator = null,
            childUrl = "/child",
            entryTitle = entry.title,
            sourceId = entry.source,
        ) shouldBe status
    }

    @Test
    fun `missing download provider is valid and exposes no runtime behavior`() = runTest {
        val feature = featureFor(backgroundScope)

        feature.isApplicable(EntryType.BOOK).shouldBeFalse()
        feature.state.first() shouldBe EntryDownloadRuntimeState()
        feature.downloadCount(entry) shouldBe 0
        feature.status(
            type = entry.type,
            childId = 11L,
            childName = "Child",
            childScanlator = null,
            childUrl = "/child",
            entryTitle = entry.title,
            sourceId = entry.source,
        ).shouldBeNull()
    }

    @Test
    fun `resubscribing after observation stops cannot replay an obsolete queue`() = runTest {
        val item = queueItem()
        val processor = processor(item)
        val queue = MutableStateFlow(
            listOf(EntryDownloadQueueGroup(item.sourceId, "Source", item.entryType, listOf(item))),
        )
        every { processor.queueState } returns queue
        val feature = featureFor(backgroundScope, EntryDownloadCapability.bind(processor))
        feature.state.first().queue.single().items.single() shouldBe item

        advanceTimeBy(5_001)
        runCurrent()
        queue.value = emptyList()

        feature.state.first().queue shouldBe emptyList()
    }

    private fun featureFor(
        scope: CoroutineScope,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryDownloadRuntimeFeature {
        val plugins = bindings
            .takeIf { it.isNotEmpty() }
            ?.let { listOf(plugin(EntryType.BOOK, *it)) }
            .orEmpty()
        val composition = createEntryInteractionComposition(
            plugins = plugins,
            featureContributors = listOf(EntryDownloadRuntimeFeatureContributor),
        )
        return DefaultEntryDownloadRuntimeFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.download,
            scope = scope,
        )
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }
    }

    private fun processor(item: EntryDownloadQueueItem): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
            every { changes } returns emptyFlow()
            every { isInitializing } returns flowOf(false)
            every { isRunning } returns flowOf(true)
            every { queueState } returns flowOf(
                listOf(
                    EntryDownloadQueueGroup(
                        sourceId = item.sourceId,
                        sourceName = "Source",
                        entryType = item.entryType,
                        items = listOf(item),
                    ),
                ),
            )
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
