package eu.kanade.domain.source.service

import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BrowseFeedServiceTest {

    private val preferences = SourcePreferences(
        preferenceStore = TestPreferenceStore(),
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )
    private val service = BrowseFeedService(preferences)

    @Test
    fun `reorderFeed persists reordered feeds`() {
        val feedOne = SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID)
        val feedTwo = SourceFeed(id = "feed-2", sourceId = 2L, presetId = BUILTIN_POPULAR_PRESET_ID)
        val feedThree = SourceFeed(id = "feed-3", sourceId = 3L, presetId = BUILTIN_POPULAR_PRESET_ID)

        preferences.savedFeeds.set(listOf(feedOne, feedTwo, feedThree))

        service.reorderFeed(fromIndex = 2, toIndex = 0)

        preferences.savedFeeds.get().map(SourceFeed::id) shouldBe listOf("feed-3", "feed-1", "feed-2")
    }

    @Test
    fun `reorderFeed ignores invalid indices`() {
        val original = listOf(
            SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID),
            SourceFeed(id = "feed-2", sourceId = 2L, presetId = BUILTIN_POPULAR_PRESET_ID),
        )
        preferences.savedFeeds.set(original)

        service.reorderFeed(fromIndex = -1, toIndex = 1)
        service.reorderFeed(fromIndex = 0, toIndex = 5)

        preferences.savedFeeds.get() shouldBe original
    }

    @Test
    fun `removeFeed clears persisted timeline and anchor`() {
        val feed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID)
        preferences.savedFeeds.set(listOf(feed))
        service.saveTimeline(
            feed.id,
            SourceFeedTimeline(
                items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
                nextPageKey = 3L,
            ),
        )
        service.saveAnchor(feed.id, SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48))

        service.removeFeed(feed.id)

        service.timelineSnapshot(feed.id) shouldBe SourceFeedTimeline()
        service.anchorSnapshot(feed.id) shouldBe SourceFeedAnchor()
    }

    @Test
    fun `createFeed selects video feeds separately from regular feeds`() {
        val regularFeed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID)
        val videoFeed = SourceFeed(
            id = "feed-2",
            contentMode = SourceFeedContentMode.Video,
            sourceId = 1L,
            presetId = BUILTIN_POPULAR_PRESET_ID,
        )

        service.createFeed(regularFeed)
        service.createFeed(videoFeed)

        preferences.selectedFeedId.get() shouldBe regularFeed.id
        preferences.selectedVideoFeedId.get() shouldBe videoFeed.id
    }

    @Test
    fun `savePreset persists explicit chronological flag`() {
        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Popular style",
                listingMode = FeedListingMode.Search,
                chronological = false,
            ),
        )

        preferences.savedFeedPresets.get().single().chronological shouldBe false
    }

    @Test
    fun `savePreset replaces existing preset with matching id`() {
        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Original",
                listingMode = FeedListingMode.Search,
                chronological = true,
            ),
        )

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Updated",
                listingMode = FeedListingMode.Search,
                chronological = false,
            ),
        )

        preferences.savedFeedPresets.get() shouldBe listOf(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Updated",
                listingMode = FeedListingMode.Search,
                chronological = false,
            ),
        )
    }

    @Test
    fun `savePreset keeps timelines for name only updates`() {
        val feed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = "preset")
        preferences.savedFeeds.set(listOf(feed))
        service.saveTimeline(
            feed.id,
            SourceFeedTimeline(
                items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
                nextPageKey = 3L,
            ),
        )
        service.saveAnchor(feed.id, SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48))

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Original",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "frieren",
            ),
        )

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Renamed",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "frieren",
            ),
        )

        service.timelineSnapshot(feed.id) shouldBe SourceFeedTimeline(
            items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
            nextPageKey = 3L,
        )
        service.anchorSnapshot(feed.id) shouldBe
            SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48)
    }

    @Test
    fun `savePreset clears timelines only for affected feeds when preset behavior changes`() {
        val updatedFeed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = "preset")
        val untouchedFeed = SourceFeed(id = "feed-2", sourceId = 1L, presetId = "other")
        preferences.savedFeeds.set(listOf(updatedFeed, untouchedFeed))
        service.saveTimeline(
            updatedFeed.id,
            SourceFeedTimeline(
                items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
                nextPageKey = 3L,
            ),
        )
        service.saveAnchor(updatedFeed.id, SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48))
        service.saveTimeline(
            untouchedFeed.id,
            SourceFeedTimeline(
                items = listOf(FeedItemRef(4L, EntryType.MANGA), FeedItemRef(5L, EntryType.MANGA)),
                nextPageKey = 6L,
            ),
        )
        service.saveAnchor(
            untouchedFeed.id,
            SourceFeedAnchor(item = FeedItemRef(5L, EntryType.MANGA), scrollOffset = 12),
        )

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Tracked",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "frieren",
            ),
        )
        service.savePreset(
            SourceFeedPreset(
                id = "other",
                sourceId = 1L,
                name = "Other",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "dungeon",
            ),
        )

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Tracked",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "apothecary",
            ),
        )

        service.timelineSnapshot(updatedFeed.id) shouldBe SourceFeedTimeline()
        service.anchorSnapshot(updatedFeed.id) shouldBe SourceFeedAnchor()
        service.timelineSnapshot(untouchedFeed.id) shouldBe SourceFeedTimeline(
            items = listOf(FeedItemRef(4L, EntryType.MANGA), FeedItemRef(5L, EntryType.MANGA)),
            nextPageKey = 6L,
        )
        service.anchorSnapshot(untouchedFeed.id) shouldBe
            SourceFeedAnchor(item = FeedItemRef(5L, EntryType.MANGA), scrollOffset = 12)
    }

    @Test
    fun `savePreset clears timeline when chronological behavior changes`() {
        val feed = SourceFeed(id = "feed-1", sourceId = 1L, presetId = "preset")
        preferences.savedFeeds.set(listOf(feed))
        service.saveTimeline(
            feed.id,
            SourceFeedTimeline(
                items = listOf(FeedItemRef(1L, EntryType.MANGA), FeedItemRef(2L, EntryType.MANGA)),
                nextPageKey = 3L,
            ),
        )
        service.saveAnchor(feed.id, SourceFeedAnchor(item = FeedItemRef(2L, EntryType.MANGA), scrollOffset = 48))

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Tracked",
                listingMode = FeedListingMode.Search,
                chronological = true,
                query = "frieren",
            ),
        )

        service.savePreset(
            SourceFeedPreset(
                id = "preset",
                sourceId = 1L,
                name = "Tracked",
                listingMode = FeedListingMode.Search,
                chronological = false,
                query = "frieren",
            ),
        )

        service.timelineSnapshot(feed.id) shouldBe SourceFeedTimeline()
        service.anchorSnapshot(feed.id) shouldBe SourceFeedAnchor()
    }
}

private class TestPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, TestPreference<*>>()

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

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return preference(key, defaultValue)
    }

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> {
        return preference(key, defaultValue)
    }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> {
        return preference(key, defaultValue)
    }

    override fun getAll(): Map<String, *> {
        return values.mapValues { it.value.get() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> preference(key: String, defaultValue: T): TestPreference<T> {
        return values.getOrPut(key) { TestPreference(key, defaultValue) } as TestPreference<T>
    }
}

private class TestPreference<T>(
    private val preferenceKey: String,
    private val initialDefault: T,
) : Preference<T> {
    private val state = MutableStateFlow<T?>(null)

    override fun key(): String = preferenceKey

    override fun get(): T = state.value ?: initialDefault

    override fun set(value: T) {
        state.value = value
    }

    override fun isSet(): Boolean = state.value != null

    override fun delete() {
        state.value = null
    }

    override fun defaultValue(): T = initialDefault

    override fun changes(): Flow<T> {
        return state.asStateFlow().map { it ?: initialDefault }
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, get())
    }
}
