package eu.kanade.tachiyomi.ui.browse.feed

import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedGap
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.base.viewmodel.createViewModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.FakePreferenceStore

class ChronologicalFeedViewModelTest {

    @Test
    fun `init cleans saved favorites while preserving surviving anchor`() = feedTest { viewModels ->
        val preferences = SourcePreferences(FakePreferenceStore(), testJson)
        preferences.hideInLibraryItems.set(true)

        val browseFeedService = BrowseFeedService(preferences)
        browseFeedService.saveTimeline(
            feedId = FEED_ID,
            timeline = SourceFeedTimeline.fromItems(
                listOf(
                    FeedItemRef(1L, EntryType.MANGA),
                    FeedItemRef(2L, EntryType.ANIME),
                ),
                nextPageKey = 2L,
            ),
        )
        browseFeedService.saveAnchor(
            feedId = FEED_ID,
            anchor = SourceFeedAnchor.fromItem(FeedItemRef(2L, EntryType.ANIME), scrollOffset = 24),
        )

        val viewModel = viewModels.fakeFeedViewModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                1L to FakeItem(id = 1L, type = EntryType.MANGA, favorite = true),
                2L to FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
                3L to FakeItem(id = 3L, type = EntryType.ANIME, favorite = true),
                4L to FakeItem(id = 4L, type = EntryType.MANGA, favorite = false),
            ),
        )

        try {
            advanceUntilIdle()

            viewModel.state.value.itemRefs shouldBe listOf(FeedItemRef(2L, EntryType.ANIME))
            viewModel.state.value.savedAnchor shouldBe SourceFeedAnchor.fromItem(
                FeedItemRef(2L, EntryType.ANIME),
                scrollOffset = 24,
            )
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe SourceFeedTimeline.fromItems(
                listOf(FeedItemRef(2L, EntryType.ANIME)),
                nextPageKey = 2L,
            )
            browseFeedService.anchorSnapshot(FEED_ID) shouldBe SourceFeedAnchor.fromItem(
                FeedItemRef(2L, EntryType.ANIME),
                scrollOffset = 24,
            )
        } finally {
            viewModels.clear()
        }
    }

    @Test
    fun `a catch-up that never reaches the saved items stops at the page limit and resumes from the gap`() =
        feedTest { viewModels ->
            val preferences = SourcePreferences(FakePreferenceStore(), testJson)
            val browseFeedService = BrowseFeedService(preferences)
            val oldRef = FeedItemRef(10_000L, EntryType.MANGA)
            browseFeedService.saveTimeline(FEED_ID, SourceFeedTimeline.fromItems(listOf(oldRef), nextPageKey = 99L))
            // An endless listing: page n holds item n and always has a next page.
            val pagingSource = RecordingPagingSource(
                pages = (0L..40L).associate { key ->
                    (if (key == 0L) null else key) to pageResult(listOf(manga(key + 1)), nextKey = key + 1)
                },
            )
            val viewModel = viewModels.fakeFeedViewModel(
                feedId = FEED_ID,
                browseFeedService = browseFeedService,
                workerDispatcher = Dispatchers.Main,
                itemsById = (1L..41L).associateWith { manga(it) } + (oldRef.id to manga(oldRef.id)),
                pagingSourceFactory = { pagingSource },
            )

            try {
                advanceUntilIdle()
                viewModel.refresh()
                advanceUntilIdle()

                val firstBatch = (1L..10L).map { FeedItemRef(it, EntryType.MANGA) }
                pagingSource.loadKeys.size shouldBe FeedViewModel.CATCH_UP_PAGE_LIMIT
                viewModel.state.value.itemRefs shouldBe firstBatch + oldRef
                viewModel.state.value.gaps shouldBe listOf(SourceFeedGap(firstBatch.last(), nextPageKey = 10L))
                viewModel.state.value.newItemsAvailableCount shouldBe 10
                browseFeedService.timelineSnapshot(FEED_ID).gaps shouldBe viewModel.state.value.gaps

                viewModel.loadGap(viewModel.state.value.gaps.single())
                advanceUntilIdle()

                pagingSource.loadKeys.size shouldBe FeedViewModel.CATCH_UP_PAGE_LIMIT * 2
                viewModel.state.value.itemRefs shouldBe (1L..20L).map { FeedItemRef(it, EntryType.MANGA) } + oldRef
                viewModel.state.value.gaps.single().nextPageKey shouldBe 20L
            } finally {
                viewModels.clear()
            }
        }

    @Test
    fun `an empty page during a catch-up ends it instead of failing`() = feedTest { viewModels ->
        val preferences = SourcePreferences(FakePreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        val oldRef = FeedItemRef(100L, EntryType.MANGA)
        browseFeedService.saveTimeline(FEED_ID, SourceFeedTimeline.fromItems(listOf(oldRef), nextPageKey = 99L))
        // Page 2 is missing, which the fake source reports like a catalogue's empty page.
        val pagingSource = RecordingPagingSource(
            pages = mapOf(
                null to pageResult(listOf(manga(1)), nextKey = 1L),
                1L to pageResult(listOf(manga(2)), nextKey = 2L),
            ),
        )
        val viewModel = viewModels.fakeFeedViewModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = listOf(1L, 2L, 100L).associateWith(::manga),
            pagingSourceFactory = { pagingSource },
        )

        try {
            advanceUntilIdle()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.state.value.itemRefs shouldBe listOf(1L, 2L, 100L).map { FeedItemRef(it, EntryType.MANGA) }
            viewModel.state.value.gaps shouldBe emptyList()
            viewModel.state.value.gapError shouldBe null
            browseFeedService.timelineSnapshot(FEED_ID).nextPageKey shouldBe 99L
        } finally {
            viewModels.clear()
        }
    }

    @Test
    fun `a failed catch-up page keeps what loaded and retries from where it failed`() = feedTest { viewModels ->
        val preferences = SourcePreferences(FakePreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        val oldRefs = listOf(FeedItemRef(100L, EntryType.MANGA), FeedItemRef(101L, EntryType.MANGA))
        browseFeedService.saveTimeline(FEED_ID, SourceFeedTimeline.fromItems(oldRefs, nextPageKey = 99L))
        var failures = 0
        val pagingSource = RecordingPagingSource(
            pages = mapOf(
                null to pageResult(listOf(manga(1), manga(2)), nextKey = 1L),
                1L to pageResult(listOf(manga(3), manga(100)), nextKey = 2L),
            ),
            beforeLoad = { key -> if (key == 1L && failures++ == 0) throw RateLimitedException() },
        )
        val viewModel = viewModels.fakeFeedViewModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = listOf(1L, 2L, 3L, 100L, 101L).associateWith(::manga),
            pagingSourceFactory = { pagingSource },
        )

        try {
            advanceUntilIdle()
            viewModel.refresh()
            advanceUntilIdle()

            val loadedRefs = listOf(1L, 2L).map { FeedItemRef(it, EntryType.MANGA) }
            val gap = SourceFeedGap(loadedRefs.last(), nextPageKey = 1L)
            viewModel.state.value.itemRefs shouldBe loadedRefs + oldRefs
            viewModel.state.value.gapError?.gap shouldBe gap
            browseFeedService.timelineSnapshot(FEED_ID).items shouldBe loadedRefs + oldRefs

            viewModel.loadGap(gap)
            advanceUntilIdle()

            viewModel.state.value.itemRefs shouldBe
                listOf(1L, 2L, 3L).map { FeedItemRef(it, EntryType.MANGA) } + oldRefs
            viewModel.state.value.gaps shouldBe emptyList()
            viewModel.state.value.gapError shouldBe null
            pagingSource.loadKeys shouldBe listOf(null, 1L, 1L)
        } finally {
            viewModels.clear()
        }
    }

    companion object {
        private const val FEED_ID = "feed"

        private val testJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun feedTest(testBody: suspend TestScope.(ViewModelStore) -> Unit) = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
        testBody(ViewModelStore())
    } finally {
        Dispatchers.resetMain()
    }
}

