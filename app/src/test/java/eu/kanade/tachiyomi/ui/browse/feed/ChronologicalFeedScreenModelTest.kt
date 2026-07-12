package eu.kanade.tachiyomi.ui.browse.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.model.CatalogListItem

class ChronologicalFeedScreenModelTest {

    @Test
    fun `init cleans saved favorites while preserving surviving anchor`() = feedTest {
        val preferences = SourcePreferences(TestPreferenceStore(), testJson)
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
    fun `saving anchor updates offset while visible item remains unchanged`() = feedTest {
        val preferences = SourcePreferences(TestPreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        val itemRef = FeedItemRef(2L, EntryType.ANIME)
        browseFeedService.saveTimeline(
            feedId = FEED_ID,
            timeline = SourceFeedTimeline.fromItems(listOf(itemRef), nextPageKey = null),
        )
        browseFeedService.saveAnchor(
            feedId = FEED_ID,
            anchor = SourceFeedAnchor.fromItem(itemRef, scrollOffset = 12),
        )

        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                2L to FakeItem(id = 2L, type = EntryType.ANIME, favorite = false),
            ),
        )

        try {
            advanceUntilIdle()

            screenModel.saveAnchor(itemRef, scrollOffset = 48)

            screenModel.savedAnchorSnapshot() shouldBe SourceFeedAnchor.fromItem(itemRef, scrollOffset = 48)
            browseFeedService.anchorSnapshot(FEED_ID) shouldBe
                SourceFeedAnchor.fromItem(itemRef, scrollOffset = 48)
        } finally {
            screenModel.onDispose()
        }
    }

