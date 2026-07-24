package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryLibraryUpdateRefreshFeatureTest {
    private val entry = Entry.create().copy(id = 7L, profileId = 3L, type = EntryType.BOOK)

    @Test
    fun `Library Update refresh preserves request context and orders inserted children`() = runTest {
        val later = EntryChapter.create().copy(id = 21L, entryId = entry.id, sourceOrder = 9L)
        val earlier = EntryChapter.create().copy(id = 22L, entryId = entry.id, sourceOrder = 2L)
        val sourceRefresh = mockk<EntrySourceRefreshFeature> {
            coEvery { refresh(any()) } returns EntrySourceRefreshResult.Refreshed(
                insertedChildren = listOf(earlier, later),
                insertedChildrenTotal = 2,
                updatedChildren = 0,
                removedChildren = 0,
                metadataChanged = true,
            )
        }
        val feature = feature(sourceRefresh)

        feature.newSession().refresh(
            EntryLibraryUpdateRefreshRequest(
                entry = entry,
                fetchMetadata = false,
                fetchWindowLowerBound = 10L,
                fetchWindowUpperBound = 20L,
            ),
        ) shouldBe EntryLibraryUpdateRefreshResult.Updated(listOf(later, earlier))
        coVerify(exactly = 1) {
            sourceRefresh.refresh(
                EntrySourceRefreshRequest(
                    entry = entry,
                    fetchDetails = false,
                    fetchChildren = true,
                    manual = false,
                    fetchWindow = EntrySourceRefreshWindow(10L, 20L),
                ),
            )
        }
    }

    @Test
    fun `Library Update receives structured source refresh failures`() = runTest {
        val sourceRefresh = mockk<EntrySourceRefreshFeature>()
        val feature = feature(sourceRefresh)
        val request = EntryLibraryUpdateRefreshRequest(entry, true, 0L, 0L)

        coEvery { sourceRefresh.refresh(any()) } returns EntrySourceRefreshResult.SourceUnavailable(entry.source)
        feature.newSession().refresh(request) shouldBe EntryLibraryUpdateRefreshResult.SourceUnavailable

        coEvery { sourceRefresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)
        feature.newSession().refresh(request) shouldBe EntryLibraryUpdateRefreshResult.NoChildren

        val failure = IllegalStateException("refresh failed")
        coEvery { sourceRefresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(failure))
        feature.newSession().refresh(request) shouldBe EntryLibraryUpdateRefreshResult.OperationalFailure(failure)
    }

    private fun feature(sourceRefresh: EntrySourceRefreshFeature): EntryLibraryUpdateRefreshFeature {
        val composition = refreshFeatureTestComposition()
        return DefaultEntryLibraryUpdateRefreshFeature(
            evaluation = composition.featureGraphEvaluation,
            sourceRefresh = sourceRefresh,
            executions = composition.featureExecutions,
        )
    }
}
