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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager

@OptIn(ExperimentalCoroutinesApi::class)
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
        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any(), any()) } throws
            NoChaptersException()

        feature.refresh(EntrySourceRefreshRequest(entry, manual = false)) shouldBe
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)

        val failure = IllegalStateException("refresh failed")
        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any(), any()) } throws failure
        feature.refresh(EntrySourceRefreshRequest(entry, manual = false))
            .shouldBeInstanceOf<EntrySourceRefreshResult.Failed>()
            .reason shouldBe EntrySourceRefreshFailure.Operation(failure)

        coEvery { sync.syncStrictly(any(), any(), any(), any(), any(), any(), any(), any()) } throws
            CancellationException()
        shouldThrow<CancellationException> {
            feature.refresh(EntrySourceRefreshRequest(entry, manual = false))
        }
    }

    @Test
    fun `a refresh started before the sources load waits for them instead of reporting the source missing`() =
        runTest {
            val sourcesInitialized = MutableStateFlow(false)
            val source = mockk<UnifiedSource>()
            val sourceManager = mockk<SourceManager> {
                every { isInitialized } returns sourcesInitialized
                every { get(entry.source) } answers { source.takeIf { sourcesInitialized.value } }
            }
            val sync = mockk<SyncEntryWithSource> {
                coEvery { syncStrictly(any(), any(), any(), any(), any(), any(), any(), any()) } returns
                    SyncEntryWithSource.SyncResult(emptyList(), 0, 0, emptyList(), false)
            }
            val feature = feature(sourceManager, sync) { true }

            val refreshing = async { feature.refresh(EntrySourceRefreshRequest(entry, manual = false)) }
            runCurrent()
            sourcesInitialized.value = true

            refreshing.await().shouldBeInstanceOf<EntrySourceRefreshResult.Refreshed>()
        }

    private fun sourceManager(source: UnifiedSource?): SourceManager = mockk {
        every { isInitialized } returns MutableStateFlow(true)
        every { get(entry.source) } returns source
    }

    private fun feature(
        sourceManager: SourceManager,
        sync: SyncEntryWithSource,
        updateTitles: (Long) -> Boolean,
    ): EntrySourceRefreshFeature {
        return DefaultEntrySourceRefreshFeature(
            automaticDownloads = { mockk(relaxed = true) },
            sourceManager = sourceManager,
            syncEntryWithSource = sync,
            downloadRuntime = { mockk(relaxed = true) },
            updateLibraryTitles = updateTitles,
            networkToLocalEntry = mockk(),
            mergeSourceGroups = { mockk() },
            finishChapterListWalk = mockk(),
        )
    }
}
