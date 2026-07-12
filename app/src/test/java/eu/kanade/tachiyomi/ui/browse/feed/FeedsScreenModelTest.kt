package eu.kanade.tachiyomi.ui.browse.feed

import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.service.BrowseFeedService
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Source

class FeedsScreenModelTest {

    @Test
    fun `missing source makes feed invalid after sources load`() {
        val state = FeedsScreenModel.State(
            presets = listOf(
                SourceFeedPreset(id = "preset", sourceId = 1L, name = "Custom", listingMode = FeedListingMode.Search),
            ).toImmutableListForTest(),
            feeds = listOf(
                SourceFeed(id = "feed", sourceId = 2L, presetId = BUILTIN_POPULAR_PRESET_ID),
            ).toImmutableListForTest(),
            sourcesLoaded = true,
        )

        state.isFeedValid(state.feeds.first()) shouldBe false
        state.validFeeds shouldBe emptyList<SourceFeed>().toImmutableListForTest()
    }

    @Test
    fun `latest feed becomes invalid when source no longer supports latest`() {
        val state = FeedsScreenModel.State(
            sources = listOf(
                Source(id = 1L, lang = "en", name = "Source", supportsLatest = false, isStub = false),
            ).toImmutableListForTest(),
            feeds = listOf(
                SourceFeed(id = "feed", sourceId = 1L, presetId = BUILTIN_LATEST_PRESET_ID),
            ).toImmutableListForTest(),
            sourcesLoaded = true,
        )

        state.isFeedValid(state.feeds.first()) shouldBe false
    }

    @Test
    fun `custom preset remains valid only for matching source`() {
        val state = FeedsScreenModel.State(
            sources = listOf(
                Source(id = 1L, lang = "en", name = "Source", supportsLatest = true, isStub = false),
            ).toImmutableListForTest(),
            presets = listOf(
                SourceFeedPreset(id = "preset", sourceId = 2L, name = "Custom", listingMode = FeedListingMode.Search),
            ).toImmutableListForTest(),
            feeds = listOf(SourceFeed(id = "feed", sourceId = 1L, presetId = "preset")).toImmutableListForTest(),
            sourcesLoaded = true,
        )

        state.isFeedValid(state.feeds.first()) shouldBe false
    }

    @Test
    fun `profile switch waits for matching sources before emitting feed state`() = runTest {
        val activeProfileIdFlow = MutableStateFlow(1L)
        val sourcesLoaded = MutableStateFlow(true)
        val sourcesByProfile = mapOf(
            1L to MutableSharedFlow<List<Source>>(replay = 1),
            2L to MutableSharedFlow<List<Source>>(replay = 1),
        )
        val browseStateByProfile = mapOf(
            1L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
            2L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
        )
        val states = mutableListOf<FeedsScreenModel.State>()

        val job = launch {
            observeProfileAwareFeedState(
                activeProfileIdFlow = activeProfileIdFlow,
                enabledSources = { sourcesByProfile.getValue(it) },
                browseState = { browseStateByProfile.getValue(it) },
                sourcesLoaded = sourcesLoaded,
            ).toList(states)
        }

        sourcesByProfile.getValue(1L).emit(
            listOf(Source(id = 1L, lang = "en", name = "Source 1", supportsLatest = true, isStub = false)),
        )
        browseStateByProfile.getValue(1L).emit(
            BrowseFeedService.State(
                presets = emptyList(),
                feeds = listOf(SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID)),
                selectedFeedId = "feed-1",
            ),
        )
        advanceUntilIdle()

        states.last().profileId shouldBe 1L
        states.last().selectedFeedId shouldBe "feed-1"
        states.last().sources.map(Source::id) shouldBe listOf(1L)

        activeProfileIdFlow.value = 2L
        browseStateByProfile.getValue(2L).emit(
            BrowseFeedService.State(
                presets = emptyList(),
                feeds = listOf(SourceFeed(id = "feed-2", sourceId = 2L, presetId = BUILTIN_POPULAR_PRESET_ID)),
                selectedFeedId = "feed-2",
            ),
        )
        advanceUntilIdle()

        states.size shouldBe 1

        sourcesByProfile.getValue(2L).emit(
            listOf(Source(id = 2L, lang = "en", name = "Source 2", supportsLatest = true, isStub = false)),
        )
        advanceUntilIdle()

        states.last().profileId shouldBe 2L
        states.last().selectedFeedId shouldBe "feed-2"
        states.last().sources.map(Source::id) shouldBe listOf(2L)

