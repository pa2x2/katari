package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class EntryDownloadQueueRunnerFixture(
    val type: EntryType,
    private val transfer: suspend (Long) -> Unit = {},
) {
    val queue = MutableStateFlow<List<EntryDownloadQueueGroup>>(emptyList())
    val running = MutableStateFlow(false)
    val completed = mutableListOf<Long>()
    val attempted = mutableListOf<Long>()
    val entry = Entry.create().copy(id = 1, source = 2, profileId = 1, type = type)

    val processor = mockk<EntryDownloadProcessor>(relaxed = true) {
        every { type } returns this@EntryDownloadQueueRunnerFixture.type
        every { changes } returns emptyFlow()
        every { isInitializing } returns flowOf(false)
        every { isRunning } returns running
        every { queueState } returns queue
        every { events } returns emptyFlow()
        every { updates() } returns emptyFlow()
        coEvery { hasPendingDownloads() } answers {
            queue.value.any { group -> group.items.any { it.state == EntryDownloadState.QUEUE } }
        }
        coEvery { queue(any(), any(), any()) } coAnswers {
            secondArg<List<EntryChapter>>().forEach { enqueue(it.id) }
        }
        coEvery { runDownloadsUntilIdle() } coAnswers {
            running.value = true
            try {
                while (true) {
                    val next = queue.value.flatMap { it.items }.firstOrNull { it.state == EntryDownloadState.QUEUE }
                        ?: break
                    attempted += next.childId
                    setState(next.childId, EntryDownloadState.DOWNLOADING)
                    try {
                        transfer(next.childId)
                        completed += next.childId
                        publish(queue.value.flatMap { it.items }.filterNot { it.identity == next.identity })
                    } catch (error: CancellationException) {
                        setState(next.childId, EntryDownloadState.QUEUE)
                        throw error
                    } catch (_: Exception) {
                        setState(next.childId, EntryDownloadState.ERROR)
                    }
                }
            } finally {
                running.value = false
            }
        }
    }

    fun enqueue(childId: Long) {
        publish(
            queue.value.flatMap { it.items } + EntryDownloadQueueItem(
                identity = EntryDownloadIdentity.from(entry, chapter(childId)),
                state = EntryDownloadState.QUEUE,
                title = type.name,
                subtitle = "Child $childId",
                dateUpload = 0,
                chapterNumber = childId.toDouble(),
                progress = 0,
                progressMax = 100,
            ),
        )
    }

    fun chapter(id: Long) = EntryChapter.create().copy(id = id, entryId = entry.id)

    private fun setState(childId: Long, state: EntryDownloadState) {
        publish(
            queue.value.flatMap { it.items }.map {
                if (it.childId ==
                    childId
                ) {
                    it.copy(state = state, presentation = EntryDownloadPresentation.forState(state))
                } else {
                    it
                }
            },
        )
    }

    private fun publish(items: List<EntryDownloadQueueItem>) {
        queue.value = if (items.isEmpty()) emptyList() else listOf(EntryDownloadQueueGroup(2, "Source", type, items))
    }
}

internal fun downloadInteraction(vararg fixtures: EntryDownloadQueueRunnerFixture): EntryDownloadInteraction =
    createEntryInteractionComposition(
        plugins = fixtures.map { fixture ->
            object : EntryInteractionPlugin {
                override val type = fixture.type
                override val owner = ContributionOwner("test.download.${type.name.lowercase()}")
                override val providerBindings = listOf(EntryDownloadCapability.bind(fixture.processor))
            }
        },
        featureContributors = listOf(EntryDownloadRuntimeFeatureContributor),
    ).interactions.download
