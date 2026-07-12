package eu.kanade.tachiyomi.ui.video.player

import androidx.lifecycle.SavedStateHandle
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlaybackState
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.repository.PlaybackStateRepository
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
        val playbackRepository = FakePlaybackStateRepository(
            existingState = PlaybackState(
                entryId = 1L,
                chapterId = 2L,
                positionMs = 12_345L,
                durationMs = 99_999L,
                completed = false,
                lastWatchedAt = 500L,
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
        playbackRepository.requestedChapterIds shouldBe listOf(2L)
        historyRepository.upserts shouldBe emptyList()
    }

    @Test
    fun `persist playback writes playback state and history delta`() = runTest(dispatcher) {
        val playbackRepository = FakePlaybackStateRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val viewModel = createViewModel(
            entryChapterRepository = FakeEntryChapterRepository(emptyList()),
            playbackRepository = playbackRepository,
            historyRepository = historyRepository,
            resolver = RecordingVideoStreamResolver(),
        )

        viewModel.init(entryId = 1L, chapterId = 2L)
        advanceUntilIdle()

        viewModel.persistPlayback(positionMs = 15_000L, durationMs = 100_000L)
        advanceUntilIdle()

        playbackRepository.upserts.single().chapterId shouldBe 2L
        playbackRepository.upserts.single().positionMs shouldBe 15_000L
        playbackRepository.upserts.single().durationMs shouldBe 100_000L
        playbackRepository.upserts.single().completed shouldBe false
        historyRepository.upserts.single().chapterId shouldBe 2L
        historyRepository.upserts.single().sessionReadDuration shouldBe 15_000L
    }

    @Test
    fun `play next chapter resolves adjacent chapter in source order`() = runTest(dispatcher) {
        val playbackRepository = FakePlaybackStateRepository(existingState = null)
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
        val playbackRepository = FakePlaybackStateRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val resolver = RecordingVideoStreamResolver()
        val getEntryWithChapters = mockk<GetEntryWithChapters>()
        coEvery { getEntryWithChapters.awaitEntry(100L) } returns Entry.create().copy(
            id = 100L,
            title = "Merged",
            url = "/entry/100",
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )
        coEvery { getEntryWithChapters.awaitChapters(id = 100L, bypassMerge = false) } returns listOf(
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
        val playbackRepository = FakePlaybackStateRepository(existingState = null)
        val historyRepository = FakeHistoryRepository()
        val resolver = RecordingVideoStreamResolver()
        val getEntryWithChapters = mockk<GetEntryWithChapters>()
        coEvery { getEntryWithChapters.awaitEntry(1L) } returns Entry.create().copy(
            id = 1L,
            title = "Owner",
            url = "/entry/1",
            chapterFlags = Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SORTING_SOURCE,
        )
        coEvery { getEntryWithChapters.awaitChapters(id = 1L, bypassMerge = true) } returns listOf(
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

    private fun createViewModel(
        entryChapterRepository: EntryChapterRepository,
        playbackRepository: PlaybackStateRepository,
        historyRepository: HistoryRepository,
        resolver: VideoStreamResolver,
        getEntryWithChapters: GetEntryWithChapters? = null,
    ): VideoPlayerViewModel {
        return VideoPlayerViewModel(
            savedState = SavedStateHandle(),
            resolveVideoStream = resolver,
            playbackPreferencesRepository = FakePlaybackPreferencesRepository(),
            entryChapterRepository = entryChapterRepository,
            getEntryWithChapters = getEntryWithChapters,
            playbackStateRepository = playbackRepository,
            historyRepository = historyRepository,
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
            source = 99L,
            title = "Entry $id",
            initialized = true,
            url = "/entry/$id",
        )
    }

    private inner class RecordingVideoStreamResolver : VideoStreamResolver {
        val requests = mutableListOf<Long>()

        override suspend fun invoke(
            entryId: Long,
            chapterId: Long,
            ownerEntryId: Long,
            selection: PlaybackSelection?,
        ): ResolveVideoStream.Result {
            requests += chapterId
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
                    selection = selection ?: PlaybackSelection(),
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
        private val existing: PlaybackPreferences? = null,
    ) : PlaybackPreferencesRepository {
        override suspend fun getByEntryId(entryId: Long): PlaybackPreferences? = existing?.takeIf {
            it.entryId == entryId
        }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<PlaybackPreferences?> = flowOf(
            existing?.takeIf { it.entryId == entryId },
        )

        override suspend fun upsert(preferences: PlaybackPreferences) = Unit
    }

    private class FakePlaybackStateRepository(
        private val existingState: PlaybackState?,
    ) : PlaybackStateRepository {
        val requestedChapterIds = mutableListOf<Long>()
        val upserts = mutableListOf<PlaybackState>()

        override suspend fun getByChapterId(chapterId: Long): PlaybackState? {
            requestedChapterIds += chapterId
            return existingState?.takeIf { it.chapterId == chapterId }
        }

        override fun getByChapterIdAsFlow(chapterId: Long): Flow<PlaybackState?> = emptyFlow()

        override fun getByEntryIdAsFlow(entryId: Long): Flow<List<PlaybackState>> = flowOf(
            existingState?.takeIf { it.entryId == entryId }?.let(::listOf) ?: emptyList(),
        )

        override suspend fun upsert(state: PlaybackState) {
            upserts += state
        }

        override suspend fun upsertAndSyncEpisodeState(state: PlaybackState) {
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
