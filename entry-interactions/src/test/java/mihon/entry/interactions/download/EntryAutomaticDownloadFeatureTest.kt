package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class EntryAutomaticDownloadFeatureTest {
    private val entry = Entry.create().copy(
        id = 7L,
        type = EntryType.BOOK,
        favorite = true,
    )
    private val chapter = EntryChapter.create().copy(id = 12L, entryId = entry.id)

    @Test
    fun `a core Download provider activates both automatic download paths`() = runTest {
        val processor = downloadProcessor()
        val policy = mockk<EntryAutomaticDownloadPolicy> {
            coEvery { evaluate(entry, listOf(chapter)) } returns selectedDecision(listOf(chapter))
        }
        val composition = compositionFor(plugin(EntryDownloadCapability.bind(processor)))
        val feature = featureFor(composition, policy)

        feature.downloadAfterEntryRefresh(entry, listOf(chapter)) shouldBe
            EntryAutomaticDownloadResult.Scheduled(1)
        val libraryUpdate = feature.newLibraryUpdateBatch()
        libraryUpdate.enqueue(entry, listOf(chapter)) shouldBe EntryAutomaticDownloadResult.Scheduled(1)

        coVerify(exactly = 1) { processor.download(entry, listOf(chapter), startNow = false) }
        coVerify(exactly = 1) { processor.queue(entry, listOf(chapter), autoStart = false) }
        verify(exactly = 0) { processor.startDownloads() }

        libraryUpdate.complete()
        libraryUpdate.complete()

        verify(exactly = 1) { processor.startDownloads() }
    }

    @Test
    fun `a disabled library update batch does not start unrelated work`() = runTest {
        val processor = downloadProcessor()
        val policy = EntryAutomaticDownloadPolicy(
            entryChapterRepository = mockk(relaxed = true),
            downloadPreferences = DownloadPreferences(InMemoryPreferenceStore()),
            getCategories = mockk(relaxed = true),
        )
        val feature = featureFor(compositionFor(plugin(EntryDownloadCapability.bind(processor))), policy)
        val libraryUpdate = feature.newLibraryUpdateBatch()

        libraryUpdate.enqueue(entry, listOf(chapter)) shouldBe EntryAutomaticDownloadResult.Blocked(
            setOf(EntryAutomaticDownloadBlocker.DISABLED),
        )
        libraryUpdate.complete()

        coVerify(exactly = 0) { processor.queue(any(), any(), any()) }
        verify(exactly = 0) { processor.startDownloads() }
    }

    @Test
    fun `provider absence is valid and does not evaluate contextual policy`() = runTest {
        val policy = mockk<EntryAutomaticDownloadPolicy>()
        val feature = featureFor(compositionFor(), policy)

        feature.isApplicable(entry.type) shouldBe false
        feature.downloadAfterEntryRefresh(entry, listOf(chapter)) shouldBe
            EntryAutomaticDownloadResult.Inapplicable(EntryType.BOOK)

        coVerify(exactly = 0) { policy.evaluate(any(), any()) }
    }

    @Test
    fun `category and preference policy stays shared operation context`() = runTest {
        val preferences = DownloadPreferences(InMemoryPreferenceStore()).apply {
            downloadNewEntryChapters.set(true)
            downloadNewEntryChapterCategoriesExclude.set(setOf("9"))
        }
        val chapters = mockk<EntryChapterRepository>(relaxed = true)
        val categories = mockk<GetCategories> {
            coEvery { await(entry.id) } returns listOf(Category(9L, "Excluded", 0L, 0L))
        }
        val policy = EntryAutomaticDownloadPolicy(chapters, preferences, categories)
        val processor = downloadProcessor()
        val feature = featureFor(compositionFor(plugin(EntryDownloadCapability.bind(processor))), policy)

        feature.downloadAfterEntryRefresh(entry, listOf(chapter)) shouldBe EntryAutomaticDownloadResult.Blocked(
            setOf(EntryAutomaticDownloadBlocker.CATEGORY_POLICY_REJECTED),
        )

        coVerify(exactly = 0) { processor.download(any(), any(), any()) }
    }

    @Test
    fun `unread-only policy reports when prior consumption removes every candidate`() = runTest {
        val candidate = chapter.copy(chapterNumber = 3.0)
        val preferences = DownloadPreferences(InMemoryPreferenceStore()).apply {
            downloadNewEntryChapters.set(true)
            downloadNewUnreadEntryChaptersOnly.set(true)
        }
        val chapters = mockk<EntryChapterRepository> {
            coEvery { getChaptersByEntryIdAwait(entry.id) } returns listOf(candidate.copy(read = true))
        }
        val categories = mockk<GetCategories> {
            coEvery { await(entry.id) } returns emptyList()
        }
        val processor = downloadProcessor()
        val feature = featureFor(
            compositionFor(plugin(EntryDownloadCapability.bind(processor))),
            EntryAutomaticDownloadPolicy(chapters, preferences, categories),
        )

        feature.downloadAfterEntryRefresh(entry, listOf(candidate)) shouldBe EntryAutomaticDownloadResult.Blocked(
            setOf(EntryAutomaticDownloadBlocker.NO_UNREAD_CANDIDATES),
        )

        coVerify(exactly = 0) { processor.download(any(), any(), any()) }
    }

    private fun compositionFor(vararg plugins: EntryInteractionPlugin): EntryInteractionComposition {
        return createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(EntryAutomaticDownloadFeatureContributor),
        )
    }

    private fun featureFor(
        composition: EntryInteractionComposition,
        policy: EntryAutomaticDownloadPolicy,
    ): EntryAutomaticDownloadCoordinator {
        return DefaultEntryAutomaticDownloadFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.download,
            sharedPolicy = policy,
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.book")
            override val providerBindings = bindings.toList()
        }
    }

    private fun downloadProcessor(): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }

    private fun selectedDecision(candidates: List<EntryChapter>) = EntryAutomaticDownloadPolicyDecision(
        candidates = candidates,
        hasNewChapters = true,
        enabled = true,
        favorite = true,
        categoryAllowed = true,
        unreadOnly = false,
    )
}
