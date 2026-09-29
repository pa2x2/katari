package eu.kanade.tachiyomi.ui.browse.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.service.BrowseFeedPreferenceStore
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ChronologicalFeedScreenModelTest {

    @Test
    fun `init cleans saved favorites while preserving surviving anchor`() = feedTest {
        val preferences = SourcePreferences(BrowseFeedPreferenceStore(), testJson)
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

        val screenModel = FakeFeedScreenModel(
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

            screenModel.state.value.itemRefs shouldBe listOf(FeedItemRef(2L, EntryType.ANIME))
            screenModel.state.value.savedAnchor shouldBe SourceFeedAnchor.fromItem(
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
            screenModel.onDispose()
        }
    }

    @Test
    fun `partial bridge pages stay behind the loading boundary until overlap`() = feedTest {
        val preferences = SourcePreferences(BrowseFeedPreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        val oldRef = FeedItemRef(100L, EntryType.MANGA)
        browseFeedService.saveTimeline(
            FEED_ID,
            SourceFeedTimeline.fromItems(listOf(oldRef), nextPageKey = 10L),
        )
        browseFeedService.saveAnchor(
            FEED_ID,
            SourceFeedAnchor.fromItem(oldRef, scrollOffset = 32),
        )
        val continueBridge = CompletableDeferred<Unit>()
        val pagingSource = RecordingPagingSource(
            pages = mapOf(
                null to pageResult(
                    data = listOf(
                        FakeItem(id = 1L, type = EntryType.MANGA, favorite = false),
                        FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
                    ),
                    nextKey = 1L,
                ),
                1L to pageResult(
                    data = listOf(FakeItem(id = 100L, type = EntryType.MANGA, favorite = false)),
                    nextKey = 2L,
                ),
            ),
            beforeLoad = { key ->
                if (key == 1L) continueBridge.await()
            },
        )
        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                1L to FakeItem(id = 1L, type = EntryType.MANGA, favorite = false),
                2L to FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
                100L to FakeItem(id = 100L, type = EntryType.MANGA, favorite = false),
            ),
            pagingSourceFactory = { pagingSource },
        )

        try {
            advanceUntilIdle()
            screenModel.refresh(manual = true)
            runCurrent()

            val bridgedRefs = listOf(
                FeedItemRef(1L, EntryType.MANGA),
                FeedItemRef(2L, EntryType.ANIME),
            )
            screenModel.state.value.itemRefs shouldBe listOf(oldRef)
            screenModel.state.value.isBridgingRefresh shouldBe true
            screenModel.state.value.pendingRefresh shouldBe FeedScreenModel.PendingRefresh(
                itemRefs = bridgedRefs,
                nextPageKey = 1L,
            )
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe
                SourceFeedTimeline.fromItems(listOf(oldRef), nextPageKey = 10L)

            screenModel.saveAnchor(oldRef, scrollOffset = 12)
            browseFeedService.anchorSnapshot(FEED_ID) shouldBe
                SourceFeedAnchor.fromItem(oldRef, scrollOffset = 12)

            continueBridge.complete(Unit)
            advanceUntilIdle()

            val mergedRefs = bridgedRefs + oldRef
            screenModel.state.value.itemRefs shouldBe mergedRefs
            screenModel.state.value.pendingRefresh shouldBe null
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe
                SourceFeedTimeline.fromItems(mergedRefs, nextPageKey = 10L)
            browseFeedService.anchorSnapshot(FEED_ID) shouldBe
                SourceFeedAnchor.fromItem(oldRef, scrollOffset = 12)
        } finally {
            screenModel.onDispose()
        }
    }

    @Test
    fun `refresh without any overlap keeps an explicit switch to the newest timeline`() = feedTest {
        val preferences = SourcePreferences(BrowseFeedPreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        val oldRef = FeedItemRef(100L, EntryType.MANGA)
        browseFeedService.saveTimeline(
            FEED_ID,
            SourceFeedTimeline.fromItems(listOf(oldRef), nextPageKey = 10L),
        )
        browseFeedService.saveAnchor(
            FEED_ID,
            SourceFeedAnchor.fromItem(oldRef, scrollOffset = 32),
        )
        val pagingSource = RecordingPagingSource(
            pages = mapOf(
                null to pageResult(
                    data = listOf(
                        FakeItem(id = 1L, type = EntryType.MANGA, favorite = false),
                        FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
                    ),
                    nextKey = 1L,
                ),
                1L to pageResult(
                    data = listOf(FakeItem(id = 3L, type = EntryType.MANGA, favorite = false)),
                    nextKey = null,
                ),
            ),
        )
        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                1L to FakeItem(id = 1L, type = EntryType.MANGA, favorite = false),
                2L to FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
                3L to FakeItem(id = 3L, type = EntryType.MANGA, favorite = false),
                100L to FakeItem(id = 100L, type = EntryType.MANGA, favorite = false),
            ),
            pagingSourceFactory = { pagingSource },
        )

        try {
            advanceUntilIdle()
            screenModel.refresh(manual = true)
            advanceUntilIdle()

            val newestRefs = listOf(
                FeedItemRef(1L, EntryType.MANGA),
                FeedItemRef(2L, EntryType.ANIME),
                FeedItemRef(3L, EntryType.MANGA),
            )
            pagingSource.loadKeys shouldBe listOf(null, 1L)
            screenModel.state.value.itemRefs shouldBe listOf(oldRef)
            screenModel.state.value.pendingRefresh shouldBe FeedScreenModel.PendingRefresh(
                itemRefs = newestRefs,
                nextPageKey = null,
            )
            screenModel.state.value.newItemsAvailableCount shouldBe 3
            screenModel.state.value.newItemsCountIsLowerBound shouldBe false
            screenModel.state.value.isBridgingRefresh shouldBe false

            screenModel.showNewItems()
            advanceUntilIdle()

            screenModel.state.value.itemRefs shouldBe newestRefs
            screenModel.state.value.pendingRefresh shouldBe null
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe
                SourceFeedTimeline.fromItems(newestRefs, nextPageKey = null)
            browseFeedService.anchorSnapshot(FEED_ID) shouldBe
                SourceFeedAnchor.fromItem(newestRefs.first(), scrollOffset = 0)
        } finally {
            screenModel.onDispose()
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
private fun feedTest(testBody: suspend TestScope.() -> Unit) = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
        testBody()
    } finally {
        Dispatchers.resetMain()
    }
}

private data class FakeItem(
    val id: Long,
    val type: EntryType,
    val favorite: Boolean,
)

private class FakeFeedScreenModel(
    feedId: String,
    browseFeedService: BrowseFeedService,
    workerDispatcher: CoroutineDispatcher,
    private val itemsById: Map<Long, FakeItem> = emptyMap(),
    private val pagingSourceFactory: () -> RecordingPagingSource<FakeItem> = { RecordingPagingSource(emptyMap()) },
) : FeedScreenModel<FakeItem>(
    feedId = feedId,
    listingQuery = null,
    initialFilterSnapshot = emptyList(),
    chronological = true,
    browseFeedService = browseFeedService,
    hideInLibraryItems = true,
    workerDispatcher = Dispatchers.Main,
) {

    override suspend fun subscribeItem(ref: FeedItemRef): Flow<FakeItem> {
        val item = itemsById[ref.id] ?: error("Unknown item $ref")
        return flowOf(item)
    }

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

private fun <T : Any> pageResult(data: List<T>, nextKey: Long?): PagingSource.LoadResult.Page<Long, T> {
    return PagingSource.LoadResult.Page(
        data = data,
        prevKey = null,
        nextKey = nextKey,
    )
}