private data class FakeItem(
    val id: Long,
    val type: EntryType,
    val favorite: Boolean,
)

private fun ViewModelStore.fakeFeedViewModel(
    feedId: String,
    browseFeedService: BrowseFeedService,
    workerDispatcher: CoroutineDispatcher,
    itemsById: Map<Long, FakeItem> = emptyMap(),
    pagingSourceFactory: () -> RecordingPagingSource<FakeItem> = { RecordingPagingSource(emptyMap()) },
) = createViewModel {
    FeedViewModel(
        feedId = feedId,
        initialFilterSnapshot = emptyList(),
        chronological = true,
        browseFeedService = browseFeedService,
        hideInLibraryItems = true,
        source = FakeFeedItemSource(itemsById, pagingSourceFactory),
        workerDispatcher = workerDispatcher,
    )
}

private class FakeFeedItemSource(
    private val itemsById: Map<Long, FakeItem>,
    private val pagingSourceFactory: () -> RecordingPagingSource<FakeItem>,
) : FeedItemSource<FakeItem> {

    override suspend fun observeItems(refs: List<FeedItemRef>): Flow<List<FakeItem>> {
        return flowOf(refs.mapNotNull { itemsById[it.id] })
    }

    override fun supersedes(candidate: FakeItem, current: FakeItem): Boolean = true

    override suspend fun resolveFilters(): EntryFilterList = EntryFilterList()

    override fun createPagingSource(filters: EntryFilterList): PagingSource<Long, FakeItem> {
        return pagingSourceFactory()
    }

    override fun itemRef(item: FakeItem): FeedItemRef {
        return FeedItemRef(item.id, item.type)
    }

    override fun isItemInLibrary(item: FakeItem): Boolean = item.favorite

    override suspend fun filterNonLibraryRefs(refs: List<FeedItemRef>): List<FeedItemRef> {
        return refs.filter { ref ->
            val item = itemsById[ref.id] ?: return@filter false
            !item.favorite
        }
    }

    override fun isEndOfListing(error: Throwable): Boolean = error is NoResultsException
}

private class RecordingPagingSource<T : Any>(
    private val pages: Map<Long?, PagingSource.LoadResult.Page<Long, T>>,
    private val beforeLoad: suspend (Long?) -> Unit = {},
) : PagingSource<Long, T>() {

    val loadKeys = mutableListOf<Long?>()

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
        loadKeys += params.key
        beforeLoad(params.key)
        return pages[params.key]
            ?: LoadResult.Error(NoResultsException())
    }

    override fun getRefreshKey(state: PagingState<Long, T>): Long? = null
}

private class NoResultsException : Exception()

private class RateLimitedException : Exception()

private fun manga(id: Long) = FakeItem(id = id, type = EntryType.MANGA, favorite = false)

private fun <T : Any> pageResult(data: List<T>, nextKey: Long?): PagingSource.LoadResult.Page<Long, T> {
    return PagingSource.LoadResult.Page(
        data = data,
        prevKey = null,
        nextKey = nextKey,
    )
}
