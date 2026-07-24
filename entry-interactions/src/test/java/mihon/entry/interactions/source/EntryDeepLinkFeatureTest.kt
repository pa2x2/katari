package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUriType
import eu.kanade.tachiyomi.source.entry.ResolvableSource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager

class EntryDeepLinkFeatureTest {

    @Test
    fun `no resolver no match and resolver failure are distinct outcomes`() = runTest {
        val sourceManager = mockk<SourceManager>()
        val feature = feature(sourceManager)

        every { sourceManager.getAll() } returns emptyList()
        feature.resolve("unknown") shouldBe EntryDeepLinkResolution.NoMatch

        val resolver = mockk<ResolvableSource> {
            every { getUriType("unknown") } returns EntryUriType.Unknown
        }
        every { sourceManager.getAll() } returns listOf(resolver)
        feature.resolve("unknown") shouldBe EntryDeepLinkResolution.NoMatch

        val error = IllegalStateException("resolver failure")
        every { resolver.getUriType("unknown") } throws error
        feature.resolve("unknown") shouldBe EntryDeepLinkResolution.Failed(error)
    }

    @Test
    fun `missing deep linked child is resolved through Source Refresh Feature`() = runTest {
        val sourceChild = SEntryChapter.create().apply { url = "/child" }
        val networkEntry = SEntry.create().apply {
            url = "/entry"
            title = "Entry"
            type = EntryType.MANGA
        }
        val persistedEntry = Entry.create().copy(id = 9L, source = 4L, type = EntryType.MANGA)
        val insertedChild = EntryChapter.create().copy(id = 12L, entryId = persistedEntry.id, url = sourceChild.url)
        val resolver = mockk<ResolvableSource> {
            every { id } returns persistedEntry.source
            every { getUriType("https://example.test/child") } returns EntryUriType.Chapter
            coEvery { getEntry("https://example.test/child") } returns networkEntry
            coEvery { getChapter("https://example.test/child") } returns sourceChild
        }
        val sourceRefresh = mockk<EntrySourceRefreshFeature> {
            coEvery { refresh(any()) } returns EntrySourceRefreshResult.Refreshed(
                insertedChildren = listOf(insertedChild),
                insertedChildrenTotal = 1,
                updatedChildren = 0,
                removedChildren = 0,
                metadataChanged = false,
            )
        }
        val feature = DefaultEntryDeepLinkFeature(
            evaluation = sourceFeatureEvaluation(EntryDeepLinkFeatureContributor),
            sourceManager = mockk { every { getAll() } returns listOf(resolver) },
            networkToLocalEntry = mockk {
                coEvery { this@mockk.invoke(any<Entry>()) } returns persistedEntry
            },
            entryChapterRepository = mockk {
                coEvery { getChapterByUrlAndEntryId(sourceChild.url, persistedEntry.id) } returns null
            },
            sourceRefresh = sourceRefresh,
        )

        feature.resolve("https://example.test/child") shouldBe
            EntryDeepLinkResolution.Resolved(persistedEntry, insertedChild.id)
    }

    private fun feature(sourceManager: SourceManager) = DefaultEntryDeepLinkFeature(
        evaluation = sourceFeatureEvaluation(EntryDeepLinkFeatureContributor),
        sourceManager = sourceManager,
        networkToLocalEntry = mockk<NetworkToLocalEntry>(),
        entryChapterRepository = mockk<EntryChapterRepository>(),
        sourceRefresh = mockk<EntrySourceRefreshFeature>(),
    )
}