        job.cancel()
    }

    @Test
    fun `profile switch clears selected feed until matching profile data arrives`() = runTest {
        val activeProfileIdFlow = MutableStateFlow(1L)
        val sourcesLoaded = MutableStateFlow(true)
        val sourcesByProfile = mapOf(
            1L to MutableSharedFlow<List<Source>>(replay = 1),
            2L to MutableSharedFlow<List<Source>>(replay = 1),
        )
        val browseStateByProfile = mapOf(
            1L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
            2L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
        )
        val states = mutableListOf<FeedsScreenModel.State>()

        val job = launch {
            observeProfileAwareFeedState(
                activeProfileIdFlow = activeProfileIdFlow,
                enabledSources = { sourcesByProfile.getValue(it) },
                browseState = { browseStateByProfile.getValue(it) },
                sourcesLoaded = sourcesLoaded,
            ).toList(states)
        }

        sourcesByProfile.getValue(1L).emit(
            listOf(Source(id = 1L, lang = "en", name = "Source 1", supportsLatest = true, isStub = false)),
        )
        browseStateByProfile.getValue(1L).emit(
            BrowseFeedService.State(
                presets = emptyList(),
                feeds = listOf(SourceFeed(id = "feed-1", sourceId = 1L, presetId = BUILTIN_POPULAR_PRESET_ID)),
                selectedFeedId = "feed-1",
            ),
        )
        advanceUntilIdle()

        states.last().selectedFeedId shouldBe "feed-1"

        activeProfileIdFlow.value = 2L
        advanceUntilIdle()

        states.last().selectedFeedId shouldBe "feed-1"

        sourcesByProfile.getValue(2L).emit(
            listOf(Source(id = 2L, lang = "en", name = "Source 2", supportsLatest = true, isStub = false)),
        )
        browseStateByProfile.getValue(2L).emit(
            BrowseFeedService.State(
                presets = emptyList(),
                feeds = listOf(SourceFeed(id = "feed-2", sourceId = 2L, presetId = BUILTIN_POPULAR_PRESET_ID)),
                selectedFeedId = "feed-2",
            ),
        )
        advanceUntilIdle()

        states.last().selectedFeedId shouldBe "feed-2"

        job.cancel()
    }

    @Test
    fun `unified feed state includes all feeds and presets regardless of legacy kind`() = runTest {
        val activeProfileIdFlow = MutableStateFlow(1L)
        val sourcesLoaded = MutableStateFlow(true)
        val sourcesByProfile = mapOf(
            1L to MutableSharedFlow<List<Source>>(replay = 1),
        )
        val browseStateByProfile = mapOf(
            1L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
        )
        val states = mutableListOf<FeedsScreenModel.State>()

        val job = launch {
            observeProfileAwareFeedState(
                activeProfileIdFlow = activeProfileIdFlow,
                enabledSources = { sourcesByProfile.getValue(it) },
                browseState = { browseStateByProfile.getValue(it) },
                sourcesLoaded = sourcesLoaded,
            ).toList(states)
        }

        sourcesByProfile.getValue(1L).emit(
            listOf(Source(id = 1L, lang = "en", name = "Mixed Source", supportsLatest = true, isStub = false)),
        )
        browseStateByProfile.getValue(1L).emit(
            BrowseFeedService.State(
                presets = listOf(
                    SourceFeedPreset(
                        id = "preset-a",
                        sourceId = 1L,
                        name = "Preset A",
                        listingMode = FeedListingMode.Search,
                    ),
                    SourceFeedPreset(
                        id = "preset-b",
                        sourceId = 1L,
                        name = "Preset B",
                        listingMode = FeedListingMode.Search,
                    ),
                ),
                feeds = listOf(
                    SourceFeed(
                        id = "feed-a",
                        sourceId = 1L,
                        presetId = BUILTIN_POPULAR_PRESET_ID,
                    ),
                    SourceFeed(
                        id = "feed-b",
                        sourceId = 1L,
                        presetId = BUILTIN_POPULAR_PRESET_ID,
                    ),
                ),
                selectedFeedId = "feed-a",
            ),
        )
        advanceUntilIdle()

        states.last().feeds.map(SourceFeed::id) shouldBe listOf("feed-a", "feed-b")
        states.last().presets.map(SourceFeedPreset::id) shouldBe listOf("preset-a", "preset-b")

        job.cancel()
    }

    @Test
    fun `video feed state ignores regular feeds and uses video selection`() = runTest {
        val activeProfileIdFlow = MutableStateFlow(1L)
        val sourcesLoaded = MutableStateFlow(true)
        val sourcesByProfile = mapOf(
            1L to MutableSharedFlow<List<Source>>(replay = 1),
        )
        val browseStateByProfile = mapOf(
            1L to MutableSharedFlow<BrowseFeedService.State>(replay = 1),
        )
        val states = mutableListOf<FeedsScreenModel.State>()

        val job = launch {
            observeProfileAwareFeedState(
                activeProfileIdFlow = activeProfileIdFlow,
                enabledSources = { sourcesByProfile.getValue(it) },
                browseState = { browseStateByProfile.getValue(it) },
                sourcesLoaded = sourcesLoaded,
                contentMode = SourceFeedContentMode.Video,
            ).toList(states)
        }

        sourcesByProfile.getValue(1L).emit(
            listOf(Source(id = 1L, lang = "en", name = "Anime Source", supportsLatest = true, isStub = false)),
        )
        browseStateByProfile.getValue(1L).emit(
            BrowseFeedService.State(
                presets = emptyList(),
                feeds = listOf(
                    SourceFeed(
                        id = "regular-feed",
                        contentMode = SourceFeedContentMode.Browse,
                        sourceId = 1L,
                        presetId = BUILTIN_POPULAR_PRESET_ID,
                    ),
                    SourceFeed(
                        id = "video-feed",
                        contentMode = SourceFeedContentMode.Video,
                        sourceId = 1L,
                        presetId = BUILTIN_POPULAR_PRESET_ID,
                    ),
                ),
                selectedFeedId = "regular-feed",
                selectedVideoFeedId = "video-feed",
            ),
        )
        advanceUntilIdle()

        states.last().feeds.map(SourceFeed::id) shouldBe listOf("video-feed")
        states.last().selectedFeedId shouldBe "video-feed"

        job.cancel()
    }
}

private fun <T> List<T>.toImmutableListForTest() = toImmutableList()
