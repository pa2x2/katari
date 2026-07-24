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
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

class EntryDownloadLifecycleFeatureTest {
    @Test
    fun `provider absence is valid and makes events inapplicable`() = runTest {
        val fixture = fixture(includeDownload = false)
        val visible = entry(id = 1L)

        fixture.feature.onEvent(
            EntryDownloadLifecycleEvent.MarkedConsumed(visible, listOf(chapter(12L, visible.id))),
        ) shouldBe EntryDownloadLifecycleResult.Inapplicable(EntryType.BOOK)
    }

    @Test
    fun `download without bookmarking applies shared cleanup to bookmarked children`() = runTest {
        val visible = entry(id = 1L)
        val bookmarked = chapter(id = 12L, entryId = visible.id, bookmark = true)
        val fixture = fixture()
        fixture.preferences.removeAfterMarkedAsRead.set(true)

        fixture.feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(visible, listOf(bookmarked)))

        coVerify(exactly = 1) { fixture.downloads.delete(visible, listOf(bookmarked)) }
    }

    @Test
    fun `adding bookmarking automatically protects downloads and the override remains contextual`() = runTest {
        val visible = entry(id = 1L)
        val bookmarked = chapter(id = 12L, entryId = visible.id, bookmark = true)
        val fixture = fixture(includeBookmarking = true)
        fixture.preferences.removeAfterMarkedAsRead.set(true)

        fixture.feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(visible, listOf(bookmarked)))
        coVerify(exactly = 0) { fixture.downloads.delete(any(), any()) }

        fixture.preferences.removeBookmarkedChapters.set(true)
        fixture.feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(visible, listOf(bookmarked)))
        coVerify(exactly = 1) { fixture.downloads.delete(visible, listOf(bookmarked)) }
    }

    @Test
    fun `completion cleanup defers physical deletion until the viewer closes`() = runTest {
        val visible = entry(id = 1L)
        val previous = chapter(id = 11L, entryId = visible.id, number = 1.0)
        val current = chapter(id = 12L, entryId = visible.id, number = 2.0)
        val fixture = fixture(readingOrder = listOf(current, previous))
        fixture.preferences.removeAfterReadSlots.set(1)

        fixture.feature.onEvent(EntryDownloadLifecycleEvent.Completed(visible, current))

        coVerify(exactly = 1) { fixture.downloads.cleanup(visible, listOf(previous)) }
        coVerify(exactly = 0) { fixture.downloads.delete(any(), any()) }
    }

    @Test
    fun `excluded categories suppress shared cleanup without changing applicability`() = runTest {
        val visible = entry(id = 1L)
        val consumed = chapter(id = 12L, entryId = visible.id)
        val fixture = fixture(categories = mapOf(visible.id to listOf(category(8L))))
        fixture.preferences.removeAfterMarkedAsRead.set(true)
        fixture.preferences.removeExcludeCategories.set(setOf("8"))

        fixture.feature.onEvent(EntryDownloadLifecycleEvent.MarkedConsumed(visible, listOf(consumed))) shouldBe
            EntryDownloadLifecycleResult.Handled

        coVerify(exactly = 0) { fixture.downloads.delete(any(), any()) }
        fixture.feature.isApplicable(EntryType.BOOK) shouldBe true
    }

    @Test
    fun `download ahead queues missing children once after continuity is established`() = runTest {
        val visible = entry(id = 1L)
        val current = chapter(id = 11L, entryId = visible.id, number = 1.0)
        val next = chapter(id = 12L, entryId = visible.id, number = 2.0)
        val third = chapter(id = 13L, entryId = visible.id, number = 3.0)
        val fourth = chapter(id = 14L, entryId = visible.id, number = 4.0)
        val fixture = fixture(readingOrder = listOf(fourth, third, next, current))
        fixture.preferences.autoDownloadWhileReading.set(2)
        every { fixture.downloads.isDownloaded(visible, current, any()) } returns true
        every { fixture.downloads.isDownloaded(visible, next, any()) } returns true
        every { fixture.downloads.isDownloaded(visible, third, any()) } returns false
        every { fixture.downloads.isDownloaded(visible, fourth, any()) } returns false
        val event = EntryDownloadLifecycleEvent.Progressed(visible, current, fraction = 0.5)

        fixture.feature.onEvent(event)
        fixture.feature.onEvent(event)

        coVerify(exactly = 1) { fixture.downloads.queue(visible, listOf(third, fourth), autoStart = false) }
        verify(exactly = 1) { fixture.downloads.startDownloads() }
    }

    private fun fixture(
        includeDownload: Boolean = true,
        includeBookmarking: Boolean = false,
        memberEntries: List<Entry> = emptyList(),
        readingOrder: List<EntryChapter> = emptyList(),
        categories: Map<Long, List<Category>> = emptyMap(),
    ): Fixture {
        val preferences = DownloadPreferences(InMemoryPreferenceStore())
        val download = downloadProcessor()
        val bindings = buildList<EntryInteractionProviderBinding<*>> {
            if (includeDownload) add(EntryDownloadCapability.bind(download))
            if (includeBookmarking) add(EntryBookmarkCapability.bind(bookmarkProcessor()))
        }
        val plugin = object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.partial-download-lifecycle-type")
            override val providerBindings = bindings
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryDownloadLifecycleFeatureContributor),
        )
        val entryRepository = mockk<EntryRepository>(relaxed = true) {
            coEvery { getEntryById(any()) } answers {
                memberEntries.firstOrNull { it.id == firstArg<Long>() }
            }
        }
        val getCategories = mockk<GetCategories> {
            coEvery { await(any()) } answers { categories[firstArg<Long>()].orEmpty() }
        }
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(any(), any(), any()) } returns readingOrder
        }
        return Fixture(
            preferences = preferences,
            downloads = download,
            feature = DefaultEntryDownloadLifecycleFeature(
                evaluation = composition.featureGraphEvaluation,
                downloadPreferences = preferences,
                getCategories = getCategories,
                getEntryWithChapters = getEntryWithChapters,
                entryRepository = entryRepository,
                downloads = composition.interactions.download,
            ),
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

    private fun bookmarkProcessor(): EntryBookmarkProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }

    private fun entry(id: Long, sourceId: Long = 10L): Entry = Entry.create().copy(
        id = id,
        profileId = 7L,
        source = sourceId,
        type = EntryType.BOOK,
    )

    private fun chapter(
        id: Long,
        entryId: Long,
        number: Double = 1.0,
        bookmark: Boolean = false,
    ): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = entryId,
        chapterNumber = number,
        sourceOrder = id,
        bookmark = bookmark,
    )

    private fun category(id: Long) = Category(id = id, name = "Category $id", order = id, flags = 0L)

    private data class Fixture(
        val preferences: DownloadPreferences,
        val downloads: EntryDownloadProcessor,
        val feature: EntryDownloadLifecycleFeature,
    )
}
