package eu.kanade.tachiyomi.ui.video.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngineRun
import eu.kanade.tachiyomi.ui.video.player.tracks.VideoPlayerAudioTrackOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildWindow
import mihon.video.engine.spi.loading.resolvedRequest
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.MergedMemberExpansionSession
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.domain.source.service.SourceInfoNames

private const val PRELOAD_MAX_AGE_MS = 30_000L

internal class VideoPlayerViewModel(
    private val savedState: SavedStateHandle,
    private val resolveVideoStream: VideoStreamResolver,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryRepository: EntryRepository,
    private val sourceNames: SourceInfoNames,
    mergedMemberExpansionRepository: MergedMemberExpansionRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val mediaSession: AnimeMediaSessionProcessor,
    private val webViewFeature: EntryWebViewFeature,
    private val resolveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Loading())
    val state = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    private val memberExpansion = MergedMemberExpansionSession(
        repository = mergedMemberExpansionRepository,
        scope = viewModelScope,
        memberIds = mutableState.filterIsInstance<State.Ready>().map { it.memberIds },
    )

    /** Merged members whose episodes the drawer lists; the playing member opens until it is toggled. */
    val expandedMemberIds: StateFlow<Set<Long>> = combine(
        memberExpansion.state,
        mutableState.filterIsInstance<State.Ready>().map { it.ownerEntryId },
    ) { expansion, playingMemberId -> expansion.shown(playingMemberId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleMemberExpanded(memberId: Long) {
        val ready = mutableState.value as? State.Ready ?: return
        memberExpansion.toggle(memberId, ready.ownerEntryId)
    }

    private var initialized = false
    private var playbackSession: VideoPlaybackSession? = null
    private val activitySession = EntryMediaSessionActivitySession()
    private val persistMutex = Mutex()
    private val preferencesMutex = Mutex()
    private var visibleEntryId: Long = INVALID_ID
    private var ownerEntryId: Long = INVALID_ID
    private var chapterId: Long = INVALID_ID
    private var bypassMerge: Boolean = false
    private var sessionPlaybackSpeed: Float =
        savedState[SESSION_PLAYBACK_SPEED_KEY] ?: DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED
    private var defaultPlaybackSpeed: Float = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED

    // Carried into each episode the session opens, where the player looks for a track with the same key.
    private var sessionAudioTrackKey: String? = null
    private var applySelectionJob: Job? = null
    private var previewSelectionJob: Job? = null
    private val selectionResultCache = LinkedHashMap<SelectionCacheKey, ResolveVideoStream.Result.Success>()
    private var nextChapterPreloadJob: Job? = null
    private var nextChapterPreload: PreloadedEpisode? = null
    private var loadingState = State.Loading()

    // Auto's move to mpv, carried into the streams and episodes opened after it so each doesn't fail first again.
    private var sessionEngineRun: VideoPlayerEngineRun? = null

    fun init(
        entryId: Long,
        chapterId: Long,
        ownerEntryId: Long = entryId,
        bypassMerge: Boolean = false,
        defaultPlaybackSpeed: Float = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED,
    ) {
        if (initialized) return
        initialized = true
        this.defaultPlaybackSpeed = defaultPlaybackSpeed
        // A speed saved across process death outranks the default the player starts with.
        if (savedState.get<Float>(SESSION_PLAYBACK_SPEED_KEY) == null) sessionPlaybackSpeed = defaultPlaybackSpeed
        this.visibleEntryId = entryId
        this.ownerEntryId = ownerEntryId
        this.chapterId = chapterId
        this.bypassMerge = bypassMerge
        savedState[VIDEO_ID_KEY] = entryId
        savedState[OWNER_VIDEO_ID_KEY] = ownerEntryId
        savedState[EPISODE_ID_KEY] = chapterId
        savedState[BYPASS_MERGE_KEY] = bypassMerge

        viewModelScope.launch {
            val chapter = runCatching { entryChapterRepository.getChapterById(chapterId) }.getOrNull()
            loadingState = State.Loading(
                // The episode's own entry, so a merged entry names the member that's playing.
                title = runCatching { entryRepository.getEntryById(chapter?.entryId ?: ownerEntryId)?.displayTitle }
                    .getOrNull(),
                episodeName = chapter?.name,
            )
            if (mutableState.value is State.Loading) mutableState.value = loadingState
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
        val current = mutableState.value as? State.Ready ?: return
        if (selection.dubKey == current.playback.sourceSelection.dubKey) {
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
                        subtitles = cachedPreview.stream.subtitles,
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
                                subtitles = result.stream.subtitles,
                                isLoading = false,
                            ),
                        ),
                    )
                }
                is ResolveVideoStream.Result.Error -> {
                    mutableEvents.tryEmit(Event.ShowProblem(result.reason.toProblem()))
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
        sessionPlaybackSpeed = defaultPlaybackSpeed
        savedState[SESSION_PLAYBACK_SPEED_KEY] = defaultPlaybackSpeed
        sessionAudioTrackKey = null
        mutableState.value = current.copy(
            playback = current.playback.copy(
                sessionPlaybackSpeed = defaultPlaybackSpeed,
                currentAudioTrackKey = null,
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

    /** Plays on from [positionMs] with a new player in [run]'s engine. */
    fun switchEngine(run: VideoPlayerEngineRun, positionMs: Long) {
        val current = mutableState.value as? State.Ready ?: return
        rememberEngineRun(run, current.playback.engineOverride)
        mutableState.value = current.copy(
            engineRun = run,
            resumePositionMs = positionMs.coerceAtLeast(0L),
            playbackRevision = current.playbackRevision + 1L,
        )
    }

    /**
     * Saves the title's engine choice, null for Auto, and plays on in [run]: from [positionMs] with a new player when
     * its engine is another than the one playing.
     */
    fun chooseEngine(override: PlayerEngineOverride?, run: VideoPlayerEngineRun, restart: Boolean, positionMs: Long) {
        val current = mutableState.value as? State.Ready ?: return
        rememberEngineRun(run, override)
        mutableState.value = current.copy(
            playback = current.playback.copy(engineOverride = override),
            engineRun = run,
            resumePositionMs = if (restart) positionMs.coerceAtLeast(0L) else current.resumePositionMs,
            playbackRevision = current.playbackRevision + if (restart) 1L else 0L,
        )
        viewModelScope.launch {
            updatePlaybackPreferences(current.ownerEntryId) { it.copy(playerEngine = override) }
        }
    }

    fun markMpvHintShown() {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.mpvHintShown) return
        mutableState.value = current.copy(playback = current.playback.copy(mpvHintShown = true))
        viewModelScope.launch {
            updatePlaybackPreferences(current.ownerEntryId) { it.copy(mpvHintShown = true) }
        }
    }

    private fun rememberEngineRun(run: VideoPlayerEngineRun, override: PlayerEngineOverride?) {
        sessionEngineRun = run.takeIf { override == null && !it.tryOnce && it.engine == VideoPlayerEngine.Mpv }
    }

    fun updateAudioTracks(tracks: List<VideoPlayerAudioTrackOption>) {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.audioTracks == tracks) return
        mutableState.value = current.copy(playback = current.playback.copy(audioTracks = tracks))
    }

    fun selectAudioTrack(key: String) {
        sessionAudioTrackKey = key
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(playback = current.playback.copy(currentAudioTrackKey = key))
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

    fun persistPlayback(positionMs: Long, durationMs: Long, activeDurationMs: Long = 0L) {
        val current = mutableState.value as? State.Ready ?: return
        val safePositionMs = positionMs.coerceAtLeast(0L)
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        val session = playbackSession ?: VideoPlaybackSession(
            current.ownerEntryId,
            current.chapterId,
            current.chapterResourceKey,
            activitySession,
        )
            .also { playbackSession = it }
        val snapshot = session.snapshot(
            positionMs = safePositionMs,
            durationMs = safeDurationMs,
            activeDurationMs = activeDurationMs,
        )
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
                            activity = snapshot.activity,
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
            changeEpisode(previousEpisodeId)
        }
    }

    fun playNextEpisode() {
        val current = mutableState.value as? State.Ready ?: return
        val nextEpisodeId = current.childWindow.adjacent(EntryChildDirection.NEXT)?.id ?: return
        viewModelScope.launch {
            changeEpisode(nextEpisodeId)
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
            changeEpisode(episodeId, visibleEntryId, ownerEntryId)
        }
    }

    private suspend fun resolvePlayback(
        selection: PlaybackSelection? = null,
        preservePositionMs: Long? = null,
        initial: Boolean = false,
        showLoading: Boolean = true,
        requestedSubtitle: VideoPlayerSubtitleSelection? = null,
        changingEpisode: PendingEpisodeChange? = null,
    ) {
        val previousReady = (mutableState.value as? State.Ready)?.copy(episodeChange = null)
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        mutableState.value = when {
            previousReady == null -> loadingState
            changingEpisode != null -> previousReady.copy(
                episodeChange = EpisodeChange.Loading(changingEpisode.episodeName),
            )
            showLoading -> loadingState
            else -> previousReady.copy(isSourceSwitching = true)
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
                if (changingEpisode != null && previousReady != null) {
                    changingEpisode.restore()
                    previousReady.copy(
                        episodeChange = EpisodeChange.Failed(
                            episodeId = changingEpisode.episodeId,
                            visibleEntryId = changingEpisode.visibleEntryId,
                            episodeName = changingEpisode.episodeName,
                            problem = result.reason.toProblem(),
                            webView = episodeWebView(changingEpisode.episodeId),
                        ),
                    )
                } else if (!showLoading && previousReady != null) {
                    mutableEvents.tryEmit(Event.ShowProblem(result.reason.toProblem()))
                    previousReady.copy(
                        playback = previousReady.playback.copy(preview = VideoPlaybackPreviewState()),
                        isSourceSwitching = false,
                    )
                } else {
                    State.Error(
                        problem = result.reason.toProblem(),
                        title = loadingState.title,
                        webView = episodeWebView(chapterId),
                    )
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
                    activitySession,
                )
            session.restore(current.playbackStateByChapterId[current.chapterId])
            session.restore(current.resumePositionMs)
            playbackSession = session
        }
    }

    private fun resolveEpisodeNavigation(
        effectiveEntry: Entry,
        episodes: List<EntryChapter>,
        episodeId: Long,
    ): EntryChildWindow<EntryChapter>? {
        return episodes.sortedForReading(effectiveEntry).entryChildWindow(episodeId, EntryChapter::id)
    }

    private suspend fun resolveEpisodeDrawerData(
        entry: Entry,
        ownerEntry: Entry,
    ): EpisodeDrawerData {
        val effectiveEntry = if (bypassMerge) ownerEntry else entry
        val episodes = getEntryWithChapters.awaitChapters(effectiveEntry, bypassMerge = bypassMerge)
        val memberIds = episodes.map(EntryChapter::entryId).distinct()
        val fallbackEntries = mapOf(entry.id to entry, ownerEntry.id to ownerEntry)
        val memberEntries = memberIds.associateWith { memberId ->
            runCatching { entryRepository.getEntryById(memberId) }.getOrNull() ?: fallbackEntries[memberId]
        }
        val memberTitleById = memberEntries.mapValues { (_, member) -> member?.displayTitle.orEmpty() }
        val memberSourceIds = memberEntries.mapNotNull { (memberId, member) -> member?.let { memberId to it.source } }
        val memberSourceNameById = if (memberSourceIds.map { it.second }.distinct().size > 1) {
            memberSourceIds.associate { (memberId, sourceId) ->
                memberId to sourceNames.of(sourceId)
            }
        } else {
            emptyMap()
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
            memberSourceNameById = memberSourceNameById,
            playbackStateByChapterId = playbackStateByEpisodeId,
        )
    }

    private suspend fun changeEpisode(
        targetEpisodeId: Long,
        targetVisibleEntryId: Long = visibleEntryId,
        targetOwnerEntryId: Long = ownerEntryId,
    ) {
        val current = mutableState.value as? State.Ready ?: return
        applySelectionJob?.cancel()
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        if (nextChapterPreload?.key?.chapterId != targetEpisodeId) {
            nextChapterPreload = null
        }
        clearSelectionResultCache(preserveNextEpisodeId = targetEpisodeId)
        val targetChapter = entryChapterRepository.getChapterById(targetEpisodeId)
        val change = PendingEpisodeChange(
            episodeId = targetEpisodeId,
            visibleEntryId = targetVisibleEntryId,
            episodeName = targetChapter?.name
                ?: current.allChapters.firstOrNull { it.id == targetEpisodeId }?.name.orEmpty(),
            previousVisibleEntryId = visibleEntryId,
            previousOwnerEntryId = ownerEntryId,
            previousChapterId = chapterId,
            previousSession = playbackSession,
        )
        visibleEntryId = targetVisibleEntryId
        ownerEntryId = targetChapter?.entryId ?: targetOwnerEntryId
        chapterId = targetEpisodeId
        saveEpisodeIds()
        playbackSession = null
        resolvePlayback(initial = true, changingEpisode = change)
    }

    fun retryEpisodeChange() {
        val failed = (mutableState.value as? State.Ready)?.episodeChange as? EpisodeChange.Failed ?: return
        viewModelScope.launch {
            changeEpisode(failed.episodeId, failed.visibleEntryId)
        }
    }

    fun dismissEpisodeChange() {
        val current = mutableState.value as? State.Ready ?: return
        if (current.episodeChange !is EpisodeChange.Failed) return
        mutableState.value = current.copy(episodeChange = null)
    }

    private suspend fun episodeWebView(episodeId: Long): EntryChildWebViewResolution.Available? {
        val chapter = runCatching { entryChapterRepository.getChapterById(episodeId) }.getOrNull() ?: return null
        val owner = runCatching { entryRepository.getEntryById(chapter.entryId) }.getOrNull() ?: return null
        return webViewFeature.resolveChild(owner, chapter) as? EntryChildWebViewResolution.Available
    }

    private fun saveEpisodeIds() {
        savedState[VIDEO_ID_KEY] = visibleEntryId
        savedState[OWNER_VIDEO_ID_KEY] = ownerEntryId
        savedState[EPISODE_ID_KEY] = chapterId
    }

    /** An episode being opened over the one still playing, with what to put back if it can't be. */
    private inner class PendingEpisodeChange(
        val episodeId: Long,
        val visibleEntryId: Long,
        val episodeName: String,
        private val previousVisibleEntryId: Long,
        private val previousOwnerEntryId: Long,
        private val previousChapterId: Long,
        private val previousSession: VideoPlaybackSession?,
    ) {
        fun restore() {
            this@VideoPlayerViewModel.visibleEntryId = previousVisibleEntryId
            ownerEntryId = previousOwnerEntryId
            chapterId = previousChapterId
            saveEpisodeIds()
            playbackSession = previousSession
        }
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
        val episodeDrawerData = resolveEpisodeDrawerData(
            entry = result.visibleEntry,
            ownerEntry = result.ownerEntry,
        )
        val effectiveEntry = if (bypassMerge) result.ownerEntry else result.visibleEntry
        val childWindow = resolveEpisodeNavigation(
            effectiveEntry = effectiveEntry,
            episodes = episodeDrawerData.chapters,
            episodeId = result.chapter.id,
        ) ?: EntryChildWindow(current = result.chapter)
        val chapterListItems = buildVideoPlayerEpisodeDisplayData(
            entry = episodeDrawerData.entry,
            chapters = episodeDrawerData.chapters,
            memberIds = episodeDrawerData.memberIds,
            memberTitleById = episodeDrawerData.memberTitleById,
            memberSourceNameById = episodeDrawerData.memberSourceNameById,
            playbackStates = episodeDrawerData.playbackStateByChapterId.values.toList(),
        ).chapterListItems
        val playback = buildPlaybackUiState(result.playbackData, result.stream, result.savedPreferences)
            .copy(
                currentSubtitle = requestedSubtitle
                    ?.let { resolveSourceSubtitleSelection(it, result.stream.subtitles) }
                    ?: resolvePersistedSubtitleSelection(result.savedPreferences.subtitleKey, result.stream.subtitles),
            )
            .copy(preview = preview, currentAudioTrackKey = sessionAudioTrackKey)
        val childWebView = when (val resolution = webViewFeature.resolveChild(result.ownerEntry, result.chapter)) {
            is EntryChildWebViewResolution.Available -> resolution
            is EntryChildWebViewResolution.Failed -> {
                logcat(LogPriority.ERROR, resolution.cause) { "Failed to resolve episode WebView URL" }
                null
            }
            else -> null
        }
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
            chapterListItems = chapterListItems,
            sourceAvailable = true,
            childWebView = childWebView,
            entryTitle = result.ownerEntry.displayTitle,
            chapterName = result.chapter.name,
            streamLabel = playback.currentStreamLabel,
            streamUrl = playback.currentStream.resolvedRequest.url,
            stream = playback.currentStream,
            playback = playback,
            resumePositionMs = resumePositionMs,
            isSourceSwitching = isSourceSwitching,
            playbackRevision = playbackRevision,
            engineRun = sessionEngineRun
                ?.takeIf { result.savedPreferences.playerEngine == null }
                ?.copy(failures = emptyList()),
        )
    }

    private fun buildPlaybackUiState(
        playbackData: PlaybackDescriptor,
        currentStream: VideoStream,
        savedPreferences: PlaybackPreferences,
    ): VideoPlaybackUiState {
        val streamOptions = playbackData.streams
            .filter { it.request?.url?.isNotBlank() == true }
            .map { stream ->
                VideoPlaybackOption(
                    key = stream.key.ifBlank { stream.label.ifBlank { stream.resolvedRequest.url } },
                    label = stream.label.ifBlank { stream.resolvedRequest.url },
                )
            }

        return VideoPlaybackUiState(
            sourceSelection = playbackData.selection.copy(
                streamKey = currentStream.key.ifBlank {
                    currentStream.label.ifBlank { currentStream.resolvedRequest.url }
                },
            ),
            preferredSourceQualityKey = savedPreferences.sourceQualityKey,
            sessionPlaybackSpeed = sessionPlaybackSpeed,
            currentStream = currentStream,
            currentStreamLabel = currentStream.label.ifBlank { currentStream.resolvedRequest.url },
            streamOptions = streamOptions,
            playbackData = playbackData,
            currentAdaptiveQuality = savedPreferences.toAdaptiveQualityPreference(),
            subtitleAppearance = savedPreferences.toSubtitleAppearance(),
            engineOverride = savedPreferences.playerEngine,
            mpvHintShown = savedPreferences.mpvHintShown,
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
        updatePlaybackPreferences(entryId) { existingPreferences ->
            val resolvedSourceSelection = sourceSelection ?: PlaybackSelection(
                dubKey = existingPreferences.dubKey,
                streamKey = existingPreferences.streamKey,
                sourceQualityKey = existingPreferences.sourceQualityKey,
            )
            val resolvedAdaptiveQuality = adaptiveQuality ?: existingPreferences.toAdaptiveQualityPreference()
            val resolvedSubtitleAppearance =
                (subtitleAppearance ?: existingPreferences.toSubtitleAppearance()).normalized()
            existingPreferences.copy(
                dubKey = resolvedSourceSelection.dubKey,
                streamKey = resolvedSourceSelection.streamKey,
                sourceQualityKey = resolvedSourceSelection.sourceQualityKey,
                subtitleKey = if (updateSubtitleKey) subtitleKey else existingPreferences.subtitleKey,
                playerQualityMode = resolvedAdaptiveQuality.toPlayerQualityMode(),
                playerQualityHeight = resolvedAdaptiveQuality.heightOrNull(),
                subtitleOffsetX = resolvedSubtitleAppearance.toPersistedOffsetX(),
                subtitleOffsetY = resolvedSubtitleAppearance.toPersistedOffsetY(),
                subtitleTextSize = resolvedSubtitleAppearance.toPersistedTextSize(),
                subtitleTextColor = resolvedSubtitleAppearance.toPersistedTextColor(),
                subtitleBackgroundColor = resolvedSubtitleAppearance.toPersistedBackgroundColor(),
                subtitleBackgroundOpacity = resolvedSubtitleAppearance.toPersistedBackgroundOpacity(),
            )
        }
    }

    // Each save changes some of the title's preferences and keeps the rest, so saves made at once mustn't interleave.
    private suspend fun updatePlaybackPreferences(entryId: Long, update: (PlaybackPreferences) -> PlaybackPreferences) {
        preferencesMutex.withLock {
            val existingPreferences = playbackPreferencesRepository.getByEntryId(entryId)
                ?: defaultPlaybackPreferences(entryId)
            playbackPreferencesRepository.upsert(
                update(existingPreferences).copy(updatedAt = System.currentTimeMillis()),
            )
        }
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
        /** [title] and [episodeName] are null until the episode being opened has been read. */
        data class Loading(val title: String? = null, val episodeName: String? = null) : State

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
            val chapterListItems: List<VideoPlayerEpisodeListEntry>,
            val sourceAvailable: Boolean,
            val childWebView: EntryChildWebViewResolution.Available?,
            val entryTitle: String,
            val chapterName: String,
            val streamLabel: String,
            val streamUrl: String,
            val stream: VideoStream,
            val playback: VideoPlaybackUiState,
            val resumePositionMs: Long,
            val isSourceSwitching: Boolean = false,
            val playbackRevision: Long = 0L,
            val episodeChange: EpisodeChange? = null,
            /** The engine a new player uses, once Auto or the viewer moved away from the title's start choice. */
            val engineRun: VideoPlayerEngineRun? = null,
        ) : State {
            val previousChapterId: Long?
                get() = childWindow.previous?.id

            val nextChapterId: Long?
                get() = childWindow.next?.id
        }

        data class Error(
            val problem: VideoPlayerProblem,
            val title: String?,
            val webView: EntryChildWebViewResolution.Available?,
        ) : State
    }

    /** Another episode being opened, or one that couldn't be, over the episode still on screen. */
    sealed interface EpisodeChange {
        val episodeName: String

        data class Loading(override val episodeName: String) : EpisodeChange

        data class Failed(
            val episodeId: Long,
            val visibleEntryId: Long,
            override val episodeName: String,
            val problem: VideoPlayerProblem,
            val webView: EntryChildWebViewResolution.Available?,
        ) : EpisodeChange
    }

    sealed interface Event {
        data class ShowProblem(val problem: VideoPlayerProblem) : Event
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
    val memberSourceNameById: Map<Long, String>,
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
