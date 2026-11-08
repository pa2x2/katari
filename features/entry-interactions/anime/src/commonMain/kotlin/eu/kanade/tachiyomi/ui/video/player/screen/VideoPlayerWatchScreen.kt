package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerContentOverlay
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEndScreen
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEpisodeChangeError
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettings
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsContent
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSpeedBoostPill
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSwitchingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerSafeContentPadding
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerWindowTitle
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekPosition
import eu.kanade.tachiyomi.ui.video.player.introtimes.Effect
import eu.kanade.tachiyomi.ui.video.player.introtimes.introTimesServices
import eu.kanade.tachiyomi.ui.video.player.introtimes.onlineMarkers
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPictureScene
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import eu.kanade.tachiyomi.ui.video.player.posture.VideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.posture.currentVideoPlayerPosture
import eu.kanade.tachiyomi.ui.video.player.rotatedVideoPlayerOrientation
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.tracks.applyAdaptiveQuality
import eu.kanade.tachiyomi.ui.video.player.tracks.applyAudioTrack
import eu.kanade.tachiyomi.ui.video.player.tracks.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedLabel
import mihon.entry.interactions.anime.introtimes.IntroTimesService
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.viewer.chrome.ui.handoff.ProgressOfferNote
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.tracks.subtitleChoiceKey
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.math.ceil

private const val TEMPORARY_SPEED_BOOST = 2f

/**
 * The screen while an episode plays: the video and its input, the controls, the end screen, the settings and the
 * subtitle editor, and the overlays for loading, switching and errors. It attaches each new episode's player to the
 * host's playback, picture-in-picture and media controls.
 */
