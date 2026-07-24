package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntrySourceRefreshFeatureTest {
    private val entry = Entry.create().copy(
        id = 11L,
        source = 7L,
        profileId = 17L,
        type = EntryType.BOOK,
    )
    private val child = EntryChapter.create().copy(id = 31L, entryId = entry.id)

    @Test
    fun `refresh resolves profile policy and maps strict synchronization result`() = runTest {
        val source = mockk<UnifiedSource>()
        val sourceManager = sourceManager(source)
        val sync = mockk<SyncEntryWithSource>()
        val updateTitles = mockk<(Long) -> Boolean> {
            every { this@mockk.invoke(entry.profileId) } returns false
        }
        coEvery {
            sync.syncStrictly(
                entry = entry,
                profileId = entry.profileId,
                updateLibraryTitles = false,
                fetchDetails = false,
                fetchChapters = true,
                manualFetch = true,
                fetchWindow = 10L to 20L,
            )
        } returns SyncEntryWithSource.SyncResult(
            insertedChapters = listOf(child),
            insertedChaptersTotal = 2,
            updatedChapters = 3,
            removedChapters = 4,
            hasMetadataChanges = true,
        )
        val feature = feature(sourceManager, sync, updateTitles)

        feature.refresh(
            EntrySourceRefreshRequest(
                entry = entry,
                fetchDetails = false,
                fetchChildren = true,
                manual = true,
                fetchWindow = EntrySourceRefreshWindow(10L, 20L),
            ),
        ) shouldBe EntrySourceRefreshResult.Refreshed(
            insertedChildren = listOf(child),
            insertedChildrenTotal = 2,
            updatedChildren = 3,
            removedChildren = 4,
            metadataChanged = true,
        )
        verify(exactly = 1) { updateTitles(entry.profileId) }
        coVerify(exactly = 1) { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `source lookup absence is a contextual outcome`() = runTest {
        val sync = mockk<SyncEntryWithSource>(relaxed = true)
        val updateTitles = mockk<(Long) -> Boolean>(relaxed = true)
        val missing = feature(sourceManager(null), sync, updateTitles)

        missing.refresh(EntrySourceRefreshRequest(entry, manual = false)) shouldBe
            EntrySourceRefreshResult.SourceUnavailable(sourceId = entry.source)
        coVerify(exactly = 0) { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { updateTitles(any()) }
    }

    @Test
    fun `empty child and operation failures remain structured while cancellation propagates`() = runTest {
        val sourceManager = sourceManager(mockk<UnifiedSource>())
        val sync = mockk<SyncEntryWithSource>()
        val feature = feature(sourceManager, sync) { true }
        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any()) } throws NoChaptersException()

        feature.refresh(EntrySourceRefreshRequest(entry, manual = false)) shouldBe
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)

        val failure = IllegalStateException("refresh failed")
        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any()) } throws failure
        feature.refresh(EntrySourceRefreshRequest(entry, manual = false))
            .shouldBeInstanceOf<EntrySourceRefreshResult.Failed>()
            .reason shouldBe EntrySourceRefreshFailure.Operation(failure)

        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any()) } throws CancellationException()
        shouldThrow<CancellationException> {
            feature.refresh(EntrySourceRefreshRequest(entry, manual = false))
        }
    }

    @Test
    fun `refresh rejects a request with no operation`() {
        val feature = feature(sourceManager(mockk<UnifiedSource>()), mockk()) { true }

        assertThrows<IllegalArgumentException> {
            kotlinx.coroutines.runBlocking {
                feature.refresh(
                    EntrySourceRefreshRequest(
                        entry,
                        fetchDetails = false,
                        fetchChildren = false,
                        manual = false,
                    ),
                )
            }
        }
    }

    private fun sourceManager(source: UnifiedSource?): SourceManager = mockk {
        every { get(entry.source) } returns source
    }

    private fun feature(
        sourceManager: SourceManager,
        sync: SyncEntryWithSource,
        updateTitles: (Long) -> Boolean,
    ): EntrySourceRefreshFeature {
        val composition = refreshFeatureTestComposition()
        return DefaultEntrySourceRefreshFeature(
            evaluation = composition.featureGraphEvaluation,
            executions = composition.featureExecutions,
            sourceManager = sourceManager,
            syncEntryWithSource = sync,
            updateLibraryTitles = updateTitles,
        )
    }
}
