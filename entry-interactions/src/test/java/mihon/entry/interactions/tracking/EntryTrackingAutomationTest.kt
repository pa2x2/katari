package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.tracking.EntryTrackingAccountHost
import mihon.entry.interactions.host.tracking.EntryTrackingAutomationHost
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import mihon.entry.interactions.host.tracking.EntryTrackingCollectionHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostBindingOutcome
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.entry.interactions.host.tracking.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.host.tracking.EntryTrackingOperationHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

class EntryTrackingAutomationTest {
    private val entry = Entry.create().copy(id = 11L, type = EntryType.BOOK)
    private val automaticService = service(id = 7L, automatic = true)
    private val manualService = service(id = 8L, automatic = false)
    private val track = track(serviceId = automaticService.id)

    @Test
    fun `automatic binding discovers external support and reconciles every bound track`() = runTest {
        val automation = mockk<EntryTrackingAutomationHost>()
        coEvery { automation.bindAutomatically(entry, setOf(automaticService.id)) } returns listOf(
            EntryTrackingHostBindingOutcome.Bound(
                automaticService.id,
                automaticService.name,
                track,
            ),
        )
        coEvery { automation.reconcileRemoteProgress(entry, automaticService.id, track) } returns Unit
        val feature = feature(
            automation = automation,
            snapshot = snapshot(
                entryService(automaticService, loggedIn = true, acceptsSource = true),
                entryService(manualService, loggedIn = true, acceptsSource = true),
            ),
        )

        feature.bindAutomatically(entry) shouldBe EntryTrackingAutomaticBindingResult.Completed(
            boundServices = setOf(EntryTrackingServiceId(automaticService.id)),
            unmatchedServices = emptySet(),
            failures = emptyList(),
        )
        coVerify(exactly = 1) { automation.bindAutomatically(entry, setOf(automaticService.id)) }
        coVerify(exactly = 1) { automation.reconcileRemoteProgress(entry, automaticService.id, track) }
    }

    @Test
    fun `automatic binding blocker does not invoke application mechanics`() = runTest {
        val automation = mockk<EntryTrackingAutomationHost>(relaxed = true)
        val feature = feature(
            automation = automation,
            snapshot = snapshot(entryService(automaticService, loggedIn = false, acceptsSource = true)),
        )

        feature.bindAutomatically(entry) shouldBe EntryTrackingAutomaticBindingResult.Unavailable(
            EntryTrackingAutomationUnavailableReason.NOT_LOGGED_IN,
        )
        coVerify(exactly = 0) { automation.bindAutomatically(any(), any()) }
    }

    @Test
    fun `progress synchronization selects only authenticated supported tracked services`() = runTest {
        val secondService = service(id = 9L, automatic = false)
        val automation = mockk<EntryTrackingAutomationHost>()
        coEvery {
            automation.synchronizeProgress(entry.id, setOf(automaticService.id), 5.0, true)
        } returns emptyList()
        val feature = feature(
            automation = automation,
            snapshot = snapshot(
                entryService(automaticService, loggedIn = true, acceptsSource = true, track = track),
                entryService(secondService, loggedIn = false, acceptsSource = true, track = track(secondService.id)),
                entryService(manualService, loggedIn = true, acceptsSource = true, track = null),
            ),
        )

        feature.inspectProgressSynchronization(entry, 5.0) shouldBe EntryTrackingProgressInspection.UpdateRequired
        feature.synchronizeProgress(
            entry,
            5.0,
        ).shouldBeInstanceOf<EntryTrackingProgressSynchronizationResult.Completed>()
        coVerify(exactly = 1) {
            automation.synchronizeProgress(entry.id, setOf(automaticService.id), 5.0, true)
        }
    }

    @Test
    fun `migration preparation is delegated through tracking without a type opt-in`() = runTest {
        val automation = mockk<EntryTrackingAutomationHost>()
        val target = entry.copy(id = 22L)
        val prepared = track.copy(entryId = target.id)
        coEvery { automation.prepareMigrationTracks(entry, target, listOf(track)) } returns listOf(prepared)
        val feature = feature(automation, snapshot())

        feature.prepareMigrationTracks(entry, target, listOf(track.toTrackingRecord())) shouldBe
            EntryTrackingMigrationPreparationResult.Prepared(listOf(prepared.toTrackingRecord()))
        coVerify(exactly = 1) { automation.prepareMigrationTracks(entry, target, listOf(track)) }
    }

    private fun feature(
        automation: EntryTrackingAutomationHost,
        snapshot: EntryTrackingHostEntrySnapshot,
    ): EntryTrackingFeature {
        val host = object : EntryTrackingHost {
            override val operations: EntryTrackingOperationHost = mockk(relaxed = true)
            override val automation = automation
            override val accounts: EntryTrackingAccountHost = mockk(relaxed = true)
            override val collection: EntryTrackingCollectionHost = mockk(relaxed = true)
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = snapshot.services.map(EntryTrackingHostEntryService::service)

            override fun observeEntry(entry: Entry) = flowOf(snapshot)
        }
        return DefaultEntryTrackingFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackingFeatureContributor),
            host = host,
        )
    }

    private fun snapshot(vararg services: EntryTrackingHostEntryService) =
        EntryTrackingHostEntrySnapshot(services.toList())

    private fun entryService(
        service: EntryTrackingHostService,
        loggedIn: Boolean,
        acceptsSource: Boolean,
        track: EntryTrack? = null,
    ) = EntryTrackingHostEntryService(service, loggedIn, acceptsSource, track, displayScore = null)

    private fun service(id: Long, automatic: Boolean) = EntryTrackingHostService(
        id = id,
        name = "Service $id",
        logoResource = 0,
        supportedEntryTypes = setOf(EntryType.BOOK),
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = false,
            supportsAutomaticBinding = automatic,
        ),
    )

    private fun track(serviceId: Long) = EntryTrack(
        id = serviceId,
        entryId = entry.id,
        trackerId = serviceId,
        remoteId = serviceId,
        libraryId = null,
        title = "Tracked",
        progress = 3.0,
        total = 10L,
        status = 1L,
        score = 0.0,
        remoteUrl = "",
        startDate = 0L,
        finishDate = 0L,
        private = false,
    )
}