@Composable
internal fun VideoPlayerWatchScreen(
    current: VideoPlayerViewModel.State.Ready,
    viewModel: VideoPlayerViewModel,
    host: VideoPlayerScreenHost,
) {
    val visit = host.visit
    val playback = host.playback
    val pictureInPicture = host.pictureInPicture
    val brightness = host.brightness
    val volume = host.volume
    val mediaControls = host.mediaControls
    val resolvedSeekPreview by host.settings.seekPreview.state.collectAsState()
    val seekPreviewEnabled = resolvedSeekPreview.effectiveValue
    val resolvedSeekStep by host.settings.seekStep.state.collectAsState()
    val seekStepSeconds = resolvedSeekStep.effectiveValue
    val resolvedAutoPlayNext by host.settings.autoPlayNext.state.collectAsState()
    val autoPlayNext = resolvedAutoPlayNext.effectiveValue

    val subtitlePayloadKey = remember(current.playback.subtitles) {
        current.playback.subtitles.joinToString(separator = "||") { subtitleChoiceKey(it) }
    }
    val ui = remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
        VideoPlayerWatchUiState(
            subtitleAppearance = current.playback.subtitleAppearance,
            resumePositionMs = current.resumePositionMs,
        )
    }
    var settingsSection by remember { mutableStateOf<VideoPlayerSettingsSection?>(null) }
    val engine = rememberWatchedPlayerEngine(current, subtitlePayloadKey, host, viewModel)
    engine.NoticeEffects()
    val nextEpisode = rememberVideoPlayerNextEpisodeState(current.chapterId)
    val temporarySpeedBoost = remember { mutableStateOf(false) }
    var temporarySpeedBoostActive by temporarySpeedBoost
    val isInPictureInPictureMode = pictureInPicture.isInPictureInPictureMode
    // A picture-in-picture window of the player's own draws the picture, subtitles and end screen this hands it
    val inOwnWindow = isInPictureInPictureMode && pictureInPicture.drawsOwnWindow
    val posture = currentVideoPlayerPosture()
    // Half-open on a table the controls have their own half, so nothing that keeps the picture clear hides them
    val controlsPinned = posture is VideoPlayerPosture.Tabletop
    LaunchedEffect(ui, controlsPinned) {
        if (controlsPinned) ui.controlsVisible = true
    }
    val shouldHideChromeForSeekFeedback = ui.seekFeedbackState?.hidePlayerChrome == true
    val hidePlayerChrome = (shouldHideChromeForSeekFeedback && !controlsPinned) || isInPictureInPictureMode
    val nextEpisodeAvailable = current.nextChapterId != null
    val episodeOpening = current.episodeChange is VideoPlayerViewModel.EpisodeChange.Loading
    val endScreenVisible = ui.playbackSnapshot.playbackEnded &&
        nextEpisodeAvailable &&
        !nextEpisode.endScreenDismissed &&
        !episodeOpening &&
        (!isInPictureInPictureMode || pictureInPicture.drawsOwnWindow) &&
        !ui.subtitleEditorVisible
    val effectivePlaybackSpeed = if (temporarySpeedBoostActive) {
        TEMPORARY_SPEED_BOOST
    } else {
        current.playback.sessionPlaybackSpeed
    }
    val onPreviousEpisode = {
        if (current.canPlayPrevious) {
            temporarySpeedBoostActive = false
            ui.controlsVisible = !visit.controlsLocked
            ui.settingsVisible = false
            ui.episodesDrawerVisible = false
            ui.subtitleEditorVisible = false
            ui.isScrubbing = false
            nextEpisode.leaveEpisodeEnd(ui.playbackSnapshot.playbackEnded)
            ui.controllerInteractionSequence += 1L
            playback.flush()
            viewModel.playPreviousEpisode()
        }
    }
    val onNextEpisode = {
        if (current.canPlayNext) {
            temporarySpeedBoostActive = false
            nextEpisode.leaveEpisodeEnd(ui.playbackSnapshot.playbackEnded)
            ui.controlsVisible = !visit.controlsLocked
            ui.settingsVisible = false
            ui.episodesDrawerVisible = false
            ui.subtitleEditorVisible = false
            ui.isScrubbing = false
            ui.controllerInteractionSequence += 1L
            playback.flush()
            viewModel.playNextEpisode()
        }
    }
    SideEffect {
        mediaControls.onNextEpisode = onNextEpisode
        mediaControls.onPreviousEpisode = onPreviousEpisode
        mediaControls.setEpisodeNavigation(
            canPlayPrevious = current.canPlayPrevious,
            canPlayNext = current.canPlayNext,
        )
        pictureInPicture.setEpisodeNavigation(
            onPreviousEpisode = onPreviousEpisode.takeIf { current.canPlayPrevious },
            onNextEpisode = onNextEpisode.takeIf { current.canPlayNext },
        )
    }
    val latestPlayback = rememberUpdatedState(current.playback)
    val publishSnapshot: (VideoPlayerPlaybackSnapshot) -> Unit = { snapshot ->
        ui.playbackSnapshot = snapshot
        pictureInPicture.onPlaybackSnapshot(snapshot)
    }
    val currentPlayer = rememberWatchedPlayer(
        current = current,
        subtitlePayloadKey = subtitlePayloadKey,
        seekStepSeconds = seekStepSeconds,
        host = host,
        viewModel = viewModel,
        ui = ui,
        latestPlayback = latestPlayback,
        publishSnapshot = publishSnapshot,
        engine = engine,
    )

    val registerControllerInteraction: (Boolean) -> Unit = ui::registerControllerInteraction
    val openSettings: (VideoPlayerSettingsSection?) -> Unit = { section ->
        temporarySpeedBoostActive = false
        ui.episodesDrawerVisible = false
        settingsSection = section
        ui.settingsVisible = true
        registerControllerInteraction(true)
    }
    val seekInDirection: (VideoPlayerSeekDirection, Boolean) -> Unit = { direction, hidePlayerChrome ->
        val durationMs = ui.playbackSnapshot.durationMs.takeIf { it > 0L }
            ?: currentPlayer.state.value.durationMs?.coerceAtLeast(0L)
            ?: 0L
        val basePositionMs = if (ui.isScrubbing) ui.scrubPositionMs else currentPlayer.currentPositionMs
        val targetPositionMs = resolveVideoPlayerSeekPosition(
            positionMs = basePositionMs,
            durationMs = durationMs,
            direction = direction,
            stepSeconds = seekStepSeconds,
        )

        ui.isScrubbing = false
        ui.scrubPositionMs = targetPositionMs
        ui.playbackSnapshot = ui.playbackSnapshot.copy(positionMs = targetPositionMs)
        currentPlayer.seekTo(targetPositionMs)
        ui.triggerSeekFeedback(direction, abs(targetPositionMs - basePositionMs), hidePlayerChrome)
    }
    val performGestureSeek: (VideoPlayerSeekDirection) -> Unit = { direction ->
        if (!controlsPinned) ui.controlsVisible = false
        registerControllerInteraction(false)
        seekInDirection(direction, true)
    }
    val togglePlayback = {
        registerControllerInteraction(true)
        if (ui.playbackSnapshot.playbackEnded) {
            currentPlayer.seekTo(0L)
        }
        if (currentPlayer.state.value.isPlaying) {
            currentPlayer.pause()
        } else {
            currentPlayer.play()
        }
    }

    val markerSkipSetting = rememberMarkerSkipSetting(host, current.ownerEntryId)
    val markerSkipMode = markerSkipSetting?.let { it.state.collectAsState().value.effectiveValue }
    val markerSkip = rememberVideoPlayerMarkerSkip(current.chapterId)
    val findsIntroTimesOnline = host.settings.findIntroTimesOnline.state.collectAsState().value.effectiveValue
    val fromAniSkip = host.settings.introTimesFromAniSkip.state.collectAsState().value.effectiveValue
    val fromTheIntroDb = host.settings.introTimesFromTheIntroDb.state.collectAsState().value.effectiveValue
    val introTimesServices = remember(findsIntroTimesOnline, fromAniSkip, fromTheIntroDb, current.stream.markers) {
        introTimesServices(
            settingOn = findsIntroTimesOnline,
            turnedOn = buildSet {
                if (fromAniSkip) add(IntroTimesService.ANISKIP)
                if (fromTheIntroDb) add(IntroTimesService.THEINTRODB)
            },
            streamMarkers = current.stream.markers,
        )
    }
    val introTimes by viewModel.introTimes.state.collectAsState()
    val onlineMarkers = remember(current.chapterId, introTimesServices, introTimes) {
        onlineMarkers(current.chapterId, introTimesServices, introTimes)
    }
    val markers = remember(current.stream.markers, onlineMarkers) {
        current.stream.markers + onlineMarkers.map { it.marker }
    }
    // Moving on to the next episode waits while another opens, so its offer at the credits steps aside meanwhile.
    val skipOffer = markerSkip.offerAt(
        mode = markerSkipMode,
        positionMs = ui.playbackSnapshot.positionMs,
        durationMs = ui.playbackSnapshot.durationMs,
        markers = markers,
        hasNextEpisode = nextEpisodeAvailable,
    )?.takeUnless { it.goesToNextEpisode && episodeOpening }
    val chapters = rememberVideoPlayerChapters(currentPlayer, markers, ui.playbackSnapshot.durationMs)
    val seekToPosition: (Long) -> Unit = { positionMs ->
        ui.scrubPositionMs = positionMs
        ui.playbackSnapshot = ui.playbackSnapshot.copy(positionMs = positionMs)
        currentPlayer.seekTo(positionMs)
    }
    val skipCountdown = markerSkip.countdownFor(markerSkipMode, skipOffer)
    markerSkip.Effects(mode = markerSkipMode, offer = skipOffer, ui = ui, seekTo = seekToPosition)
    val skip = {
        when {
            skipOffer == null -> Unit
            skipOffer.goesToNextEpisode -> onNextEpisode()
            else -> seekToPosition(skipOffer.targetMs)
        }
    }

    LaunchedEffect(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        engine.problem = null
        ui.startupOverlayVisible = true
        ui.settingsVisible = false
        ui.episodesDrawerVisible = false
        ui.subtitleEditorVisible = false
        ui.subtitleEditorDraft = current.playback.subtitleAppearance
        ui.resumePlaybackAfterSubtitleEditor = false
        temporarySpeedBoostActive = false
        nextEpisode.countdownElapsedMs = 0L
        nextEpisode.endScreenDismissed = false
        ui.controlsVisible = !isInPictureInPictureMode && !visit.controlsLocked
        ui.isScrubbing = false
        ui.ignoreNextGestureSeekTapUp = false
        ui.seekFeedbackState = null
        ui.sideGestureFeedbackState = null
        brightness?.reapply()
        volume.sync(currentPlayer)
        ui.scrubPositionMs = current.resumePositionMs.coerceAtLeast(0L)
        ui.playbackSnapshot = VideoPlayerPlaybackSnapshot(positionMs = ui.scrubPositionMs)
        ui.controllerInteractionSequence += 1L
        playback.attach(currentPlayer)
        mediaControls.attach(
            player = currentPlayer,
            metadata = VideoMetadata(
                title = current.chapterName,
                series = current.entryTitle,
                episodePreviewUrl = current.childWindow.current.previewUrl,
                cover = current.entry.asEntryCover(),
            ),
            seekStepSeconds = seekStepSeconds,
        )
        currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
        currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
        currentPlayer.setPlayWhenReady(true)
        currentPlayer.prepare()
        publishSnapshot(currentPlayer.capturePlaybackSnapshot())
    }

    // After the effect above, which starts every new player playing.
    LaunchedEffect(currentPlayer, visit.pendingPausedStartMs) {
        val positionMs = visit.pendingPausedStartMs ?: return@LaunchedEffect
        visit.pendingPausedStartMs = null
        currentPlayer.setPlayWhenReady(false)
        currentPlayer.seekTo(positionMs)
    }

    viewModel.introTimes.Effect(current.chapterId, introTimesServices) { ui.playbackSnapshot.durationMs }

    LaunchedEffect(current.playback.currentAdaptiveQuality, currentPlayer) {
        currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
    }

    LaunchedEffect(current.playback.currentSubtitle, currentPlayer) {
        currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
    }

    LaunchedEffect(current.playback.currentAudioTrackKey, currentPlayer) {
        currentPlayer.applyAudioTrack(current.playback.currentAudioTrackKey, current.stream.audioTracks)
    }

    LaunchedEffect(effectivePlaybackSpeed, currentPlayer) {
        currentPlayer.setSpeed(effectivePlaybackSpeed)
    }

    val seekPreview = rememberVideoPlayerSeekPreviewSession(
        current = current,
        subtitlePayloadKey = subtitlePayloadKey,
        enabled = seekPreviewEnabled,
        host = host,
    )
    val scrub = rememberVideoPlayerScrub(ui, seekPreview, pictureInPicture, currentPlayer)

    VideoPlayerControlsAutoHide(ui, controlsPinned)

    val displayedPositionMs = if (ui.isScrubbing) {
        ui.scrubPositionMs.coerceToPlaybackDuration(ui.playbackSnapshot.durationMs)
    } else {
        ui.playbackSnapshot.positionMs
    }
    seekPreview.FrameRequestsEffect(
        ui = ui,
        visit = visit,
        isInPictureInPictureMode = isInPictureInPictureMode,
        isSourceSwitching = current.isSourceSwitching,
    )
    LaunchedEffect(ui, current.playback.pictureFraming) {
        ui.picturePreview = null
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = ui.episodesDrawerVisible,
        onBackCompleted = {
            ui.episodesDrawerVisible = false
            registerControllerInteraction(true)
        },
    )
    val dismissSettings = {
        temporarySpeedBoostActive = false
        ui.settingsVisible = false
        registerControllerInteraction(true)
    }
    // The landscape side panel is drawn in the player's window; the portrait sheet handles back itself.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = ui.settingsVisible,
        onBackCompleted = dismissSettings,
    )
    val actions = VideoPlayerWatchActions(
        previousEpisode = onPreviousEpisode,
        nextEpisode = onNextEpisode,
        openSettings = openSettings,
        dismissSettings = dismissSettings,
        togglePlayback = togglePlayback,
        seekFromControls = { direction ->
            registerControllerInteraction(true)
            seekInDirection(direction, false)
        },
        gestureSeek = performGestureSeek,
        seekStep = { direction ->
            registerControllerInteraction(false)
            seekInDirection(direction, false)
        },
        toggleFullScreen = {
            host.fullScreen?.let { if (it.isFullScreen) it.leave() else it.enter() }
            registerControllerInteraction(false)
        },
        openEpisodes = {
            temporarySpeedBoostActive = false
            ui.settingsVisible = false
            ui.episodesDrawerVisible = true
            registerControllerInteraction(true)
        },
        skip = skip,
        endSpeedBoost = { temporarySpeedBoostActive = false },
        leaveEpisodeEnd = { nextEpisode.leaveEpisodeEnd(ui.playbackSnapshot.playbackEnded) },
    )
    val inputTarget = rememberVideoPlayerWatchInputTarget(
        ui = ui,
        host = host,
        viewModel = viewModel,
        speedBoost = temporarySpeedBoost,
        current = current,
        player = currentPlayer,
        scrub = scrub,
        actions = actions,
        controlsPinned = controlsPinned,
        seekGestureModeActive = shouldHideChromeForSeekFeedback,
        skipOffered = skipOffer != null,
        seekStepSeconds = seekStepSeconds,
    )
    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            temporarySpeedBoostActive = false
            ui.controlsVisible = false
            ui.settingsVisible = false
            ui.episodesDrawerVisible = false
            ui.subtitleEditorVisible = false
            ui.isScrubbing = false
            ui.ignoreNextGestureSeekTapUp = false
            ui.seekFeedbackState = null
            ui.sideGestureFeedbackState = null
        } else {
            host.onHideSystemUi()
        }
    }
    LaunchedEffect(current.episodeChange is VideoPlayerViewModel.EpisodeChange.Failed) {
        if (current.episodeChange is VideoPlayerViewModel.EpisodeChange.Failed) {
            // The episode stays; it waits for Retry instead of running on under the error.
            currentPlayer.pause()
        }
    }
    nextEpisode.Effects(
        ui = ui,
        current = current,
        viewModel = viewModel,
        autoPlayNext = autoPlayNext,
        endScreenVisible = endScreenVisible,
        playsNextAtEnd = isInPictureInPictureMode && !pictureInPicture.drawsOwnWindow,
        onNextEpisode = onNextEpisode,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceContainer.Media),
    ) {
        // Half-open on a table the picture, and the gestures on it, keep to the upper half
        val pictureArea = when (posture) {
            is VideoPlayerPosture.Tabletop -> Modifier.fillMaxWidth().height(posture.pictureBottom)
            else -> Modifier.fillMaxSize()
        }
        if (!inOwnWindow) {
            host.input.Picture(
                player = currentPlayer,
                target = inputTarget,
                modifier = pictureArea.background(SurfaceContainer.Media),
            )

            VideoPlayerSubtitles(
                player = currentPlayer,
                current = current,
                ui = ui,
                aspectRatio = inputTarget.videoAspectRatio,
                framing = inputTarget.pictureFraming,
                keepSubtitlesInPicture = visit.subtitlesInPicture,
                modifier = pictureArea,
            )
        }

        VideoPlayerContentOverlay(
            brightness = brightness?.overlayValue ?: 0,
            color = null,
            colorBlendMode = null,
            modifier = pictureArea,
        )

        if (inOwnWindow) {
            pictureInPicture.Window(
                VideoPlayerPictureInPictureScene(
                    player = currentPlayer,
                    input = inputTarget,
                    snapshot = ui.playbackSnapshot,
                    title = videoPlayerWindowTitle(current.entryTitle, current.memberTitle, current.chapterName),
                    endScreenVisible = endScreenVisible,
                    subtitles = { modifier ->
                        VideoPlayerSubtitles(
                            player = currentPlayer,
                            current = current,
                            ui = ui,
                            aspectRatio = inputTarget.videoAspectRatio,
                            framing = PictureFraming.Fit,
                            keepSubtitlesInPicture = visit.subtitlesInPicture,
                            modifier = modifier,
                        )
                    },
                    endScreen = { modifier ->
                        nextEpisode.EndScreen(
                            visible = endScreenVisible,
                            nextEpisodeName = current.childWindow.next?.name.orEmpty(),
                            autoPlayNext = autoPlayNext,
                            onPlayNext = onNextEpisode,
                            modifier = modifier,
                            compact = true,
                        )
                    },
                ),
            )
        } else {
            nextEpisode.EndScreen(
                visible = endScreenVisible,
                nextEpisodeName = current.childWindow.next?.name.orEmpty(),
                autoPlayNext = autoPlayNext,
                onPlayNext = onNextEpisode,
            )
        }

        VideoPlayerControlsLayer(
            current = current,
            ui = ui,
            seekPreview = seekPreview,
            scrub = scrub,
            host = host,
            viewModel = viewModel,
            actions = actions,
            isInPictureInPictureMode = isInPictureInPictureMode,
            endScreenVisible = endScreenVisible,
            displayedPositionMs = displayedPositionMs,
            hidePlayerChrome = hidePlayerChrome,
            seekStepSeconds = seekStepSeconds,
            markers = markers,
            onlineMarkers = onlineMarkers,
            chapters = chapters,
            skipOffer = skipOffer,
            skipCountdown = skipCountdown,
            onSkip = skip,
        )

        VideoPlayerStatusOverlays(
            current = current,
            ui = ui,
            host = host,
            viewModel = viewModel,
            actions = actions,
            engine = engine,
            temporarySpeedBoost = if (temporarySpeedBoostActive) TEMPORARY_SPEED_BOOST else null,
            isInPictureInPictureMode = isInPictureInPictureMode,
            endScreenVisible = endScreenVisible,
            seekStepSeconds = seekStepSeconds,
            markerSkip = markerSkip,
            onUndoSkip = seekToPosition,
        )

        val progressOffer by viewModel.progressOffers.offer.collectAsState()
        progressOffer?.takeIf { !isInPictureInPictureMode && !inOwnWindow }?.let { offer ->
            ProgressOfferNote(
                deviceName = offer.deviceName,
                place = stringResource(
                    MR.strings.sync_handoff_position_time,
                    offer.episodeName,
                    formatPlaybackTimestamp(offer.positionMs),
                ),
                onGoThere = { viewModel.goToProgressOffer()?.let(seekToPosition) },
                onDismiss = viewModel.progressOffers::dismiss,
                // At the top, clear of the controls and subtitles at the bottom
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(KatariTheme.spacing.l),
            )
        }

        VideoPlayerSettingsLayer(
            current = current,
            ui = ui,
            host = host,
            viewModel = viewModel,
            actions = actions,
            currentPlayer = currentPlayer,
            settingsSection = settingsSection,
            isInPictureInPictureMode = isInPictureInPictureMode,
            engine = engine,
            markerSkipSetting = markerSkipSetting,
            markers = markers,
            introTimes = introTimes?.takeIf { introTimesServices.isNotEmpty() && it.chapterId == current.chapterId },
            chapters = chapters,
            onSeek = seekToPosition,
        )

        VideoPlayerSubtitleEditorLayer(
            current = current,
            ui = ui,
            host = host,
            viewModel = viewModel,
            actions = actions,
            currentPlayer = currentPlayer,
            isInPictureInPictureMode = isInPictureInPictureMode,
        )
    }
}
