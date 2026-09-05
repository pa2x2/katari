package mihon.entry.interactions.download

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDownloadQueueObservationTest {
    @Test
    fun `queue snapshots advance progress and phase without membership changes`() = runTest {
        val fixture = EntryDownloadQueueObservationFixture()
        val snapshots = mutableListOf<List<EntryDownloadQueueGroup>>()
        backgroundScope.launch { fixture.snapshots.collect { snapshots += it } }
        runCurrent()
        val queued = snapshots.single().single().items.single()

        fixture.download.item = fixture.download.item.copy(
            state = EntryDownloadState.DOWNLOADING,
            progress = 42,
            presentation = EntryDownloadPresentation(
                EntryDownloadPhase.TRANSFERRING,
                EntryDownloadProgress.Percent(42),
            ),
        )
        fixture.progress.emit(fixture.download)
        runCurrent()

        snapshots.last().single().items.single().progress shouldBe 42
        queued.state shouldBe EntryDownloadState.QUEUE
        queued.progress shouldBe 0

        fixture.download.item = fixture.download.item.copy(
            presentation = EntryDownloadPresentation(EntryDownloadPhase.FINALIZING),
        )
        fixture.status.emit(fixture.download)
        runCurrent()

        snapshots.last().single().items.single().presentation.phase shouldBe EntryDownloadPhase.FINALIZING
    }

    @Test
    fun `a new collector gets current transfer progress without waiting for another event`() = runTest {
        val fixture = EntryDownloadQueueObservationFixture()
        fixture.snapshots.first().single().items.single().progress shouldBe 0

        fixture.download.item = fixture.download.item.copy(
            state = EntryDownloadState.DOWNLOADING,
            progress = 73,
            presentation = EntryDownloadPresentation(
                EntryDownloadPhase.TRANSFERRING,
                EntryDownloadProgress.Percent(73),
            ),
        )
        // These events have no subscribers. A restarted screen must still see the current value.
        fixture.progress.emit(fixture.download)

        fixture.snapshots.first().single().items.single().progress shouldBe 73
    }

    @Test
    fun `delayed transfer events cannot restore a removed download`() = runTest {
        val fixture = EntryDownloadQueueObservationFixture()
        val snapshots = mutableListOf<List<EntryDownloadQueueGroup>>()
        backgroundScope.launch { fixture.snapshots.collect { snapshots += it } }
        runCurrent()

        fixture.queue.value = emptyList()
        fixture.progress.emit(fixture.download)
        fixture.status.emit(fixture.download)
        runCurrent()

        snapshots.last().shouldBeEmpty()
        fixture.snapshots.first().shouldBeEmpty()
    }
}
