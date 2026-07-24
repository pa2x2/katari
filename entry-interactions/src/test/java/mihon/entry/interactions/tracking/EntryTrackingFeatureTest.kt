package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.tracking.EntryTrackingAccountHost
import mihon.entry.interactions.host.tracking.EntryTrackingAutomationHost
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import mihon.entry.interactions.host.tracking.EntryTrackingCollectionHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntryService
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.entry.interactions.host.tracking.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.host.tracking.EntryTrackingOperationHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

class EntryTrackingFeatureTest {
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
    private val bookService = EntryTrackingHostService(
        id = 7L,
        name = "Future Books",
        logoResource = 19,
        supportedEntryTypes = setOf(EntryType.BOOK),
        capabilities = EntryTrackingHostServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = false,
            supportsAutomaticBinding = false,
        ),
    )

    @Test
    fun `external tracker support activates a contributed type without a type provider`() = runTest {
        val feature = feature(
            services = listOf(bookService),
            snapshots = listOf(
                EntryTrackingHostEntrySnapshot(
                    listOf(
                        EntryTrackingHostEntryService(
                            service = bookService,
                            isLoggedIn = true,
                            acceptsSource = true,
                            track = track,
                            displayScore = "7.5",
                        ),
                    ),
                ),
            ),
        )

        feature.availability(EntryType.BOOK) shouldBe EntryTrackingAvailability.Available(
            listOf(bookService.descriptor()),
        )
        feature.observeSession(entry).toList() shouldBe listOf(
            EntryTrackingSession.Available(
                listOf(
                    EntryTrackingSessionService(
                        bookService.descriptor(),
                        track.toTrackingRecord(),
                        displayScore = "7.5",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `session reports the contextual blocker without changing type validity`() = runTest {
        val feature = feature(
            services = listOf(bookService),
            snapshots = listOf(
                EntryTrackingHostEntrySnapshot(
                    listOf(
                        EntryTrackingHostEntryService(
                            service = bookService,
                            isLoggedIn = false,
                            acceptsSource = true,
                            track = null,
                            displayScore = null,
                        ),
                    ),
                ),
            ),
        )

        feature.availability(EntryType.BOOK).shouldBeInstanceOf<EntryTrackingAvailability.Available>()
        feature.observeSession(entry).toList() shouldBe listOf(
            EntryTrackingSession.Unavailable(setOf(EntryTrackingSessionUnavailableReason.NOT_LOGGED_IN)),
        )
    }

    private fun feature(
        services: List<EntryTrackingHostService>,
        snapshots: List<EntryTrackingHostEntrySnapshot>,
    ): EntryTrackingFeature {
        val host = object : EntryTrackingHost {
            override val operations: EntryTrackingOperationHost = mockk(relaxed = true)
            override val automation: EntryTrackingAutomationHost = mockk(relaxed = true)
            override val accounts: EntryTrackingAccountHost = mockk(relaxed = true)
            override val collection: EntryTrackingCollectionHost = mockk(relaxed = true)
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = services

            override fun observeEntry(entry: Entry) = flowOf(*snapshots.toTypedArray())
        }
        return DefaultEntryTrackingFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackingFeatureContributor),
            host = host,
        )
    }

    private fun EntryTrackingHostService.descriptor() = EntryTrackingServiceDescriptor(
        id = EntryTrackingServiceId(id),
        name = name,
        logoResource = logoResource,
        capabilities = EntryTrackingServiceCapabilities(
            statuses = emptyList(),
            scores = emptyList(),
            supportsReadingDates = false,
            supportsPrivateTracking = false,
            supportsRemoteDeletion = false,
            supportsAutomaticBinding = false,
        ),
    )
}
