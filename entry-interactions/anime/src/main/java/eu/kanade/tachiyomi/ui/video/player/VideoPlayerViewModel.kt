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
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoSwitchFailure
import eu.kanade.tachiyomi.ui.video.player.recovery.sourceChoice
import eu.kanade.tachiyomi.ui.video.player.recovery.switchTarget
import eu.kanade.tachiyomi.ui.video.player.server.ServerFallback
import eu.kanade.tachiyomi.ui.video.player.server.ServerFallbackNotice
import eu.kanade.tachiyomi.ui.video.player.server.allowsServerFallback
import eu.kanade.tachiyomi.ui.video.player.server.playingServer
import eu.kanade.tachiyomi.ui.video.player.server.withServersListedBefore
import eu.kanade.tachiyomi.ui.video.player.tracks.VideoPlayerAudioTrackOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import mihon.entry.interactions.anime.stream.ResolvedStreamCache
import mihon.entry.interactions.anime.stream.streamChoiceKey
import mihon.entry.interactions.anime.stream.streamDisplayLabel
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntrySourceSettingsResolution
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.video.engine.spi.loading.resolvedRequest
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.model.groupId
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.service.EntryChapterFilters
import tachiyomi.domain.entry.service.expansionSession
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
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
    private val entryChapterGroupRepository: EntryChapterGroupRepository,
    chapterGroupExpansionRepository: ChapterGroupExpansionRepository,
    private val childListFeature: EntryChildListFeature,
    private val entryProgressRepository: EntryProgressRepository,
    private val mediaSession: AnimeMediaSessionProcessor,
    private val webViewFeature: EntryWebViewFeature,
    private val sourceSettingsFeature: EntrySourceSettingsFeature,
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val libraryPreferences: LibraryPreferences,
    private val resolveDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Loading())
    val state = mutableState.asStateFlow()

    private val memberExpansion = mergedMemberExpansionRepository.expansionSession(
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

    private val groupExpansion = chapterGroupExpansionRepository.expansionSession(
        scope = viewModelScope,
        entryIds = mutableState.filterIsInstance<State.Ready>().map { it.memberIds },
    )

    /** Groups of episodes the drawer lists; the playing episode's group opens until it is toggled. */
    val expandedGroupIds: StateFlow<Set<ChapterGroupId>> = combine(
        groupExpansion.state,
        mutableState.filterIsInstance<State.Ready>().map { it.playingGroupId },
    ) { expansion, playingGroupId -> expansion.shown(playingGroupId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleGroupExpanded(group: ChapterGroupId) {
        val ready = mutableState.value as? State.Ready ?: return
        groupExpansion.toggle(group, ready.playingGroupId)
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
    private val resolvedStreams = ResolvedStreamCache()
    private var serverFallback = ServerFallback()
    private var nextChapterPreloadJob: Job? = null
    private var nextChapterPreload: PreloadedEpisode? = null
    private var loadingState = State.Loading()
    private var loadingSourceName: String? = null

    // Opening the episode the player started on, or one picked on its problem or loading screen.
    private var loadJob: Job? = null

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

        loadJob = viewModelScope.launch {
            val chapter = runCatching { entryChapterRepository.getChapterById(chapterId) }.getOrNull()
            // The episode's own entry: a merged entry's member, whose source is the one asked.
            val member = runCatching { entryRepository.getEntryById(chapter?.entryId ?: ownerEntryId) }.getOrNull()
            val visible = runCatching { entryRepository.getEntryById(entryId) }.getOrNull()
            loadingSourceName = member?.let { sourceNames.of(it.source) }
            val heading = headingTitles(visible = visible, member = member)
            loadingState = State.Loading(
                title = heading.title,
                memberTitle = heading.memberTitle,
                episodeName = chapter?.name,
            )
            if (mutableState.value is State.Loading) mutableState.value = loadingState
            resolvePlayback(initial = true)
        }
    }

    fun retry() {
        if (mutableState.value !is State.Error) return
        resolvedStreams.clear()
        serverFallback = ServerFallback()
        loadJob = viewModelScope.launch {
            resolvePlayback(initial = true)
        }
    }

    fun retryCurrentPlayback(positionMs: Long) {
        val current = mutableState.value as? State.Ready ?: return
        // The link that failed may have expired; ask the source for fresh ones.
        resolvedStreams.clear()
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
        // A server the viewer picks is the one they want to see fail, if it does.
        serverFallback =
            ServerFallback(explicitPick = selection.serverKey != current.playback.sourceSelection.serverKey)
        applySelectionJob = viewModelScope.launch {
            val preservedSubtitle = current.playback.currentSubtitle
            // The quality is the viewer's to keep; the dub, server and stream are saved only once they play.
            updatePlaybackPreferences(current.ownerEntryId) { it.copy(sourceQualityKey = selection.sourceQualityKey) }
            if (!isActive) return@launch
            resolvePlayback(
                selection = selection,
                explicitStreamPick = selection.streamKey != null &&
                    selection.streamKey != current.playback.sourceSelection.streamKey,
                preservePositionMs = current.resumePositionMs,
                showLoading = false,
                requestedSubtitle = preservedSubtitle,
            )
        }
    }

    /**
     * Lists a drafted dub's or server's options for the playing episode's settings, or for the picker of the episode
     * the player is opening or failed to open. A failure stays on the preview, where the rows offer to ask again.
     */
    fun previewSourceSelection(selection: PlaybackSelection) {
        preview(sourcePreviewTarget() ?: return, selection)
    }

    /** As [previewSourceSelection], for the picker of another episode that couldn't be opened. */
    fun previewEpisodeChangeSelection(selection: PlaybackSelection) {
        preview(episodeChangePreviewTarget() ?: return, selection)
    }

    private fun preview(target: PreviewTarget, selection: PlaybackSelection) {
        if (selection.dubKey == target.listed.dubKey && selection.serverKey == target.listed.serverKey) {
            previewSelectionJob?.cancel()
            target.write(VideoPlaybackPreviewState())
            return
        }

        val currentPreview = target.read() ?: return
        if (currentPreview.selection == selection && currentPreview.isLoading) return

        val cachedPreview = cachedSelectionResult(target.episodeId, selection)
        if (cachedPreview != null) {
            target.write(cachedPreview.toPreview(selection))
            return
        }

        previewSelectionJob?.cancel()
        target.write(VideoPlaybackPreviewState(selection = selection, isLoading = true))
        previewSelectionJob = viewModelScope.launch {
            val result = try {
                withContext(resolveDispatcher) {
                    resolveVideoStream(
                        entryId = target.visibleEntryId,
                        chapterId = target.episodeId,
                        ownerEntryId = target.ownerEntryId,
                        selection = selection,
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            }
            if (target.read()?.selection != selection) return@launch

            when (result) {
                is ResolveVideoStream.Result.Success -> {
                    cacheSelectionResult(target.episodeId, selection, result)
                    target.write(result.toPreview(selection))
                }
                is ResolveVideoStream.Result.Error -> target.write(
                    VideoPlaybackPreviewState(selection = selection, failed = true),
                )
            }
        }
    }

    /**
     * The episode a source preview lists options for, the selection those are drafted from, and where the preview
     * is kept in the state, read and written as the state is at that moment.
     */
    private class PreviewTarget(
        val listed: PlaybackSelection,
        val visibleEntryId: Long,
        val ownerEntryId: Long,
        val episodeId: Long,
        val read: () -> VideoPlaybackPreviewState?,
        val write: (VideoPlaybackPreviewState) -> Unit,
    )

    private fun sourcePreviewTarget(): PreviewTarget? {
        val listed = when (val current = mutableState.value) {
            is State.Ready -> current.playback.sourceSelection
            is State.Error -> current.choices?.selection
            is State.Loading -> current.choices?.selection
        } ?: return null
        return PreviewTarget(
            listed = listed,
            visibleEntryId = visibleEntryId,
            ownerEntryId = ownerEntryId,
            episodeId = chapterId,
            read = {
                when (val current = mutableState.value) {
                    is State.Ready -> current.playback.preview
                    is State.Error -> current.preview
                    is State.Loading -> current.preview
                }
            },
            write = { preview ->
                mutableState.value = when (val current = mutableState.value) {
                    is State.Ready -> current.copy(playback = current.playback.copy(preview = preview))
                    is State.Error -> current.copy(preview = preview)
                    is State.Loading -> current.copy(preview = preview).also { loadingState = it }
                }
            },
        )
    }

    private fun episodeChangePreviewTarget(): PreviewTarget? {
        val failed = failedEpisodeChange() ?: return null
        val listed = failed.choices?.selection ?: return null
        return PreviewTarget(
            listed = listed,
            visibleEntryId = failed.visibleEntryId,
            ownerEntryId = failed.ownerEntryId,
            episodeId = failed.episodeId,
            read = { failedEpisodeChange()?.takeIf { it.episodeId == failed.episodeId }?.preview },
            write = { preview ->
                val current = mutableState.value as? State.Ready
                val change = failedEpisodeChange()
                if (current != null && change?.episodeId == failed.episodeId) {
                    mutableState.value = current.copy(episodeChange = change.copy(preview = preview))
                }
            },
        )
    }

    private fun failedEpisodeChange(): EpisodeChange.Failed? =
        (mutableState.value as? State.Ready)?.episodeChange as? EpisodeChange.Failed

    /**
     * Opens the episode that failed or is still opening with [selection], picked on its problem or loading screen,
     * in place of the attempt under way. A server picked there is the viewer's to see fail; the servers that failed
     * stay marked while the dub stays the same.
     */
    fun openWithSelection(selection: PlaybackSelection) {
        val (listed, failedServerKeys) = when (val current = mutableState.value) {
            is State.Error -> current.choices to current.failedServerKeys
            is State.Loading -> current.choices to current.failedServerKeys
            is State.Ready -> return
        }
        val listedSelection = listed?.selection ?: return
        loadJob?.cancel()
        previewSelectionJob?.cancel()
        serverFallback = if (selection.dubKey == listedSelection.dubKey) {
            ServerFallback(
                tried = failedServerKeys,
                explicitPick = selection.serverKey != listedSelection.serverKey,
            )
        } else {
            ServerFallback()
        }
        loadJob = viewModelScope.launch {
            // The quality is the viewer's to keep; the dub, server and stream are saved only once they play.
            updatePlaybackPreferences(ownerEntryId) { it.copy(sourceQualityKey = selection.sourceQualityKey) }
            resolvePlayback(
                selection = selection,
                initial = true,
                explicitStreamPick = selection.streamKey != null && selection.streamKey != listedSelection.streamKey,
                listedBefore = listed,
            )
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
                pictureFraming = PictureFraming.Fit,
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
            updatePlaybackPreferences(current.visibleEntryId) { it.copy(pictureFraming = PictureFraming.Fit) }
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

    fun setPictureFraming(framing: PictureFraming) {
        val current = mutableState.value as? State.Ready ?: return
        if (current.playback.pictureFraming == framing) return
        mutableState.value = current.copy(playback = current.playback.copy(pictureFraming = framing))
        viewModelScope.launch {
            updatePlaybackPreferences(current.visibleEntryId) { it.copy(pictureFraming = framing) }
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

    /**
     * Moves to the next server after the playing one failed before its first frame. False when automatic fallback
     * stops here, and the failure should be shown.
     */
    fun tryNextServer(positionMs: Long): Boolean {
        val current = mutableState.value as? State.Ready ?: return false
        val retry = nextServer(current.playback.playbackData) ?: return false
        viewModelScope.launch {
            resolvePlayback(
                selection = retry.selection,
                preservePositionMs = positionMs.coerceAtLeast(0L),
                showLoading = false,
                requestedSubtitle = current.playback.currentSubtitle,
                serverNotice = retry.notice,
                listedBefore = current.playback.playbackData,
            )
        }
        return true
    }

    /**
     * Saves the dub, server and stream [playing] plays as the title's, now that its first frame showed they work. A
     * choice that never plays, picked or reached by fallback, leaves the saved one in place.
     */
    fun confirmPlayback(playing: State.Ready) {
        if (playing.playback.downloaded) return
        val selection = playing.playback.persistedSourceSelection
        viewModelScope.launch {
            updatePlaybackPreferences(playing.ownerEntryId) {
                it.copy(
                    dubKey = selection.dubKey,
                    serverKey = selection.serverKey,
                    streamKey = selection.streamKey,
                    sourceQualityKey = selection.sourceQualityKey,
                )
            }
        }
    }

    /** Tries the failed switch again, with links fetched afresh as a retry does. */
    fun retrySwitch() {
        val failure = (mutableState.value as? State.Ready)?.switchFailure ?: return
        resolvedStreams.clear()
        applySourceSelection(failure.selection)
    }

    fun dismissSwitchFailure() {
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(switchFailure = null)
    }

    fun dismissServerNotice() {
        val current = mutableState.value as? State.Ready ?: return
        mutableState.value = current.copy(serverNotice = null)
    }

    /** Where [reason] is fixed outside the player, when the app has a screen for it. */
    private suspend fun problemAction(reason: ResolveVideoStream.Reason): VideoProblemAction? = when (reason) {
        ResolveVideoStream.Reason.SourceNotFound -> VideoProblemAction.OpenExtensions
        ResolveVideoStream.Reason.EpisodeNotFound,
        ResolveVideoStream.Reason.EpisodeMismatch,
        -> visibleEntryId
            .takeIf { runCatching { entryRepository.getEntryById(it) }.getOrNull() != null }
            ?.let(VideoProblemAction::OpenEntry)
        ResolveVideoStream.Reason.TorrentOnly -> runCatching { entryRepository.getEntryById(ownerEntryId) }.getOrNull()
            ?.source
            ?.takeIf { sourceSettingsFeature.resolve(it) is EntrySourceSettingsResolution.Available }
            ?.let(VideoProblemAction::OpenSourceSettings)
        else -> null
    }

    /**
     * Shows on the loading screen, while the stream's link resolves, the server it opens and the options the source
     * listed, which the screen offers to switch to.
     */
    private fun showListed(playbackData: PlaybackDescriptor, serverNotice: ServerFallbackNotice?) {
        if (mutableState.value !is State.Loading) return
        loadingState = loadingState.copy(
            step = LoadingStep.Opening(
                server = playingServer(playbackData)?.label,
                failedServer = serverNotice?.failedServer,
            ),
            choices = playbackData.takeIf { it.sourceChoice() != null },
            failedServerKeys = serverFallback.tried,
        )
        mutableState.value = loadingState
    }

    /** The servers this episode failed on: those fallback moved away from, and the one [error] came from. */
    private fun failedServerKeys(error: ResolveVideoStream.Result.Error): Set<String> {
        val last = error.playbackData
            ?.takeIf { error.reason.allowsServerFallback }
            ?.let(::playingServer)
            ?.key
        return serverFallback.tried + listOfNotNull(last)
    }

    /** The server to open on after [error], when another server could avoid it and fallback may go on. */
    private fun nextServerAfter(error: ResolveVideoStream.Result.Error): ServerFallbackRetry? {
        if (!error.reason.allowsServerFallback) return null
        return error.playbackData?.let(::nextServer)
    }

    private fun nextServer(playbackData: PlaybackDescriptor): ServerFallbackRetry? {
        val failed = playingServer(playbackData) ?: return null
        val step = serverFallback.next(playbackData.servers, failed) ?: return null
        serverFallback = step.fallback
        return ServerFallbackRetry(
            selection = playbackData.selection.copy(serverKey = step.next.key, streamKey = null),
            notice = ServerFallbackNotice(step.failed.label, step.next.label),
        )
    }

    fun playNextEpisode() {
        val current = mutableState.value as? State.Ready ?: return
        val nextEpisodeId = current.childWindow.adjacent(EntryChildDirection.NEXT)?.id ?: return
        viewModelScope.launch {
            changeEpisode(nextEpisodeId)
        }
    }

    /** Lists the next episode's streams ahead of time. Its link is resolved only once it plays: resolved links expire. */
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
        explicitStreamPick: Boolean = false,
        serverNotice: ServerFallbackNotice? = null,
        listedBefore: PlaybackDescriptor? = null,
    ) {
        val previousReady = (mutableState.value as? State.Ready)?.copy(episodeChange = null, switchFailure = null)
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        // A move to another server lists that server's streams again, so it says where it goes instead.
        loadingState = loadingState.copy(
            step = serverNotice
                ?.let { LoadingStep.Opening(server = it.playingServer, failedServer = it.failedServer) }
                ?: loadingSourceName?.let(LoadingStep::AskingSource),
            preview = VideoPlaybackPreviewState(),
        )
        mutableState.value = when {
            previousReady == null -> loadingState
            changingEpisode != null -> previousReady.copy(
                episodeChange = EpisodeChange.Loading(changingEpisode.episodeName),
            )
            showLoading -> loadingState
            else -> previousReady.copy(isSourceSwitching = true)
        }

        val targetSelection = selection ?: previousReady?.playback?.persistedSourceSelection
        val listed = consumeMatchingPreload(
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
        val result = when (listed) {
            is ResolveVideoStream.Result.Success -> {
                showListed(listed.playbackData, serverNotice)
                withContext(resolveDispatcher) {
                    resolveVideoStream.resolveLink(listed, resolvedStreams, explicitStreamPick)
                }
            }
            is ResolveVideoStream.Result.Error -> listed.withServersListedBefore(listedBefore, targetSelection)
        }
        if (result is ResolveVideoStream.Result.Error) {
            val step = nextServerAfter(result)
            if (step != null) {
                resolvePlayback(
                    selection = step.selection,
                    preservePositionMs = preservePositionMs,
                    initial = initial,
                    showLoading = showLoading,
                    requestedSubtitle = requestedSubtitle,
                    changingEpisode = changingEpisode,
                    serverNotice = step.notice,
                    listedBefore = result.playbackData,
                )
                return
            }
        }
        mutableState.value = when (result) {
            is ResolveVideoStream.Result.Success -> {
                cacheSelectionResult(result.chapter.id, result.playbackData.selection, result)
                buildReadyState(
                    result = result,
                    preservePositionMs = preservePositionMs,
                    preview = VideoPlaybackPreviewState(),
                    isSourceSwitching = false,
                    requestedSubtitle = requestedSubtitle,
                    playbackRevision = (previousReady?.playbackRevision ?: -1L) + 1L,
                    serverNotice = serverNotice,
                )
            }
            is ResolveVideoStream.Result.Error -> {
                if (changingEpisode != null && previousReady != null) {
                    val failedOwnerEntryId = ownerEntryId
                    changingEpisode.restore()
                    previousReady.copy(
                        episodeChange = EpisodeChange.Failed(
                            episodeId = changingEpisode.episodeId,
                            visibleEntryId = changingEpisode.visibleEntryId,
                            ownerEntryId = failedOwnerEntryId,
                            episodeName = changingEpisode.episodeName,
                            problem = result.reason.toProblem(),
                            webView = episodeWebView(changingEpisode.episodeId),
                            choices = result.playbackData?.takeIf { it.sourceChoice() != null },
                            failedServerKeys = failedServerKeys(result),
                        ),
                    )
                } else if (!showLoading && previousReady != null) {
                    val attempted = targetSelection ?: previousReady.playback.persistedSourceSelection
                    previousReady.copy(
                        playback = previousReady.playback.copy(preview = VideoPlaybackPreviewState()),
                        isSourceSwitching = false,
                        switchFailure = VideoSwitchFailure(
                            problem = result.reason.toProblem(),
                            selection = attempted,
                            target = switchTarget(
                                listed = previousReady.playback.playbackData,
                                playing = previousReady.playback.sourceSelection,
                                attempted = attempted,
                            ),
                        ),
                    )
                } else {
                    State.Error(
                        problem = result.reason.toProblem(),
                        title = loadingState.title,
                        memberTitle = loadingState.memberTitle,
                        episodeName = loadingState.episodeName,
                        webView = episodeWebView(chapterId),
                        choices = result.playbackData?.takeIf { it.sourceChoice() != null },
                        failedServerKeys = failedServerKeys(result),
                        action = problemAction(result.reason),
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

    private suspend fun resolveEpisodeDrawerData(
        entry: Entry,
        ownerEntry: Entry,
        playingEpisodeId: Long,
    ): EpisodeDrawerData {
        val effectiveEntry = if (bypassMerge) ownerEntry else entry
        val allEpisodes = getEntryWithChapters.awaitChapters(effectiveEntry, bypassMerge = bypassMerge)
        // Episodes of excluded scanlators are left out, as the manga reader leaves their chapters out, except the one
        // playing, which may have been opened directly
        val shownEpisodeIds = getEntryWithChapters
            .awaitChapters(effectiveEntry, bypassMerge = bypassMerge, applyScanlatorFilter = true)
            .mapTo(hashSetOf(), EntryChapter::id)
        val episodes = allEpisodes.filter { it.id in shownEpisodeIds || it.id == playingEpisodeId }
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
        val filters = EntryChapterFilters(effectiveEntry, libraryPreferences.downloadedOnly.get())
        val downloadedEpisodeIds = if (filters.downloaded != TriState.DISABLED) {
            downloadedEpisodeIds(episodes, memberEntries)
        } else {
            emptySet()
        }

        return EpisodeDrawerData(
            entry = entry,
            chapters = episodes,
            allChapters = allEpisodes,
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            memberSourceNameById = memberSourceNameById,
            playbackStateByChapterId = playbackStateByEpisodeId,
            isShown = { episode -> filters.shows(episode) { episode.id in downloadedEpisodeIds } },
            chapterGroups = memberIds.flatMap { entryChapterGroupRepository.getByEntryId(it) },
        )
    }

    private fun downloadedEpisodeIds(episodes: List<EntryChapter>, memberEntries: Map<Long, Entry?>): Set<Long> {
        return episodes.groupBy(EntryChapter::entryId).flatMapTo(hashSetOf()) { (memberId, memberEpisodes) ->
            val member = memberEntries[memberId] ?: return@flatMapTo emptyList()
            if (member.source == LOCAL_SOURCE_ID) return@flatMapTo memberEpisodes.map(EntryChapter::id)
            downloadRuntime.statuses(member, memberEpisodes).orEmpty()
                .filterValues { it.state == EntryDownloadState.DOWNLOADED }
                .keys
        }
    }

    private suspend fun changeEpisode(
        targetEpisodeId: Long,
        targetVisibleEntryId: Long = visibleEntryId,
        targetOwnerEntryId: Long = ownerEntryId,
        selection: PlaybackSelection? = null,
        fallback: ServerFallback = ServerFallback(),
        explicitStreamPick: Boolean = false,
    ) {
        val current = mutableState.value as? State.Ready ?: return
        applySelectionJob?.cancel()
        previewSelectionJob?.cancel()
        nextChapterPreloadJob?.cancel()
        if (nextChapterPreload?.key?.chapterId != targetEpisodeId) {
            nextChapterPreload = null
        }
        clearSelectionResultCache(preserveNextEpisodeId = targetEpisodeId)
        serverFallback = fallback
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
        resolvePlayback(
            selection = selection,
            initial = true,
            changingEpisode = change,
            explicitStreamPick = explicitStreamPick,
        )
    }

    fun retryEpisodeChange() {
        val failed = (mutableState.value as? State.Ready)?.episodeChange as? EpisodeChange.Failed ?: return
        viewModelScope.launch {
            changeEpisode(failed.episodeId, failed.visibleEntryId)
        }
    }

    /** Opens the episode that couldn't be opened again with [selection], picked from what its source listed. */
    fun retryEpisodeChangeWith(selection: PlaybackSelection) {
        val failed = failedEpisodeChange() ?: return
        val listed = failed.choices?.selection ?: return
        previewSelectionJob?.cancel()
        val fallback = if (selection.dubKey == listed.dubKey) {
            ServerFallback(tried = failed.failedServerKeys, explicitPick = selection.serverKey != listed.serverKey)
        } else {
            ServerFallback()
        }
        viewModelScope.launch {
            changeEpisode(
                targetEpisodeId = failed.episodeId,
                targetVisibleEntryId = failed.visibleEntryId,
                targetOwnerEntryId = failed.ownerEntryId,
                selection = selection,
                fallback = fallback,
                explicitStreamPick = selection.streamKey != null && selection.streamKey != listed.streamKey,
            )
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
        serverNotice: ServerFallbackNotice? = null,
    ): State.Ready {
        val resumePositionMs = preservePositionMs
            ?: entryProgressRepository.get(result.ownerEntry.id, "", result.chapter.progressResourceKey)?.positionMs
            ?: 0L
        val episodeDrawerData = resolveEpisodeDrawerData(
            entry = result.visibleEntry,
            ownerEntry = result.ownerEntry,
            playingEpisodeId = result.chapter.id,
        )
        val effectiveEntry = if (bypassMerge) result.ownerEntry else result.visibleEntry
        val childWindow = episodeDrawerData.chapters
            .episodeWindow(effectiveEntry, result.chapter.id, episodeDrawerData.isShown)
            ?: EntryChildWindow(current = result.chapter)
        val chapterListItems = buildVideoPlayerEpisodeDisplayData(
            entry = episodeDrawerData.entry,
            chapters = episodeDrawerData.chapters,
            memberIds = episodeDrawerData.memberIds,
            memberTitleById = episodeDrawerData.memberTitleById,
            memberSourceNameById = episodeDrawerData.memberSourceNameById,
            playbackStates = episodeDrawerData.playbackStateByChapterId.values.toList(),
            playingEpisodeId = result.chapter.id,
            isShown = episodeDrawerData.isShown,
            chapterGroups = episodeDrawerData.chapterGroups,
            allChapters = episodeDrawerData.allChapters,
            childListFeature = childListFeature,
        ).chapterListItems
        // A preloaded episode read the title's preferences before the latest framing was saved.
        val pictureFraming = (mutableState.value as? State.Ready)
            ?.takeIf { it.visibleEntryId == result.visibleEntry.id }
            ?.playback
            ?.pictureFraming
            ?: savedPictureFraming(result)
        val heading = headingTitles(visible = result.visibleEntry, member = result.ownerEntry)
        val playback = buildPlaybackUiState(result.playbackData, result.stream, result.preferences, result.downloaded)
            .copy(
                currentSubtitle = requestedSubtitle
                    ?.let { resolveSourceSubtitleSelection(it, result.stream.subtitles) }
                    ?: resolvePersistedSubtitleSelection(result.preferences.subtitleKey, result.stream.subtitles),
            )
            .copy(preview = preview, currentAudioTrackKey = sessionAudioTrackKey, pictureFraming = pictureFraming)
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
            entryTitle = heading.title.orEmpty(),
            memberTitle = heading.memberTitle,
            chapterName = result.chapter.name,
            streamLabel = playback.currentStreamLabel,
            streamUrl = playback.currentStream.resolvedRequest.url,
            stream = playback.currentStream,
            playback = playback,
            resumePositionMs = resumePositionMs,
            isSourceSwitching = isSourceSwitching,
            playbackRevision = playbackRevision,
            engineRun = sessionEngineRun
                ?.takeIf { result.preferences.playerEngine == null }
                ?.copy(failures = emptyList()),
            serverNotice = serverNotice,
        )
    }

    /**
     * What the heading names: the entry the library shows, so a merged entry's series rather than its member, and the
     * member when its title differs. Played on its own, outside the merge, the member is the title.
     */
    private fun headingTitles(visible: Entry?, member: Entry?): HeadingTitles {
        val title = (if (bypassMerge) member else visible ?: member)?.displayTitle
        return HeadingTitles(title = title, memberTitle = member?.displayTitle?.takeIf { it != title })
    }

    private data class HeadingTitles(val title: String?, val memberTitle: String?)

    // The framing is the entry the library shows, so a merged entry's members share it.
    private suspend fun savedPictureFraming(result: ResolveVideoStream.Result.Success): PictureFraming {
        if (result.visibleEntry.id == result.ownerEntry.id) return result.preferences.pictureFraming
        return playbackPreferencesRepository.getByEntryId(result.visibleEntry.id)?.pictureFraming ?: PictureFraming.Fit
    }

    private fun buildPlaybackUiState(
        playbackData: PlaybackDescriptor,
        currentStream: VideoStream,
        savedPreferences: PlaybackPreferences,
        downloaded: Boolean,
    ): VideoPlaybackUiState {
        val streamOptions = playbackData.streams
            .map { stream ->
                VideoPlaybackOption(
                    key = streamChoiceKey(stream),
                    label = streamDisplayLabel(stream),
                )
            }

        return VideoPlaybackUiState(
            sourceSelection = playbackData.selection.copy(
                streamKey = streamChoiceKey(currentStream),
            ),
            preferredSourceQualityKey = savedPreferences.sourceQualityKey,
            sessionPlaybackSpeed = sessionPlaybackSpeed,
            currentStream = currentStream,
            currentStreamLabel = streamDisplayLabel(currentStream),
            streamOptions = streamOptions,
            playbackData = playbackData,
            currentAdaptiveQuality = savedPreferences.toAdaptiveQualityPreference(),
            subtitleAppearance = savedPreferences.toSubtitleAppearance(),
            engineOverride = savedPreferences.playerEngine,
            mpvHintShown = savedPreferences.mpvHintShown,
            downloaded = downloaded,
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
                serverKey = existingPreferences.serverKey,
                streamKey = existingPreferences.streamKey,
                sourceQualityKey = existingPreferences.sourceQualityKey,
            )
            val resolvedAdaptiveQuality = adaptiveQuality ?: existingPreferences.toAdaptiveQualityPreference()
            val resolvedSubtitleAppearance =
                (subtitleAppearance ?: existingPreferences.toSubtitleAppearance()).normalized()
            existingPreferences.copy(
                dubKey = resolvedSourceSelection.dubKey,
                serverKey = resolvedSourceSelection.serverKey,
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
                subtitleTextEdge = resolvedSubtitleAppearance.toPersistedTextEdge(),
            )
        }
    }

    // Each save changes some of the title's preferences and keeps the rest, so saves made at once mustn't interleave.
    private suspend fun updatePlaybackPreferences(entryId: Long, update: (PlaybackPreferences) -> PlaybackPreferences) {
        preferencesMutex.withLock {
            val existingPreferences = playbackPreferencesRepository.getByEntryId(entryId)
                ?: defaultPlaybackPreferences(entryId)
            val updatedPreferences = update(existingPreferences)
            // Each new player confirms its selection again, mostly unchanged.
            if (updatedPreferences == existingPreferences) return
            playbackPreferencesRepository.upsert(updatedPreferences.copy(updatedAt = System.currentTimeMillis()))
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
        /**
         * [title] and [episodeName] are null until the episode being opened has been read. [step] says what the
         * player waits for; once the source has listed them, [choices] are its other dubs, servers, streams and
         * qualities, which the viewer may switch to instead of waiting, and [failedServerKeys] the servers that
         * failed so far.
         */
        data class Loading(
            val title: String? = null,
            val memberTitle: String? = null,
            val episodeName: String? = null,
            val step: LoadingStep? = null,
            val choices: PlaybackDescriptor? = null,
            val failedServerKeys: Set<String> = emptySet(),
            val preview: VideoPlaybackPreviewState = VideoPlaybackPreviewState(),
        ) : State

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
            /** The merged member the episode comes from, when its title isn't [entryTitle]. */
            val memberTitle: String?,
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
            /** Set when the player moved to this server by itself after another one failed. */
            val serverNotice: ServerFallbackNotice? = null,
            /** A switch or retry that failed, which the episode played on through. */
            val switchFailure: VideoSwitchFailure? = null,
        ) : State {
            val previousChapterId: Long?
                get() = childWindow.previous?.id

            val nextChapterId: Long?
                get() = childWindow.next?.id

            val playingGroupId: ChapterGroupId
                get() = childWindow.current.groupId
        }

        /**
         * An episode that couldn't open. [choices] are the dubs, servers, streams and qualities the source listed
         * before it failed, when it offers others; [failedServerKeys] are the servers it failed on; [action] is where
         * the problem is fixed outside the player.
         */
        data class Error(
            val problem: VideoPlayerProblem,
            val title: String?,
            val memberTitle: String?,
            val episodeName: String?,
            val webView: EntryChildWebViewResolution.Available?,
            val choices: PlaybackDescriptor? = null,
            val failedServerKeys: Set<String> = emptySet(),
            val action: VideoProblemAction? = null,
            val preview: VideoPlaybackPreviewState = VideoPlaybackPreviewState(),
        ) : State
    }

    /** What opening an episode waits for. */
    sealed interface LoadingStep {
        data class AskingSource(val sourceName: String) : LoadingStep

        /** [server] is null when the source lists none; [failedServer] is the one fallback moved away from. */
        data class Opening(val server: String?, val failedServer: String?) : LoadingStep
    }

    /** Another episode being opened, or one that couldn't be, over the episode still on screen. */
    sealed interface EpisodeChange {
        val episodeName: String

        data class Loading(override val episodeName: String) : EpisodeChange

        /**
         * [choices] are the dubs, servers, streams and qualities the episode's source listed before it failed, when
         * it offers others, and [failedServerKeys] the servers it failed on.
         */
        data class Failed(
            val episodeId: Long,
            val visibleEntryId: Long,
            val ownerEntryId: Long,
            override val episodeName: String,
            val problem: VideoPlayerProblem,
            val webView: EntryChildWebViewResolution.Available?,
            val choices: PlaybackDescriptor? = null,
            val failedServerKeys: Set<String> = emptySet(),
            val preview: VideoPlaybackPreviewState = VideoPlaybackPreviewState(),
        ) : EpisodeChange
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
    /** Also the episodes of excluded scanlators, which tell a group with no episodes yet from a hidden one. */
    val allChapters: List<EntryChapter>,
    val memberIds: List<Long>,
    val memberTitleById: Map<Long, String>,
    val memberSourceNameById: Map<Long, String>,
    val playbackStateByChapterId: Map<Long, EntryProgressState>,
    /** Whether the entry's chapter filters keep an episode in its list. */
    val isShown: (EntryChapter) -> Boolean,
    val chapterGroups: List<EntryChapterGroup>,
)

private data class ServerFallbackRetry(val selection: PlaybackSelection, val notice: ServerFallbackNotice)

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

private fun ResolveVideoStream.Result.Success.toPreview(selection: PlaybackSelection) = VideoPlaybackPreviewState(
    selection = selection,
    playbackData = playbackData,
    subtitles = stream.subtitles,
)

private fun PlaybackSelection.normalized(): PlaybackSelection {
    return copy(streamKey = null)
}
