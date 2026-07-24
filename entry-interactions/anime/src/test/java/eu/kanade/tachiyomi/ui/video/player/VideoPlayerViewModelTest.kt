package eu.kanade.tachiyomi.ui.video.player

import androidx.lifecycle.SavedStateHandle
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryMediaSessionResult
import mihon.entry.interactions.anime.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.animeProgressState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `init exposes resume position from saved playback state`() = runTest(dispatcher) {
        val playbackRepository = FakeEntryProgressRepository(
            existingState = animeProgressState(
                entryId = 1L,
                chapterId = 2L,
                resourceKey = "/chapter/2",
                positionMs = 12_345L,
                durationMs = 99_999L,
                completed = false,
                locatorUpdatedAt = 500L,
                completionUpdatedAt = 0L,
            ),
        )
        val historyRepository = FakeHistoryRepository()
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(
                chapters = listOf(
                    chapter(id = 1L, entryId = 1L, sourceOrder = 0L),
                    chapter(id = 2L, entryId = 1L, sourceOrder = 1L),
                    chapter(id = 3L, entryId = 1L, sourceOrder = 2L),
                ),
            ),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = RecordingVideoStreamResolver(),
        )

        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()

        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.chapterId shouldBe 2L
        state.previousChapterId shouldBe 1L
        state.nextChapterId shouldBe 3L
        state.resumePositionMs shouldBe 12_345L
        playbackRepository.requestedResourceKeys shouldBe listOf("/chapter/2")
        historyRepository.upserts shouldBe emptyList()
    }

    @Test
    fun `persist playback writes playback state and history delta`() = runTest(dispatcher) {
        val playbackRepository = FakeEntryProgressRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val events = mutableListOf<EntryMediaSessionEvent>()
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(emptyList()),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = RecordingVideoStreamResolver(),
            mediaSession = EntryMediaSessionEventSink {
                events += it
                EntryMediaSessionResult.Handled
            },
        )

        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()

        viewModel.persistPlayback(positionMs = 15_000L, durationMs = 100_000L)
        advanceUntilIdle()

        playbackRepository.upserts shouldBe emptyList()
        historyRepository.upserts shouldBe emptyList()
        val event = events.single() as EntryMediaSessionEvent.Progressed
        event.progress.chapterId shouldBe 2L
        event.progress.locator.position shouldBe 15_000L
        event.progress.locator.extent shouldBe 100_000L
        event.progress.completed shouldBe false
        event.activity?.durationMillis shouldBe 15_000L
    }

    @Test
    fun `persist playback reports completion to shared media session`() = runTest(dispatcher) {
        val mediaSession = mockk<EntryMediaSessionEventSink>(relaxed = true)
        coEvery { mediaSession.onEvent(any()) } returns EntryMediaSessionResult.Handled
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(emptyList()),
            playbackRepository = FakeEntryProgressRepository(existingState = null),
            historyRepository = FakeHistoryRepository(),
            resolver = RecordingVideoStreamResolver(),
            mediaSession = mediaSession,
        )
        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()

        viewModel.persistPlayback(positionMs = 95_000L, durationMs = 100_000L)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mediaSession.onEvent(
                match {
                    it is EntryMediaSessionEvent.Progressed &&
                        it.visibleEntry == videoEntry(1L) &&
                        it.child == chapter(id = 2L, entryId = 1L, sourceOrder = 2L) &&
                        it.progress.completed
                },
            )
        }
    }

    @Test
    fun `play next chapter resolves adjacent chapter in source order`() = runTest(dispatcher) {
        val playbackRepository = FakeEntryProgressRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val resolver = RecordingVideoStreamResolver()
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(
                chapters = listOf(
                    chapter(id = 20L, entryId = 1L, sourceOrder = 2L),
                    chapter(id = 10L, entryId = 1L, sourceOrder = 1L),
                    chapter(id = 30L, entryId = 1L, sourceOrder = 3L),
                ),
            ),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = resolver,
        )

        viewModel.init(entryId = 1L, chapterId = 10L)
        advanceUntilIdle()
        viewModel.playNextEpisode()
        advanceUntilIdle()

        resolver.requests shouldBe listOf(10L, 20L)
        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.chapterId shouldBe 20L
        state.previousChapterId shouldBe 10L
        state.nextChapterId shouldBe 30L
    }

    @Test
    fun `play next chapter uses merged sequence when bypassMerge is false`() = runTest(dispatcher) {
        val playbackRepository = FakeEntryProgressRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val resolver = RecordingVideoStreamResolver()
        val getEntryWithChapters = mockk<GetEntryWithChapters>()
        coEvery { getEntryWithChapters.awaitChapters(any(), false, any()) } returns listOf(
            chapter(id = 10L, entryId = 1L, sourceOrder = 1L, chapterNumber = 1.0),
            chapter(id = 30L, entryId = 1L, sourceOrder = 2L, chapterNumber = 2.0),
            chapter(id = 20L, entryId = 2L, sourceOrder = 1L, chapterNumber = 1.0),
        )

        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(
                chapters = listOf(
                    chapter(id = 10L, entryId = 1L, sourceOrder = 1L),
                    chapter(id = 20L, entryId = 2L, sourceOrder = 1L),
                    chapter(id = 30L, entryId = 1L, sourceOrder = 2L),
                ),
            ),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = resolver,
            getEntryWithChapters = getEntryWithChapters,
        )

        viewModel.init(entryId = 100L, chapterId = 10L, ownerEntryId = 1L, bypassMerge = false)
        advanceUntilIdle()
        viewModel.playNextEpisode()
        advanceUntilIdle()

        resolver.requests shouldBe listOf(10L, 30L)
        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.chapterId shouldBe 30L
        state.previousChapterId shouldBe 10L
        state.nextChapterId shouldBe null
    }

    @Test
    fun `play next chapter stays on owner sequence when bypassMerge is true`() = runTest(dispatcher) {
        val playbackRepository = FakeEntryProgressRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val resolver = RecordingVideoStreamResolver()
        val getEntryWithChapters = mockk<GetEntryWithChapters>()
        coEvery { getEntryWithChapters.awaitChapters(any(), true, any()) } returns listOf(
            chapter(id = 10L, entryId = 1L, sourceOrder = 2L),
            chapter(id = 30L, entryId = 1L, sourceOrder = 1L),
        )

        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(
                chapters = listOf(
                    chapter(id = 10L, entryId = 1L, sourceOrder = 2L),
                    chapter(id = 20L, entryId = 2L, sourceOrder = 1L),
                    chapter(id = 30L, entryId = 1L, sourceOrder = 1L),
                ),
            ),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = resolver,
            getEntryWithChapters = getEntryWithChapters,
        )

        viewModel.init(entryId = 100L, chapterId = 10L, ownerEntryId = 1L, bypassMerge = true)
        advanceUntilIdle()
        viewModel.playNextEpisode()
        advanceUntilIdle()

        resolver.requests shouldBe listOf(10L, 30L)
        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.chapterId shouldBe 30L
        state.previousChapterId shouldBe 10L
        state.nextChapterId shouldBe null
    }

    @Test
    fun `reset player settings persists defaults and keeps the current position`() = runTest(dispatcher) {
        val preferencesRepository = FakePlaybackPreferencesRepository(
            existing = PlaybackPreferences(
                entryId = 1L,
                dubKey = "dub",
                streamKey = "stream",
                sourceQualityKey = "source-quality",
                subtitleKey = "subtitle",
                playerQualityMode = PlayerQualityMode.SPECIFIC_HEIGHT,
                playerQualityHeight = 720,
                updatedAt = 10L,
            ),
        )
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(emptyList()),
            playbackRepository = FakeEntryProgressRepository(
                existingState = animeProgressState(
                    entryId = 1L,
                    chapterId = 2L,
                    resourceKey = "/chapter/2",
                    positionMs = 12_345L,
                    durationMs = 99_999L,
                    completed = false,
                    locatorUpdatedAt = 500L,
                    completionUpdatedAt = 0L,
                ),
            ),
            historyRepository = FakeHistoryRepository(),
            resolver = RecordingVideoStreamResolver(),
            playbackPreferencesRepository = preferencesRepository,
        )
        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()
        viewModel.updateSessionPlaybackSpeed(1.5f)

        viewModel.resetPlayerSettings()
        advanceUntilIdle()

        preferencesRepository.upserts.last().run {
            dubKey shouldBe null
            streamKey shouldBe null
            sourceQualityKey shouldBe null
            subtitleKey shouldBe null
            playerQualityMode shouldBe PlayerQualityMode.AUTO
            playerQualityHeight shouldBe null
        }
        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.resumePositionMs shouldBe 12_345L
        state.playback.sessionPlaybackSpeed shouldBe DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED
        state.playback.currentAdaptiveQuality shouldBe VideoAdaptiveQualityPreference.Auto
        state.playback.currentSubtitle shouldBe VideoPlayerSubtitleSelection.None
    }

    @Test
    fun `preview defaults resolves them when the active dub is already automatic`() = runTest(dispatcher) {
        val resolvedDefaults = PlaybackSelection(sourceQualityKey = "default-quality")
        val resolver = RecordingVideoStreamResolver(defaultSelection = resolvedDefaults)
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(emptyList()),
            playbackRepository = FakeEntryProgressRepository(existingState = null),
            historyRepository = FakeHistoryRepository(),
            resolver = resolver,
        )
        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()

        viewModel.previewDefaultSourceSelection()
        advanceUntilIdle()

        resolver.selections shouldBe listOf(null, PlaybackSelection())
        val state = viewModel.state.value as VideoPlayerViewModel.State.Ready
        state.playback.preview.playbackData?.selection shouldBe resolvedDefaults
    }

    private fun createViewModel(
        entryChapterRepository: EntryChapterRepository,
        playbackRepository: EntryProgressRepository,
        historyRepository: HistoryRepository,
        resolver: VideoStreamResolver,
        getEntryWithChapters: GetEntryWithChapters? = null,
        playbackPreferencesRepository: PlaybackPreferencesRepository = FakePlaybackPreferencesRepository(),
        mediaSession: EntryMediaSessionEventSink = EntryMediaSessionEventSink {
            EntryMediaSessionResult.Handled
        },
    ): VideoPlayerViewModel {
        return VideoPlayerViewModel(
            savedState = SavedStateHandle(),
            resolveVideoStream = resolver,
            playbackPreferencesRepository = playbackPreferencesRepository,
            entryChapterRepository = entryChapterRepository,
            getEntryWithChapters = getEntryWithChapters,
            entryProgressRepository = playbackRepository,
            mediaSession = AnimeMediaSessionProcessor(mediaSession),
            resolveDispatcher = dispatcher,
            persistenceDispatcher = dispatcher,
        )
    }

    private fun chapter(
        id: Long,
        entryId: Long,
        sourceOrder: Long,
        chapterNumber: Double = sourceOrder.toDouble(),
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "/chapter/$id",
            name = "Chapter $id",
            sourceOrder = sourceOrder,
            chapterNumber = chapterNumber,
        )
    }

    private fun videoEntry(id: Long): Entry {
        return Entry.create().copy(
            id = id,
            type = EntryType.ANIME,
            source = 99L,
            title = "Entry $id",
            initialized = true,
            url = "/entry/$id",
            chapterFlags = when (id) {
                100L -> Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER
                else -> Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SORTING_SOURCE
            },
        )
    }

    private inner class RecordingVideoStreamResolver(
        private val defaultSelection: PlaybackSelection = PlaybackSelection(),
    ) : VideoStreamResolver {
        val requests = mutableListOf<Long>()
        val selections = mutableListOf<PlaybackSelection?>()

        override suspend fun invoke(
            entryId: Long,
            chapterId: Long,
            ownerEntryId: Long,
            selection: PlaybackSelection?,
        ): ResolveVideoStream.Result {
            requests += chapterId
            selections += selection
            val stream = VideoStream(
                request = VideoRequest(url = "https://cdn.example.com/$chapterId.m3u8"),
                label = "Auto",
                type = VideoStreamType.HLS,
            )
            return ResolveVideoStream.Result.Success(
                visibleEntry = videoEntry(entryId),
                ownerEntry = videoEntry(ownerEntryId),
                chapter = chapter(id = chapterId, entryId = ownerEntryId, sourceOrder = chapterId),
                playbackData = PlaybackDescriptor(
                    selection = selection
                        ?.takeUnless { it == PlaybackSelection() }
                        ?: defaultSelection,
                    streams = listOf(stream),
                ),
                stream = stream,
                subtitles = emptyList(),
                savedPreferences = PlaybackPreferences(
                    entryId = ownerEntryId,
                    dubKey = null,
                    streamKey = null,
                    sourceQualityKey = null,
                    subtitleKey = null,
                    playerQualityMode = PlayerQualityMode.AUTO,
                    playerQualityHeight = null,
                    updatedAt = 0L,
                ),
            )
        }
    }

    private class FakePlaybackPreferencesRepository(
        existing: PlaybackPreferences? = null,
    ) : PlaybackPreferencesRepository {
        private var current = existing
        val upserts = mutableListOf<PlaybackPreferences>()

        override suspend fun getByEntryId(entryId: Long): PlaybackPreferences? = current?.takeIf {
            it.entryId == entryId
        }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<PlaybackPreferences?> = flowOf(
            current?.takeIf { it.entryId == entryId },
        )

        override suspend fun upsert(preferences: PlaybackPreferences) {
            current = preferences
            upserts += preferences
        }
    }

    private class FakeEntryProgressRepository(
        existingState: EntryProgressState?,
    ) : EntryProgressRepository {
        private val states = existingState?.let(::mutableListOf) ?: mutableListOf()
        val requestedResourceKeys = mutableListOf<String>()
        val upserts = mutableListOf<EntryProgressState>()

        override suspend fun get(entryId: Long, contentKey: String, resourceKey: String): EntryProgressState? {
            requestedResourceKeys += resourceKey
            return states.firstOrNull {
                it.entryId == entryId && it.contentKey == contentKey && it.resourceKey == resourceKey
            }
        }

        override suspend fun getByEntryId(entryId: Long): List<EntryProgressState> =
            states.filter { it.entryId == entryId }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.entryId == entryId })

        override fun getByChapterIdAsFlow(chapterId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.chapterId == chapterId })

        override suspend fun upsert(state: EntryProgressState) = record(state)

        override suspend fun upsertAndSyncChild(state: EntryProgressState) = record(state)

        override suspend fun merge(state: EntryProgressState): EntryProgressState = state.also(::record)

        override suspend fun mergeAndSyncChild(state: EntryProgressState): EntryProgressState = state.also(::record)

        override suspend fun rekey(
            entryId: Long,
            chapterId: Long?,
            oldContentKey: String,
            oldResourceKey: String,
            newContentKey: String,
            newResourceKey: String,
        ) = Unit

        private fun record(state: EntryProgressState) {
            states.removeAll { it.identity == state.identity }
            states += state
            upserts += state
        }
    }

    private class FakeHistoryRepository : HistoryRepository {
        val upserts = mutableListOf<HistoryUpdate>()

        override fun getHistory(query: String): Flow<List<HistoryWithRelations>> = emptyFlow()

        override suspend fun getLastHistory(): HistoryWithRelations? = null

        override suspend fun getTotalReadDuration(): Long = 0L

        override suspend fun getHistoryByEntryId(entryId: Long): List<History> = emptyList()

        override suspend fun resetHistory(historyId: Long) = Unit

        override suspend fun resetHistoryByEntryId(entryId: Long) = Unit

        override suspend fun deleteAllHistory(): Boolean = false

        override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
            upserts += historyUpdate
        }
    }

    private class FakeEntryChapterRepository(
        private val chapters: List<EntryChapter>,
    ) : EntryChapterRepository {
        private val chaptersById = chapters.associateBy(EntryChapter::id)

        override suspend fun getChapterById(id: Long): EntryChapter? = chaptersById[id]

        override fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>> = flowOf(
            chapters.filter { it.entryId == entryId },
        )

        override fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>> = flowOf(
            chapters.filter { it.entryId in entryIds },
        )

        override suspend fun getChaptersByEntryIdAwait(
            entryId: Long,
            applyScanlatorFilter: Boolean,
        ): List<EntryChapter> {
            return chapters.filter { it.entryId == entryId }
        }

        override suspend fun getRecentRead(offset: Int, limit: Int): List<EntryChapter> = emptyList()

        override suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter> = emptyList()

        override suspend fun insert(chapter: EntryChapter): Long = chapter.id

        override suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter> = chapters

        override suspend fun update(chapter: EntryChapter): Boolean = true

        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean = true

        override suspend fun delete(id: Long): Boolean = true

        override suspend fun deleteByEntryId(entryId: Long): Boolean = true

        override suspend fun removeChaptersWithIds(chapterIds: List<Long>) = Unit

        override suspend fun getScanlatorsByEntryId(entryId: Long): List<String> = emptyList()

        override fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>> = emptyFlow()

        override suspend fun getChapterByUrlAndEntryId(
            url: String,
            entryId: Long,
        ): EntryChapter? = chapters.firstOrNull {
            it.url == url && it.entryId == entryId
        }
    }
}