    @Test
    fun `initial refresh filters favorites when hide in library items is enabled`() = feedTest {
        val preferences = SourcePreferences(TestPreferenceStore(), testJson)
        preferences.hideInLibraryItems.set(true)
        val browseFeedService = BrowseFeedService(preferences)

        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                3L to FakeItem(id = 3L, type = EntryType.MANGA, favorite = true),
                4L to FakeItem(id = 4L, type = EntryType.ANIME, favorite = false),
            ),
            pagingSourceFactory = {
                RecordingPagingSource(
                    pages = mapOf(
                        null to pageResult(
                            data = listOf(
                                FakeItem(id = 3L, type = EntryType.MANGA, favorite = true),
                                FakeItem(id = 4L, type = EntryType.ANIME, favorite = false),
                            ),
                            nextKey = null,
                        ),
                    ),
                )
            },
        )

        try {
            advanceUntilIdle()

            screenModel.state.value.itemRefs shouldBe listOf(FeedItemRef(4L, EntryType.ANIME))
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe SourceFeedTimeline.fromItems(
                listOf(FeedItemRef(4L, EntryType.ANIME)),
                nextPageKey = null,
            )
        } finally {
            screenModel.onDispose()
        }
    }

    @Test
    fun `init with existing timeline skips automatic refresh`() = feedTest {
        val preferences = SourcePreferences(TestPreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        browseFeedService.saveTimeline(
            feedId = FEED_ID,
            timeline = SourceFeedTimeline.fromItems(
                listOf(
                    FeedItemRef(7L, EntryType.MANGA),
                    FeedItemRef(8L, EntryType.ANIME),
                ),
                nextPageKey = 9L,
            ),
        )

        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                7L to FakeItem(id = 7L, type = EntryType.MANGA, favorite = false),
                8L to FakeItem(id = 8L, type = EntryType.ANIME, favorite = false),
            ),
            pagingSourceFactory = { RecordingPagingSource(emptyMap()) },
        )

        try {
            advanceUntilIdle()

            screenModel.state.value.itemRefs shouldBe listOf(
                FeedItemRef(7L, EntryType.MANGA),
                FeedItemRef(8L, EntryType.ANIME),
            )
            screenModel.state.value.nextPageKey shouldBe 9L
            screenModel.state.value.hasLoaded shouldBe true
            screenModel.state.value.isRefreshing shouldBe false
        } finally {
            screenModel.onDispose()
        }
    }

    @Test
    fun `refresh reuses one paging source so duplicates across scanned pages are suppressed`() = feedTest {
        val preferences = SourcePreferences(TestPreferenceStore(), testJson)
        val browseFeedService = BrowseFeedService(preferences)
        browseFeedService.saveTimeline(
            FEED_ID,
            SourceFeedTimeline.fromItems(listOf(FeedItemRef(12L, EntryType.MANGA)), nextPageKey = 3L),
        )

        val screenModel = FakeFeedScreenModel(
            feedId = FEED_ID,
            browseFeedService = browseFeedService,
            workerDispatcher = Dispatchers.Main,
            itemsById = mapOf(
                10L to FakeItem(id = 10L, type = EntryType.MANGA, favorite = false),
                11L to FakeItem(id = 11L, type = EntryType.ANIME, favorite = false),
                12L to FakeItem(id = 12L, type = EntryType.MANGA, favorite = false),
            ),
            pagingSourceFactory = {
                RecordingPagingSource(
                    pages = mapOf(
                        null to pageResult(
                            data = listOf(
                                FakeItem(id = 10L, type = EntryType.MANGA, favorite = false),
                                FakeItem(id = 11L, type = EntryType.ANIME, favorite = false),
                                FakeItem(id = 12L, type = EntryType.MANGA, favorite = false),
                            ),
                            nextKey = 1L,
                        ),
                        1L to pageResult(
                            data = listOf(
                                FakeItem(id = 12L, type = EntryType.MANGA, favorite = false),
                                FakeItem(id = 13L, type = EntryType.ANIME, favorite = false),
                            ),
                            nextKey = null,
                        ),
                    ),
                )
            },
        )

        try {
            advanceUntilIdle()
            screenModel.refresh()
            advanceUntilIdle()

            screenModel.state.value.itemRefs shouldBe listOf(
                FeedItemRef(10L, EntryType.MANGA),
                FeedItemRef(11L, EntryType.ANIME),
                FeedItemRef(12L, EntryType.MANGA),
            )
            browseFeedService.timelineSnapshot(FEED_ID) shouldBe SourceFeedTimeline.fromItems(
                listOf(
                    FeedItemRef(10L, EntryType.MANGA),
                    FeedItemRef(11L, EntryType.ANIME),
                    FeedItemRef(12L, EntryType.MANGA),
                ),
                nextPageKey = 1L,
            )
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
) : PagingSource<Long, T>() {

    val loadKeys = mutableListOf<Long?>()

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
        loadKeys += params.key
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

private class TestPreferenceStore : PreferenceStore {
    private val prefs = mutableMapOf<String, Any?>()
    private val cache = mutableMapOf<String, Preference<*>>()

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return preference(key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return preference(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return preference(key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return preference(key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return preference(key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return preference(key, defaultValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return cache.getOrPut(key) {
            val stored = prefs[key]
            val initial = if (stored is String) deserializer(stored) else defaultValue
            TestPreference(
                key = key,
                defaultValue = defaultValue,
                initial = initial,
                prefs = prefs,
                serializer = serializer as (Any?) -> String,
                deserializer = deserializer as (String) -> Any?,
            )
        } as Preference<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> {
        return cache.getOrPut(key) {
            val stored = prefs[key]
            val initial = if (stored is Int) deserializer(stored) else defaultValue
            TestPreference(
                key = key,
                defaultValue = defaultValue,
                initial = initial,
                prefs = prefs,
                serializer = { serializer(it as T).toString() },
                deserializer = { deserializer(it.toInt()) },
            )
        } as Preference<T>
    }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> {
        return preference(key, defaultValue)
    }

    override fun getAll(): Map<String, *> = prefs

    @Suppress("UNCHECKED_CAST")
    private fun <T> preference(key: String, defaultValue: T): Preference<T> {
        return cache.getOrPut(key) {
            TestPreference(key, defaultValue, prefs.getOrPut(key) { defaultValue } as T, prefs)
        } as Preference<T>
    }
}

private class TestPreference<T>(
    private val key: String,
    private val defaultValue: T,
    initial: T,
    private val prefs: MutableMap<String, Any?>,
    private val serializer: (Any?) -> String = { it.toString() },
    private val deserializer: (String) -> Any? = { it },
) : Preference<T> {

    private val flow = MutableStateFlow(initial)

    init {
        prefs[key] = initial
    }

    override fun key(): String = key

    @Suppress("UNCHECKED_CAST")
    override fun get(): T = flow.value

    override fun set(value: T) {
        prefs[key] = serializer(value)
        flow.value = value
    }

    override fun isSet(): Boolean = prefs.containsKey(key)

    override fun delete() {
        prefs.remove(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = flow.asStateFlow()

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = flow.stateIn(scope, SharingStarted.Eagerly, get())
}
