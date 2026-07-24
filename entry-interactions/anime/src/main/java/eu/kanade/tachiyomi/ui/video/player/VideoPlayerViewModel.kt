package eu.kanade.tachiyomi.ui.video.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryMediaSessionActivity
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.anime.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.positionMs
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildWindow
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.sortedForReading
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val PRELOAD_MAX_AGE_MS = 30_000L

internal class VideoPlayerViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val resolveVideoStream: VideoStreamResolver = Injekt.get<ResolveVideoStream>(),
    private val playbackPreferencesRepository: PlaybackPreferencesRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val getEntryWithChapters: GetEntryWithChapters? = runCatching {
        Injekt.get<GetEntryWithChapters>()
    }.getOrNull(),
    private val entryRepository: EntryRepository? = runCatching { Injekt.get<EntryRepository>() }.getOrNull(),
    private val entryProgressRepository: EntryProgressRepository = Injekt.get(),
    private val mediaSession: AnimeMediaSessionProcessor = Injekt.get(),
    private val resolveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    private var initialized = false
    private var playbackSession: VideoPlaybackSession? = null
    private val persistMutex = Mutex()
    private var visibleEntryId: Long = INVALID_ID
    private var ownerEntryId: Long = INVALID_ID
    private var chapterId: Long = INVALID_ID
    private var bypassMerge: Boolean = false
    private var sessionPlaybackSpeed: Float =
        savedState[SESSION_PLAYBACK_SPEED_KEY] ?: DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED
    private var applySelectionJob: Job? = null
    private var previewSelectionJob: Job? = null
    private val selectionResultCache = LinkedHashMap<SelectionCacheKey, ResolveVideoStream.Result.Success>()
    private var nextChapterPreloadJob: Job? = null
    private var nextChapterPreload: PreloadedEpisode? = null

    fun init(
        entryId: Long,
        chapterId: Long,
        ownerEntryId: Long = entryId,
        bypassMerge: Boolean = false,
    ) {
        if (initialized) return
        initialized = true
        this.visibleEntryId = entryId
        this.ownerEntryId = ownerEntryId
        this.chapterId = chapterId
        this.bypassMerge = bypassMerge
        savedState[VIDEO_ID_KEY] = entryId
        savedState[OWNER_VIDEO_ID_KEY] = ownerEntryId
        savedState[EPISODE_ID_KEY] = chapterId
        savedState[BYPASS_MERGE_KEY] = bypassMerge

        viewModelScope.launch {
            resolvePlayback(initial = true)
        }
    }

    fun retry() {
        if (mutableState.value !is State.Error) return
        viewModelScope.launch {
            resolvePlayback(initial = true)
        }
    }

    fun retryCurrentPlayback(positionMs: Long) {
        val current = mutableState.value as? State.Ready ?: return
        viewModelScope.launch {
            resolvePlayback(
                selection = current.playback.persistedSourceSelection,
                preservePositionMs = positionMs.coerceAtLeast(0L),
                showLoading = false,
            )
        }
    }

    fun applySourceSelection(selection: PlaybackSelection) {
        val current = mutableState.value as? State.Ready ?: return
        previewSelectionJob?.cancel()
        applySelectionJob?.cancel()
        applySelectionJob = viewModelScope.launch {
            val preservedSubtitle = current.playback.currentSubtitle
            persistPlaybackPreferences(
                entryId = current.ownerEntryId,
                sourceSelection = selection,
                adaptiveQuality = current.playback.currentAdaptiveQuality,
            )
            if (!isActive) return@launch
            resolvePlayback(
                selection = selection,
                preservePositionMs = current.resumePositionMs,
                showLoading = false,
                requestedSubtitle = preservedSubtitle,
            )
        }
    }

    fun previewSourceSelection(selection: PlaybackSelection) {
        previewSourceSelection(selection, resolveCurrentDub = false)
    }

    fun previewDefaultSourceSelection() {
        previewSourceSelection(PlaybackSelection(), resolveCurrentDub = true)
    }

    private fun previewSourceSelection(
        selection: PlaybackSelection,
        resolveCurrentDub: Boolean,
    ) {
        val current = mutableState.value as? State.Ready ?: return
        if (!resolveCurrentDub && selection.dubKey == current.playback.sourceSelection.dubKey) {
            previewSelectionJob?.cancel()
            mutableState.value = current.copy(
                playback = current.playback.copy(preview = VideoPlaybackPreviewState()),
            )
            return
        }

        val currentPreview = current.playback.preview
        if (currentPreview.selection == selection && currentPreview.isLoading) return

        val cachedPreview = cachedSelectionResult(current.chapterId, selection)
        if (cachedPreview != null) {
            mutableState.value = current.copy(
                playback = current.playback.copy(
                    preview = VideoPlaybackPreviewState(
                        selection = selection,
                        playbackData = cachedPreview.playbackData,
                        subtitles = cachedPreview.subtitles,
                        isLoading = false,
                    ),
                ),
            )
            return
        }

        previewSelectionJob?.cancel()
        mutableState.value = current.copy(
            playback = current.playback.copy(
                preview = VideoPlaybackPreviewState(
                    selection = selection,
                    isLoading = true,
                ),
            ),
        )

        previewSelectionJob = viewModelScope.launch {
            val result = try {
                withContext(resolveDispatcher) {
                    resolveVideoStream(
                        entryId = current.visibleEntryId,
                        chapterId = current.chapterId,
                        ownerEntryId = current.ownerEntryId,
                        selection = selection,
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            }

            val latestState = mutableState.value as? State.Ready ?: return@launch
            val latestPreview = latestState.playback.preview
            if (latestPreview.selection != selection) return@launch

            when (result) {
                is ResolveVideoStream.Result.Success -> {
                    cacheSelectionResult(current.chapterId, selection, result)
                    mutableState.value = latestState.copy(
                        playback = latestState.playback.copy(
                            preview = VideoPlaybackPreviewState(
                                selection = selection,
                                playbackData = result.playbackData,
                                subtitles = result.subtitles,
                                isLoading = false,
                            ),
                        ),
                    )
                }
                is ResolveVideoStream.Result.Error -> {
                    mutableEvents.tryEmit(Event.ShowPreviewMessage(result.reason.toMessage()))
                    mutableState.value = latestState.copy(
                        playback = latestState.playback.copy(
                            preview = VideoPlaybackPreviewState(
                                selection = selection,
                                isLoading = false,
                            ),
                        ),
                    )
                }
            }
        }
    }

    fun selectAdaptiveQuality(preference: VideoAdaptiveQualityPreference) {
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(
            playback = current.playback.copy(currentAdaptiveQuality = preference),
        )
        viewModelScope.launch {
            persistPlaybackPreferences(
                entryId = current.ownerEntryId,
                sourceSelection = current.playback.persistedSourceSelection,
                adaptiveQuality = preference,
            )
        }
    }

    fun updateSubtitleAppearance(appearance: VideoSubtitleAppearance) {
        val current = mutableState.value as? State.Ready ?: return
        val normalizedAppearance = appearance.normalized()
        if (current.playback.subtitleAppearance == normalizedAppearance) return
        mutableState.value = current.copy(
            playback = current.playback.copy(subtitleAppearance = normalizedAppearance),
        )
        viewModelScope.launch {
            persistPlaybackPreferences(
                entryId = current.ownerEntryId,
                sourceSelection = current.playback.persistedSourceSelection,
                adaptiveQuality = current.playback.currentAdaptiveQuality,
                subtitleAppearance = normalizedAppearance,
            )
        }
    }

    fun updateAdaptiveQualities(options: List<VideoAdaptiveQualityOption>) {
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(
            playback = current.playback.copy(adaptiveQualities = options),
        )
    }

    fun updateSessionPlaybackSpeed(speed: Float) {
        val normalizedSpeed = speed.coerceIn(MIN_SESSION_PLAYBACK_SPEED, MAX_SESSION_PLAYBACK_SPEED)
        if (sessionPlaybackSpeed == normalizedSpeed) return
        sessionPlaybackSpeed = normalizedSpeed
        savedState[SESSION_PLAYBACK_SPEED_KEY] = normalizedSpeed
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(
            playback = current.playback.copy(sessionPlaybackSpeed = normalizedSpeed),
        )
    }

    fun resetPlayerSettings() {
        val current = mutableState.value as? State.Ready ?: return
        previewSelectionJob?.cancel()
        applySelectionJob?.cancel()
        sessionPlaybackSpeed = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED
        savedState[SESSION_PLAYBACK_SPEED_KEY] = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED
        mutableState.value = current.copy(
            playback = current.playback.copy(
                sessionPlaybackSpeed = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED,
                currentAdaptiveQuality = VideoAdaptiveQualityPreference.Auto,
                currentSubtitle = VideoPlayerSubtitleSelection.Default,
                preview = VideoPlaybackPreviewState(),
            ),
        )
        applySelectionJob = viewModelScope.launch {
            val defaultSelection = PlaybackSelection()
            persistPlaybackPreferences(
                entryId = current.ownerEntryId,
                sourceSelection = defaultSelection,
                adaptiveQuality = VideoAdaptiveQualityPreference.Auto,
                subtitleKey = null,
                updateSubtitleKey = true,
            )
            if (!isActive) return@launch
            resolvePlayback(
                selection = defaultSelection,
                preservePositionMs = current.resumePositionMs,
                showLoading = false,
                requestedSubtitle = VideoPlayerSubtitleSelection.Default,
            )
        }
    }

    fun updateSubtitleOptions(options: List<VideoPlayerSubtitleOption>) {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.subtitleOptions == options) return
        mutableState.value = current.copy(
            playback = current.playback.copy(subtitleOptions = options),
        )
    }

    fun selectSubtitle(selection: VideoPlayerSubtitleSelection) {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.currentSubtitle != selection) {
            mutableState.value = current.copy(
                playback = current.playback.copy(currentSubtitle = selection),
            )
        }
        viewModelScope.launch {
            persistPlaybackPreferences(
                entryId = current.ownerEntryId,
                sourceSelection = current.playback.persistedSourceSelection,
                adaptiveQuality = current.playback.currentAdaptiveQuality,
                subtitleKey = subtitlePreferenceKey(selection),
                updateSubtitleKey = true,
            )
        }
    }

    fun updateResolvedSubtitle(selection: VideoPlayerSubtitleSelection) {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.currentSubtitle == selection) return
        mutableState.value = current.copy(
            playback = current.playback.copy(currentSubtitle = selection),
        )
    }

    fun persistPlayback(positionMs: Long, durationMs: Long) {
        val current = mutableState.value as? State.Ready ?: return
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val session = playbackSession ?: VideoPlaybackSession(
            current.ownerEntryId,
            current.chapterId,
            current.chapterResourceKey,
        )
            .also { playbackSession = it }
        val snapshot = session.snapshot(positionMs = safePositionMs, durationMs = safeDurationMs)
        mutableState.value = current.copy(
            resumePositionMs = safePositionMs,
            playbackStateByChapterId = current.playbackStateByChapterId + (current.chapterId to snapshot.progressState),
        )

        viewModelScope.launch(persistenceDispatcher) {
            withContext(NonCancellable) {
                persistMutex.withLock {
                    mediaSession.onEvent(
                        EntryMediaSessionEvent.Progressed(
                            visibleEntry = current.entry,
                            child = current.childWindow.current,
                            progress = snapshot.progressState,
                            fraction = if (safeDurationMs > 0L) {
                                safePositionMs.toDouble() / safeDurationMs
                            } else {
                                0.0
                            },
                            activity = snapshot.historyUpdate?.let { history ->
                                EntryMediaSessionActivity(
                                    recordedAtEpochMillis = history.readAt.time,
                                    durationMillis = history.sessionReadDuration,
                                )
                            },
                        ),
                    )
                }
            }
        }
    }

    fun resetPlaybackBaseline(positionMs: Long) {
        playbackSession?.restore(positionMs)
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(resumePositionMs = positionMs.coerceAtLeast(0L))
    }

    fun playPreviousEpisode() {
        val current = mutableState.value as? State.Ready ?: return
        val previousEpisodeId = current.childWindow.adjacent(EntryChildDirection.PREVIOUS)?.id ?: return
        viewModelScope.launch {
            playEpisode(previousEpisodeId)
        }
    }

    fun playNextEpisode() {
        val current = mutableState.value as? State.Ready ?: return
        val nextEpisodeId = current.childWindow.adjacent(EntryChildDirection.NEXT)?.id ?: return
        viewModelScope.launch {
            playEpisode(nextEpisodeId)
        }
    }

    fun preloadNextEpisode() {
        val current = mutableState.value as? State.Ready ?: return
        val nextEpisodeId = current.childWindow.adjacent(EntryChildDirection.NEXT)?.id ?: return
        val selection = current.playback.persistedSourceSelection
        val existingPreload = nextChapterPreload
        if (
            existingPreload != null &&
            existingPreload.key.visibleChapterId == current.visibleEntryId &&
            existingPreload.key.chapterId == nextEpisodeId &&
            existingPreload.key.selection == selection.normalized() &&
            existingPreload.isFresh(now())
        ) {
            return
        }

        nextChapterPreloadJob?.cancel()
        nextChapterPreload = null
        nextChapterPreloadJob = viewModelScope.launch {
            val nextOwnerEntryId = entryChapterRepository.getChapterById(nextEpisodeId)?.entryId ?: current.ownerEntryId
            val preloadKey = PreloadedChapterKey(
                visibleChapterId = current.visibleEntryId,
                ownerChapterId = nextOwnerEntryId,
                chapterId = nextEpisodeId,
                selection = selection.normalized(),
            )
            if (nextChapterPreload?.key == preloadKey) {
                return@launch
            }
            val result = runCatching {
                withContext(resolveDispatcher) {
                    resolveVideoStream(
                        entryId = current.visibleEntryId,
                        chapterId = nextEpisodeId,
                        ownerEntryId = nextOwnerEntryId,
                        selection = selection,
                    )
                }
            }.getOrNull() as? ResolveVideoStream.Result.Success ?: return@launch

            nextChapterPreload = PreloadedEpisode(
                key = preloadKey,
                result = result,
                createdAtMillis = now(),
            )
            cacheSelectionResult(nextEpisodeId, result.playbackData.selection, result)
        }
    }

    fun playEpisode(
        visibleEntryId: Long,
        ownerEntryId: Long,
        episodeId: Long,
    ) {
        viewModelScope.launch {
            this@VideoPlayerViewModel.visibleEntryId = visibleEntryId
            this@VideoPlayerViewModel.ownerEntryId = ownerEntryId
            savedState[VIDEO_ID_KEY] = visibleEntryId
            savedState[OWNER_VIDEO_ID_KEY] = ownerEntryId
            playEpisode(episodeId)
        }
    }

    private suspend fun resolvePlayback(
        selection: PlaybackSelection? = null,
        preservePositionMs: Long? = null,
        initial: Boolean = false,
        showLoading: Boolean = true,
        requestedSubtitle: VideoPlayerSubtitleSelection? = null,
    ) {
        val previousReady = mutableState.value as? State.Ready
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        mutableState.value = if (showLoading || previousReady == null) {
            State.Loading
        } else {
            previousReady.copy(isSourceSwitching = true)
        }

        val targetSelection = selection ?: previousReady?.playback?.persistedSourceSelection
        mutableState.value = when (
            val result = consumeMatchingPreload(
                visibleEntryId = this@VideoPlayerViewModel.visibleEntryId,
                ownerEntryId = this@VideoPlayerViewModel.ownerEntryId,
                chapterId = this@VideoPlayerViewModel.chapterId,
                selection = targetSelection,
            ) ?: withContext(resolveDispatcher) {
                resolveVideoStream(
                    entryId = this@VideoPlayerViewModel.visibleEntryId,
                    chapterId = this@VideoPlayerViewModel.chapterId,
                    ownerEntryId = this@VideoPlayerViewModel.ownerEntryId,
                    selection = targetSelection,
                )
            }
        ) {
            is ResolveVideoStream.Result.Success -> {
                cacheSelectionResult(result.chapter.id, result.playbackData.selection, result)
                buildReadyState(
                    result = result,
                    preservePositionMs = preservePositionMs,
                    preview = VideoPlaybackPreviewState(),
                    isSourceSwitching = false,
                    requestedSubtitle = requestedSubtitle,
                    playbackRevision = (previousReady?.playbackRevision ?: -1L) + 1L,
                )
            }
            is ResolveVideoStream.Result.Error -> {
                if (!showLoading && previousReady != null) {
                    mutableEvents.tryEmit(Event.ShowMessage(result.reason.toMessage()))
                    previousReady.copy(
                        playback = previousReady.playback.copy(preview = VideoPlaybackPreviewState()),
                        isSourceSwitching = false,
                    )
                } else {
                    State.Error(result.reason.toMessage())
                }
            }
        }

        val current = mutableState.value
        if (current is State.Ready) {
            val session = playbackSession?.takeIf { !initial }
                ?: VideoPlaybackSession(
                    current.ownerEntryId,
                    current.chapterId,
                    current.chapterResourceKey,
                )
            session.restore(current.playbackStateByChapterId[current.chapterId])
            session.restore(current.resumePositionMs)
            playbackSession = session
        }
    }

    private suspend fun resolveEpisodeNavigation(
        visibleEntry: Entry,
        ownerEntry: Entry,
        episodeId: Long,
    ): EntryChildWindow<EntryChapter>? {
        val effectiveEntry = if (bypassMerge) ownerEntry else visibleEntry
        val sortedEpisodes = getEntryWithChapters?.let { getEntryWithChapters ->
            val episodes = getEntryWithChapters.awaitChapters(effectiveEntry, bypassMerge = bypassMerge)
            episodes.sortedForReading(effectiveEntry)
        } ?: entryChapterRepository.getChaptersByEntryIdAwait(effectiveEntry.id)
            .sortedBy(EntryChapter::sourceOrder)

        return sortedEpisodes.entryChildWindow(episodeId, EntryChapter::id)
    }

    private suspend fun resolveEpisodeDrawerData(
        entry: Entry,
        ownerEntry: Entry,
    ): EpisodeDrawerData {
        val effectiveEntry = if (bypassMerge) ownerEntry else entry
        val episodes = getEntryWithChapters?.awaitChapters(effectiveEntry, bypassMerge = bypassMerge)
            ?: entryChapterRepository.getChaptersByEntryIdAwait(
                effectiveEntry.id,
            )
        val memberIds = episodes.map(EntryChapter::entryId).distinct()
        val fallbackTitles = buildMap {
            put(entry.id, entry.displayTitle)
            put(this@VideoPlayerViewModel.ownerEntryId, ownerEntry.displayTitle)
        }
        val memberTitleById = memberIds.associateWith { memberId ->
            runCatching { entryRepository?.getEntryById(memberId)?.displayTitle }.getOrNull()
                ?: fallbackTitles[memberId].orEmpty()
        }
        val playbackStateByEpisodeId = memberIds
            .flatMap { memberId -> entryProgressRepository.getByEntryId(memberId) }
            .mapNotNull { state -> state.chapterId?.let { it to state } }
            .toMap()

        return EpisodeDrawerData(
            entry = entry,
            chapters = episodes,
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            playbackStateByChapterId = playbackStateByEpisodeId,
        )
    }

    private suspend fun playEpisode(targetEpisodeId: Long) {
        mutableState.value as? State.Ready ?: return
        applySelectionJob?.cancel()
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        if (nextChapterPreload?.key?.chapterId != targetEpisodeId) {
            nextChapterPreload = null
        }
        clearSelectionResultCache(preserveNextEpisodeId = targetEpisodeId)
        this@VideoPlayerViewModel.ownerEntryId =
            entryChapterRepository.getChapterById(targetEpisodeId)?.entryId ?: this@VideoPlayerViewModel.ownerEntryId
        savedState[OWNER_VIDEO_ID_KEY] = this@VideoPlayerViewModel.ownerEntryId
        savedState[EPISODE_ID_KEY] = targetEpisodeId
        this@VideoPlayerViewModel.chapterId = targetEpisodeId
        playbackSession = null
        resolvePlayback(initial = true)
    }

    private suspend fun buildReadyState(
        result: ResolveVideoStream.Result.Success,
        preservePositionMs: Long?,
        preview: VideoPlaybackPreviewState,
        isSourceSwitching: Boolean,
        requestedSubtitle: VideoPlayerSubtitleSelection? = null,
        playbackRevision: Long,
    ): State.Ready {
        val resumePositionMs = preservePositionMs
            ?: entryProgressRepository.get(result.ownerEntry.id, "", result.chapter.progressResourceKey)?.positionMs
            ?: 0L
        val childWindow = resolveEpisodeNavigation(
            visibleEntry = result.visibleEntry,
            ownerEntry = result.ownerEntry,
            episodeId = result.chapter.id,
        ) ?: EntryChildWindow(current = result.chapter)
        val episodeDrawerData = resolveEpisodeDrawerData(
            entry = result.visibleEntry,
            ownerEntry = result.ownerEntry,
        )
        val playback = buildPlaybackUiState(result.playbackData, result.stream, result.savedPreferences)
            .copy(
                subtitles = result.subtitles,
                currentSubtitle = requestedSubtitle
                    ?.let { resolveSourceSubtitleSelection(it, result.subtitles) }
                    ?: resolvePersistedSubtitleSelection(result.savedPreferences.subtitleKey, result.subtitles),
            )
            .copy(preview = preview)
        return State.Ready(
            visibleEntryId = result.visibleEntry.id,
            ownerEntryId = result.ownerEntry.id,
            chapterId = result.chapter.id,
            chapterResourceKey = result.chapter.progressResourceKey,
            childWindow = childWindow,
            entry = episodeDrawerData.entry,
            allChapters = episodeDrawerData.chapters,
            memberIds = episodeDrawerData.memberIds,
            memberTitleById = episodeDrawerData.memberTitleById,
            playbackStateByChapterId = episodeDrawerData.playbackStateByChapterId,
            sourceAvailable = true,
            chapterTitle = result.visibleEntry.displayTitle,
            chapterName = result.chapter.name,
            streamLabel = playback.currentStreamLabel,
            streamUrl = playback.currentStream.request.url,
            stream = playback.currentStream,
            playback = playback,
            resumePositionMs = resumePositionMs,
            isSourceSwitching = isSourceSwitching,
            playbackRevision = playbackRevision,
        )
    }

    private fun buildPlaybackUiState(
        playbackData: PlaybackDescriptor,
        currentStream: VideoStream,
        savedPreferences: PlaybackPreferences,
    ): VideoPlaybackUiState {
        val streamOptions = playbackData.streams
            .filter { it.request.url.isNotBlank() }
            .map { stream ->
                VideoPlaybackOption(
                    key = stream.key.ifBlank { stream.label.ifBlank { stream.request.url } },
                    label = stream.label.ifBlank { stream.request.url },
                )
            }

        return VideoPlaybackUiState(
            sourceSelection = playbackData.selection.copy(
                streamKey = currentStream.key.ifBlank { currentStream.label.ifBlank { currentStream.request.url } },
            ),
            preferredSourceQualityKey = savedPreferences.sourceQualityKey,
            sessionPlaybackSpeed = sessionPlaybackSpeed,
            currentStream = currentStream,
            subtitles = emptyList(),
            currentStreamLabel = currentStream.label.ifBlank { currentStream.request.url },
            streamOptions = streamOptions,
            playbackData = playbackData,
            currentAdaptiveQuality = savedPreferences.toAdaptiveQualityPreference(),
            subtitleAppearance = savedPreferences.toSubtitleAppearance(),
        )
    }

    private fun cachedSelectionResult(
        episodeId: Long,
        selection: PlaybackSelection,
    ): ResolveVideoStream.Result.Success? {
        return selectionResultCache[SelectionCacheKey(chapterId = episodeId, selection = selection.normalized())]
    }

    private fun cacheSelectionResult(
        episodeId: Long,
        selection: PlaybackSelection,
        result: ResolveVideoStream.Result.Success,
    ) {
        selectionResultCache[SelectionCacheKey(chapterId = episodeId, selection = selection.normalized())] = result
        trimSelectionResultCache()
    }

    private fun trimSelectionResultCache() {
        while (selectionResultCache.size > SELECTION_CACHE_LIMIT) {
            val firstKey = selectionResultCache.entries.firstOrNull()?.key ?: break
            selectionResultCache.remove(firstKey)
        }
    }

    private fun clearSelectionResultCache(preserveNextEpisodeId: Long? = null) {
        if (preserveNextEpisodeId == null) {
            selectionResultCache.clear()
            return
        }

        val preservedEntries = selectionResultCache.filterKeys { it.chapterId == preserveNextEpisodeId }
        selectionResultCache.clear()
        selectionResultCache.putAll(preservedEntries)
    }

    private fun consumeMatchingPreload(
        visibleEntryId: Long,
        ownerEntryId: Long,
        chapterId: Long,
        selection: PlaybackSelection?,
    ): ResolveVideoStream.Result.Success? {
        val preload = nextChapterPreload ?: return null
        if (!preload.isFresh(now())) {
            nextChapterPreload = null
            return null
        }
        val selectionKey = selection?.normalized() ?: preload.key.selection
        return preload
            .takeIf {
                it.key.visibleChapterId == visibleEntryId &&
                    it.key.ownerChapterId == ownerEntryId &&
                    it.key.chapterId == chapterId &&
                    it.key.selection == selectionKey
            }
            ?.also {
                nextChapterPreload = null
            }
            ?.result
    }

    private suspend fun persistPlaybackPreferences(
        entryId: Long,
        sourceSelection: PlaybackSelection? = null,
        adaptiveQuality: VideoAdaptiveQualityPreference? = null,
        subtitleAppearance: VideoSubtitleAppearance? = null,
        subtitleKey: String? = null,
        updateSubtitleKey: Boolean = false,
    ) {
        val existingPreferences = playbackPreferencesRepository.getByEntryId(entryId)
            ?: defaultPlaybackPreferences(entryId)
        val resolvedSourceSelection = sourceSelection ?: PlaybackSelection(
            dubKey = existingPreferences.dubKey,
            streamKey = existingPreferences.streamKey,
            sourceQualityKey = existingPreferences.sourceQualityKey,
        )
        val resolvedAdaptiveQuality = adaptiveQuality ?: existingPreferences.toAdaptiveQualityPreference()
        val resolvedSubtitleAppearance = (subtitleAppearance ?: existingPreferences.toSubtitleAppearance()).normalized()
        val resolvedSubtitleKey = if (updateSubtitleKey) subtitleKey else existingPreferences.subtitleKey
        playbackPreferencesRepository.upsert(
            PlaybackPreferences(
                entryId = entryId,
                dubKey = resolvedSourceSelection.dubKey,
                streamKey = resolvedSourceSelection.streamKey,
                sourceQualityKey = resolvedSourceSelection.sourceQualityKey,
                subtitleKey = resolvedSubtitleKey,
                playerQualityMode = resolvedAdaptiveQuality.toPlayerQualityMode(),
                playerQualityHeight = resolvedAdaptiveQuality.heightOrNull(),
                subtitleOffsetX = resolvedSubtitleAppearance.toPersistedOffsetX(),
                subtitleOffsetY = resolvedSubtitleAppearance.toPersistedOffsetY(),
                subtitleTextSize = resolvedSubtitleAppearance.toPersistedTextSize(),
                subtitleTextColor = resolvedSubtitleAppearance.toPersistedTextColor(),
                subtitleBackgroundColor = resolvedSubtitleAppearance.toPersistedBackgroundColor(),
                subtitleBackgroundOpacity = resolvedSubtitleAppearance.toPersistedBackgroundOpacity(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun defaultPlaybackPreferences(entryId: Long): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = null,
            streamKey = null,
            sourceQualityKey = null,
            subtitleKey = null,
            playerQualityMode = PlayerQualityMode.AUTO,
            playerQualityHeight = null,
            updatedAt = 0L,
        )
    }

    sealed interface State {
        data object Loading : State

        data class Ready(
            val visibleEntryId: Long,
            val ownerEntryId: Long,
            val chapterId: Long,
            val chapterResourceKey: String,
            val childWindow: EntryChildWindow<EntryChapter>,
            val entry: Entry,
            val allChapters: List<EntryChapter>,
            val memberIds: List<Long>,
            val memberTitleById: Map<Long, String>,
            val playbackStateByChapterId: Map<Long, EntryProgressState>,
            val sourceAvailable: Boolean,
            val chapterTitle: String,
            val chapterName: String,
            val streamLabel: String,
            val streamUrl: String,
            val stream: VideoStream,
            val playback: VideoPlaybackUiState,
            val resumePositionMs: Long,
            val isSourceSwitching: Boolean = false,
            val playbackRevision: Long = 0L,
        ) : State {
            val previousChapterId: Long?
                get() = childWindow.previous?.id

            val nextChapterId: Long?
                get() = childWindow.next?.id

            val chapterListItems: List<VideoPlayerEpisodeListEntry>
                get() = buildVideoPlayerEpisodeDisplayData(
                    entry = entry,
                    chapters = allChapters,
                    memberIds = memberIds,
                    memberTitleById = memberTitleById,
                    playbackStates = playbackStateByChapterId.values.toList(),
                ).chapterListItems
        }

        data class Error(val message: String) : State
    }

    sealed interface Event {
        data class ShowMessage(val message: String) : Event

        data class ShowPreviewMessage(val message: String) : Event
    }

    private fun ResolveVideoStream.Reason.toMessage(): String {
        return when (this) {
            ResolveVideoStream.Reason.VideoNotFound -> "Video not found"
            ResolveVideoStream.Reason.EpisodeNotFound -> "Episode not found"
            ResolveVideoStream.Reason.EpisodeMismatch -> "Episode does not belong to the selected video"
            ResolveVideoStream.Reason.SourceLoadTimeout -> "Video source took too long to load"
            ResolveVideoStream.Reason.SourceNotFound -> "Video source not available"
            ResolveVideoStream.Reason.NoStreams -> "No playable streams returned"
            ResolveVideoStream.Reason.StreamFetchTimeout -> "Timed out while resolving streams"
            ResolveVideoStream.Reason.OfflineNoDownload -> "Device is offline and episode is not downloaded"
            is ResolveVideoStream.Reason.StreamFetchFailed -> listOfNotNull(
                cause::class.simpleName,
                cause.message,
            ).joinToString(": ").ifBlank { "Failed to resolve streams" }
        }
    }

    companion object {
        private const val VIDEO_ID_KEY = "video_id"
        private const val OWNER_VIDEO_ID_KEY = "owner_video_id"
        private const val EPISODE_ID_KEY = "episode_id"
        private const val BYPASS_MERGE_KEY = "bypass_merge"
        private const val SESSION_PLAYBACK_SPEED_KEY = "session_playback_speed"
        private const val INVALID_ID = -1L
        private const val SELECTION_CACHE_LIMIT = 12
        private const val MIN_SESSION_PLAYBACK_SPEED = 0.5f
        private const val MAX_SESSION_PLAYBACK_SPEED = 2f
    }
}

private data class EpisodeDrawerData(
    val entry: Entry,
    val chapters: List<EntryChapter>,
    val memberIds: List<Long>,
    val memberTitleById: Map<Long, String>,
    val playbackStateByChapterId: Map<Long, EntryProgressState>,
)

private data class SelectionCacheKey(
    val chapterId: Long,
    val selection: PlaybackSelection,
)

private data class PreloadedEpisode(
    val key: PreloadedChapterKey,
    val result: ResolveVideoStream.Result.Success,
    val createdAtMillis: Long,
)

private fun PreloadedEpisode.isFresh(now: Long): Boolean {
    return now - createdAtMillis <= PRELOAD_MAX_AGE_MS
}

private data class PreloadedChapterKey(
    val visibleChapterId: Long,
    val ownerChapterId: Long,
    val chapterId: Long,
    val selection: PlaybackSelection,
)

private fun PlaybackSelection.normalized(): PlaybackSelection {
    return copy(streamKey = null)
}
