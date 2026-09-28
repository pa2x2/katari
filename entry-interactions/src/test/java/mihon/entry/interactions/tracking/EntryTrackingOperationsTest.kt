package mihon.entry.interactions.tracking

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.source.sourceFeatureEvaluation
import mihon.entry.interactions.tracking.host.EntryTrackingAccountHost
import mihon.entry.interactions.tracking.host.EntryTrackingAutomationHost
import mihon.entry.interactions.tracking.host.EntryTrackingBackupHost
import mihon.entry.interactions.tracking.host.EntryTrackingCollectionHost
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.interactions.tracking.host.EntryTrackingHostEntryService
import mihon.entry.interactions.tracking.host.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.tracking.host.EntryTrackingHostService
import mihon.entry.interactions.tracking.host.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.tracking.host.EntryTrackingOperationHost
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
