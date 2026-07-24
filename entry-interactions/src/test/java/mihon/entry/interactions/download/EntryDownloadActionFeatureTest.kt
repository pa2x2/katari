package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryDownloadActionFeatureTest {
    private val entry = Entry.create().copy(id = 7L, source = 11L, type = EntryType.BOOK)
    private val localEntry = entry.copy(source = 12L)
    private val remoteRequest = EntryDownloadActionRequest(EntryType.BOOK, setOf(entry.source))
    private val localRequest = EntryDownloadActionRequest(EntryType.BOOK, setOf(localEntry.source))
    private val chapter = EntryChapter.create().copy(id = 12L, entryId = entry.id)

    @Test
    fun `a core provider does not imply bulk actions`() = runTest {
        val feature = featureFor(EntryDownloadCapability.bind(downloadProcessor()))

        feature.bulkAvailability(listOf(remoteRequest), EntryBulkDownloadAction.unread) shouldBe
            EntryDownloadActionAvailability.Inapplicable(setOf(EntryType.BOOK))
    }

    @Test
    fun `bulk provider activates shared candidate selection without bookmarking`() = runTest {
        val unread = chapter.copy(id = 13L, read = false)
        val read = chapter.copy(id = 14L, read = true)
        val feature = featureFor(
            EntryDownloadCapability.bind(downloadProcessor()),
            EntryBulkDownloadCandidateCapability.bind(candidateProcessor(listOf(unread, read))),
        )

        feature.resolveBulkDownloadCandidates(
            EntryBulkDownloadRequest(entry, EntryBulkDownloadAction.unread),
        ) shouldBe EntryBulkDownloadResolutionResult.Candidates(listOf(unread))
        feature.resolveBulkDownloadCandidates(
            EntryBulkDownloadRequest(entry, EntryBulkDownloadAction.bookmarked),
        ) shouldBe EntryBulkDownloadResolutionResult.Inapplicable(setOf(EntryType.BOOK))
    }

    @Test
    fun `adding bookmarking automatically activates bookmarked bulk selection`() = runTest {
        val regular = chapter.copy(id = 15L, bookmark = false)
        val bookmarked = chapter.copy(id = 16L, bookmark = true, read = true)
        val feature = featureFor(
            EntryDownloadCapability.bind(downloadProcessor()),
            EntryBulkDownloadCandidateCapability.bind(candidateProcessor(listOf(regular, bookmarked))),
            EntryBookmarkCapability.bind(bookmarkProcessor()),
        )

        feature.bulkAvailability(listOf(remoteRequest), EntryBulkDownloadAction.bookmarked) shouldBe
            EntryDownloadActionAvailability.Available
        feature.resolveBulkDownloadCandidates(
            EntryBulkDownloadRequest(entry, EntryBulkDownloadAction.bookmarked),
        ) shouldBe EntryBulkDownloadResolutionResult.Candidates(listOf(bookmarked))
    }

    @Test
    fun `local state and empty selections are contextual blockers`() = runTest {
        val download = downloadProcessor()
        val feature = featureFor(EntryDownloadCapability.bind(download))

        feature.individualAvailability(localRequest) shouldBe EntryDownloadActionAvailability.Blocked(
            setOf(EntryDownloadActionBlocker.LOCAL_OR_STUB),
        )
        feature.download(localEntry, listOf(chapter)) shouldBe EntryDownloadActionResult.Blocked(
            setOf(EntryDownloadActionBlocker.LOCAL_OR_STUB),
        )
        feature.download(entry, emptyList()) shouldBe EntryDownloadActionResult.Blocked(
            setOf(EntryDownloadActionBlocker.EMPTY_SELECTION),
        )
        feature.bulkAvailability(emptyList(), EntryBulkDownloadAction.unread) shouldBe
            EntryDownloadActionAvailability.Blocked(setOf(EntryDownloadActionBlocker.EMPTY_SELECTION))
        feature.individualSelectionAvailability(
            listOf(remoteRequest, EntryDownloadActionRequest(EntryType.MANGA, setOf(entry.source))),
        ) shouldBe EntryDownloadActionAvailability.Inapplicable(setOf(EntryType.MANGA))

        coVerify(exactly = 0) { download.download(any(), any(), any()) }
    }

    @Test
    fun `notification action adds its selection limit without changing type support`() {
        val feature = featureFor(EntryDownloadCapability.bind(downloadProcessor()))

        feature.notificationAvailability(entry, 0) shouldBe EntryDownloadActionAvailability.Blocked(
            setOf(EntryDownloadActionBlocker.EMPTY_SELECTION),
        )
        feature.notificationAvailability(entry, 15) shouldBe EntryDownloadActionAvailability.Available
        feature.notificationAvailability(entry, 16) shouldBe EntryDownloadActionAvailability.Blocked(
            setOf(EntryDownloadActionBlocker.NOTIFICATION_SELECTION_TOO_LARGE),
        )
        feature.individualAvailability(remoteRequest) shouldBe EntryDownloadActionAvailability.Available
    }

    private fun featureFor(vararg bindings: EntryInteractionProviderBinding<*>): EntryDownloadActionFeature {
        val plugin = object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.partial-download-type")
            override val providerBindings = bindings.toList()
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryDownloadActionFeatureContributor),
        )
        return DefaultEntryDownloadActionFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.download,
            sourceAccessResolver = EntryDownloadSourceAccessResolver { sourceIds ->
                if (localEntry.source in sourceIds) {
                    EntryDownloadSourceAccess.LOCAL_OR_STUB
                } else {
                    EntryDownloadSourceAccess.REMOTE
                }
            },
        )
    }

    private fun downloadProcessor(): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
            every { changes } returns emptyFlow()
            every { isInitializing } returns flowOf(false)
            every { isRunning } returns flowOf(false)
            every { queueState } returns flowOf(emptyList())
            every { events } returns emptyFlow()
            every { updates() } returns emptyFlow()
            every { queueStatusUpdates() } returns emptyFlow()
            every { queueProgressUpdates() } returns emptyFlow()
        }
    }

    private fun candidateProcessor(pool: List<EntryChapter>): EntryBulkDownloadCandidateProcessor {
        return mockk {
            every { type } returns EntryType.BOOK
            coEvery { resolveBulkDownloadCandidatePool(any(), any()) } returns pool
        }
    }

    private fun bookmarkProcessor(): EntryBookmarkProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }
}
