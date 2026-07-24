package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.tracking.EntryTrackingAccountHost
import mihon.entry.interactions.host.tracking.EntryTrackingAutomationHost
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import mihon.entry.interactions.host.tracking.EntryTrackingCollectionHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostRefreshResult
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.entry.interactions.host.tracking.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.host.tracking.EntryTrackingOperationHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

class EntryTrackingOperationsTest {
    private val entry = Entry.create().copy(id = 11L, type = EntryType.BOOK)
    private val track = EntryTrack(
        id = 1L,
        entryId = entry.id,
        trackerId = 7L,
        remoteId = 2L,
        libraryId = null,
        title = "Future Book",
        progress = 3.0,
        total = 12L,
        status = 4L,
        score = 7.5,
        remoteUrl = "https://example.com/book",
        startDate = 0L,
        finishDate = 0L,
        private = false,
    )
    private val service = EntryTrackingHostService(
        id = track.trackerId,
        name = "Future Books",
        logoResource = 19,
        supportedEntryTypes = setOf(EntryType.BOOK),
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = true,
            supportsAutomaticBinding = false,
        ),
    )

    @Test
    fun `operation uses the current session track behind the Feature boundary`() = runTest {
        val operations = mockk<EntryTrackingOperationHost>(relaxed = true)
        val feature = feature(operations)
        val mutation = EntryTrackingMutation.Progress(8)

        feature.mutate(entry, EntryTrackingServiceId(service.id), mutation) shouldBe
            EntryTrackingOperationResult.Completed

        coVerify(exactly = 1) { operations.mutate(service.id, track, mutation) }
    }

    @Test
    fun `unsupported tracker operation is rejected without invoking the host`() = runTest {
        val operations = mockk<EntryTrackingOperationHost>(relaxed = true)
        val feature = feature(operations)
        val mutation = EntryTrackingMutation.Private(enabled = true)

        feature.mutate(entry, EntryTrackingServiceId(service.id), mutation) shouldBe
            EntryTrackingOperationResult.Unavailable(
                EntryTrackingOperationUnavailableReason.PRIVATE_TRACKING_UNSUPPORTED,
            )

        coVerify(exactly = 0) { operations.mutate(any(), any(), any()) }
    }

    @Test
    fun `remote deletion failure is reported after local tracking is removed`() = runTest {
        val failure = IllegalStateException("remote unavailable")
        val operations = mockk<EntryTrackingOperationHost>(relaxed = true)
        coEvery { operations.deleteRemote(service.id, track) } throws failure
        val feature = feature(operations)

        val result = feature.remove(entry, EntryTrackingServiceId(service.id), removeRemote = true)

        result.shouldBeInstanceOf<EntryTrackingRemovalResult.Removed>().remoteDeletionFailure shouldBe failure
        coVerifyOrder {
            operations.deleteRemote(service.id, track)
            operations.unregister(entry.id, service.id)
        }
    }

    @Test
    fun `refresh selects the resolved session and reconciles returned tracks`() = runTest {
        val operations = mockk<EntryTrackingOperationHost>()
        val automation = mockk<EntryTrackingAutomationHost>()
        coEvery { operations.refresh(entry.id, setOf(service.id)) } returns
            EntryTrackingHostRefreshResult(listOf(track), emptyList())
        coEvery { automation.reconcileRemoteProgress(entry, service.id, track) } returns Unit
        val feature = feature(operations, automation)

        feature.refresh(entry) shouldBe EntryTrackingRefreshResult.Completed(emptyList())

        coVerify(exactly = 1) { operations.refresh(entry.id, setOf(service.id)) }
        coVerify(exactly = 1) { automation.reconcileRemoteProgress(entry, service.id, track) }
    }

    private fun feature(
        operations: EntryTrackingOperationHost,
        automation: EntryTrackingAutomationHost = mockk(relaxed = true),
    ): EntryTrackingFeature {
        val host = object : EntryTrackingHost {
            override val operations = operations
            override val automation = automation
            override val accounts: EntryTrackingAccountHost = mockk(relaxed = true)
            override val collection: EntryTrackingCollectionHost = mockk(relaxed = true)
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = listOf(service)

            override fun observeEntry(entry: Entry) = flowOf(
                EntryTrackingHostEntrySnapshot(
                    services = listOf(
                        EntryTrackingHostEntryService(
                            service = service,
                            isLoggedIn = true,
                            acceptsSource = true,
                            track = track,
                            displayScore = "7.5",
                        ),
                    ),
                ),
            )
        }
        return DefaultEntryTrackingFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackingFeatureContributor),
            host = host,
        )
    }
}
