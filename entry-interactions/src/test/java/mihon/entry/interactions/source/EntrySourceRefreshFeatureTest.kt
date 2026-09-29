package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.refresh.refreshFeatureTestComposition
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager

class EntrySourceRefreshFeatureTest {
    private val entry = Entry.create().copy(
        id = 11L,
        source = 7L,
        profileId = 17L,
        type = EntryType.BOOK,
    )

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
