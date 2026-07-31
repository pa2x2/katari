package mihon.entry.interactions.tracking

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.source.sourceFeatureEvaluation
import mihon.entry.interactions.tracking.host.EntryTrackingAccountHost
import mihon.entry.interactions.tracking.host.EntryTrackingAutomationHost
import mihon.entry.interactions.tracking.host.EntryTrackingBackupHost
import mihon.entry.interactions.tracking.host.EntryTrackingCollectionHost
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.interactions.tracking.host.EntryTrackingHostCollectionSnapshot
import mihon.entry.interactions.tracking.host.EntryTrackingHostCollectionTrack
import mihon.entry.interactions.tracking.host.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.tracking.host.EntryTrackingHostService
import mihon.entry.interactions.tracking.host.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.tracking.host.EntryTrackingOperationHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryTrackingCollectionTest {
    private val firstService = service(7L, setOf(EntryType.BOOK, EntryType.MANGA))
    private val secondService = service(8L, setOf(EntryType.ANIME))
    private val snapshot = EntryTrackingHostCollectionSnapshot(
        services = listOf(firstService, secondService),
        entries = mapOf(
            1L to listOf(
                EntryTrackingHostCollectionTrack(firstService.id, normalizedScore = 8.0, isScored = true),
                EntryTrackingHostCollectionTrack(secondService.id, normalizedScore = 6.0, isScored = true),
            ),
            2L to listOf(
                EntryTrackingHostCollectionTrack(firstService.id, normalizedScore = 0.0, isScored = false),
            ),
        ),
    )

    @Test
    fun `collection projects service applicability and normalized track evidence`() = runTest {
        val feature = feature(snapshot)

        feature.observeCollection().collect { collection ->
            collection.services.map { it.id } shouldBe
                listOf(EntryTrackingServiceId(firstService.id), EntryTrackingServiceId(secondService.id))
            collection.scoreSupportedEntryTypes shouldBe setOf(EntryType.BOOK, EntryType.MANGA, EntryType.ANIME)
            collection.entries.getValue(1L) shouldBe listOf(
                EntryTrackingCollectionTrack(EntryTrackingServiceId(firstService.id), 8.0, isScored = true),
                EntryTrackingCollectionTrack(EntryTrackingServiceId(secondService.id), 6.0, isScored = true),
            )
        }
    }

    @Test
    fun `collection summary counts tracked entries and averages scored entries`() = runTest {
        val feature = feature(snapshot)

        feature.summarizeCollection(setOf(1L, 2L, 404L)) shouldBe EntryTrackingCollectionSummary(
            trackedEntryCount = 2,
            meanScore = 7.0,
            serviceCount = 2,
        )
    }

    private fun feature(snapshot: EntryTrackingHostCollectionSnapshot): EntryTrackingFeature {
        val host = object : EntryTrackingHost {
            override val operations: EntryTrackingOperationHost = mockk(relaxed = true)
            override val automation: EntryTrackingAutomationHost = mockk(relaxed = true)
            override val accounts: EntryTrackingAccountHost = mockk(relaxed = true)
            override val collection = object : EntryTrackingCollectionHost {
                override fun observeCollection() = flowOf(snapshot)
            }
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = snapshot.services

            override fun observeEntry(entry: Entry) = flowOf(EntryTrackingHostEntrySnapshot(emptyList()))
        }
        return DefaultEntryTrackingFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackingFeatureContributor),
            host = host,
        )
    }

    private fun service(id: Long, types: Set<EntryType>) = EntryTrackingHostService(
        id = id,
        name = "Service $id",
        logoResource = 0,
        supportedEntryTypes = types,
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = false,
            supportsAutomaticBinding = false,
        ),
    )
}
