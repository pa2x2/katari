package eu.kanade.tachiyomi.ui.browse.feed

import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.service.BrowseFeedService
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.Source

class FeedsScreenModelTest {

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
            listOf(source(id = 1L, name = "Source 1")),
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
            listOf(source(id = 2L, name = "Source 2")),
        )
        advanceUntilIdle()

        states.last().profileId shouldBe 2L
        states.last().selectedFeedId shouldBe "feed-2"
        states.last().sources.map(Source::id) shouldBe listOf(2L)

        job.cancel()
    }
}

private fun source(id: Long, name: String): Source {
    return Source(
        id = id,
        lang = "en",
        name = name,
        catalogue = EntryCatalogueDescription(supportsLatest = true),
        isStub = false,
    )
}
