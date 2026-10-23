package eu.kanade.tachiyomi.ui.video.player.screen

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.text.Cue
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerContentOverlay
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSideGestureType
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.applyAdaptiveQuality
import eu.kanade.tachiyomi.ui.video.player.applyAudioTrack
import eu.kanade.tachiyomi.ui.video.player.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEndScreen
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEpisodeChangeError
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerGestureGuide
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettings
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsContent
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSpeedBoostPill
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSwitchingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerSafeContentPadding
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerTouchTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekPosition
import eu.kanade.tachiyomi.ui.video.player.playback.EpisodeNavigationPlayer
import eu.kanade.tachiyomi.ui.video.player.rotatedVideoPlayerOrientation
import eu.kanade.tachiyomi.ui.video.player.subtitleChoiceKey
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewCues
import eu.kanade.tachiyomi.ui.video.player.subtitlePreviewText
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedLabel
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import kotlin.math.ceil

private const val TEMPORARY_SPEED_BOOST = 2f
private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
private const val PLAYBACK_SNAPSHOT_INTERVAL_MS = 250L
private const val PAUSED_PLAYBACK_SNAPSHOT_INTERVAL_MS = 750L

/**
 * The screen while an episode plays: the video and its gestures, the controls, the end screen, the settings and the
 * subtitle editor, and the overlays for loading, switching and errors. It attaches each new episode's player to the
 * activity's playback, picture-in-picture and background owners in [host].
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
    val background = host.background
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is VideoPlayerViewModel.Event.ShowProblem -> context.toast(event.problem.cause.title)
            }
        }
    }
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
    var playerProblem by remember(
        current.chapterId,
        current.streamUrl,
        current.playbackRevision,
        subtitlePayloadKey,
    ) {
        mutableStateOf<VideoPlayerProblem?>(null)
    }
    val nextEpisode = rememberVideoPlayerNextEpisodeState(current.chapterId)
    val temporarySpeedBoost = remember { mutableStateOf(false) }
    var temporarySpeedBoostActive by temporarySpeedBoost
    val isInPictureInPictureMode = pictureInPicture.isInPictureInPictureMode
    val shouldHideChromeForSeekFeedback = ui.seekFeedbackState?.hidePlayerChrome == true
    val hidePlayerChrome = shouldHideChromeForSeekFeedback || isInPictureInPictureMode
    val nextEpisodeAvailable = current.nextChapterId != null
    val endScreenVisible = ui.playbackSnapshot.playbackEnded &&
        nextEpisodeAvailable &&
        !nextEpisode.endScreenDismissed &&
        !nextEpisode.advanceInFlight &&
        !isInPictureInPictureMode &&
        !ui.subtitleEditorVisible
    val effectivePlaybackSpeed = if (temporarySpeedBoostActive) {
        TEMPORARY_SPEED_BOOST
    } else {
        current.playback.sessionPlaybackSpeed
    }
    val onPreviousEpisode = {
        temporarySpeedBoostActive = false
        ui.controlsVisible = !visit.controlsLocked
        ui.settingsVisible = false
        ui.episodesDrawerVisible = false
        ui.subtitleEditorVisible = false
        ui.isScrubbing = false
        nextEpisode.advanceInFlight = true
        ui.controllerInteractionSequence += 1L
        playback.flush()
        viewModel.playPreviousEpisode()
    }
    val onNextEpisode = {
        if (!nextEpisode.advanceInFlight && nextEpisodeAvailable) {
            temporarySpeedBoostActive = false
            nextEpisode.advanceInFlight = true
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
    SideEffect { background.onNextEpisode = onNextEpisode }
    val latestPlayback = rememberUpdatedState(current.playback)
    val latestTemporarySpeedBoostActive = rememberUpdatedState(temporarySpeedBoostActive)
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
        onProblem = { playerProblem = it },
    )
    val controllerPlayer = remember(
        currentPlayer,
        current.previousChapterId,
        current.nextChapterId,
    ) {
        EpisodeNavigationPlayer(
            player = currentPlayer,
            hasPreviousEpisode = current.previousChapterId != null,
            hasNextEpisode = current.nextChapterId != null,
            onPreviousEpisode = onPreviousEpisode,
            onNextEpisode = onNextEpisode,
        )
    }

    val registerControllerInteraction: (Boolean) -> Unit = ui::registerControllerInteraction
    val openSettings: (VideoPlayerSettingsSection?) -> Unit = { section ->
        temporarySpeedBoostActive = false
        ui.episodesDrawerVisible = false
        settingsSection = section
        ui.settingsVisible = true
        registerControllerInteraction(true)
    }
    val triggerSeekFeedback: (VideoPlayerSeekDirection, Boolean) -> Unit = { direction, hidePlayerChrome ->
        ui.triggerSeekFeedback(direction, seekStepSeconds, hidePlayerChrome)
    }
    val applyBrightnessLevel: (Int) -> Unit = { level ->
        ui.triggerSideGestureFeedback(
            VideoPlayerSideGestureType.Brightness,
            brightness.adjust(level),
            VideoPlayerBrightness.MAX_LEVEL,
        )
    }
    val applyVolumeLevel: (Int) -> Unit = { level ->
        val maxLevel = volume.maxLevel.coerceAtLeast(1)
        ui.triggerSideGestureFeedback(
            VideoPlayerSideGestureType.Volume,
            volume.adjust(currentPlayer, level),
            maxLevel,
        )
    }
    val seekInDirection: (VideoPlayerSeekDirection) -> Unit = { direction ->
        val durationMs = ui.playbackSnapshot.durationMs.takeIf { it > 0L }
            ?: currentPlayer.duration.coerceAtLeast(0L)
        val basePositionMs = if (ui.isScrubbing) ui.scrubPositionMs else currentPlayer.currentPosition
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
    }
    val performGestureSeek: (VideoPlayerSeekDirection) -> Unit = { direction ->
        ui.controlsVisible = false
        registerControllerInteraction(false)
        seekInDirection(direction)
        triggerSeekFeedback(direction, true)
    }
    val togglePlayback = {
        registerControllerInteraction(true)
        if (ui.playbackSnapshot.playbackEnded) {
            currentPlayer.seekTo(0L)
        }
        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
        } else {
            currentPlayer.play()
        }
    }

    LaunchedEffect(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        playerProblem = null
        ui.startupOverlayVisible = true
        ui.settingsVisible = false
        ui.episodesDrawerVisible = false
        ui.subtitleEditorVisible = false
        ui.subtitleEditorDraft = current.playback.subtitleAppearance
        ui.resumePlaybackAfterSubtitleEditor = false
        temporarySpeedBoostActive = false
        nextEpisode.countdownElapsedMs = 0L
        nextEpisode.endScreenDismissed = false
        nextEpisode.advanceInFlight = false
        ui.controlsVisible = !isInPictureInPictureMode && !visit.controlsLocked
        ui.isScrubbing = false
        ui.ignoreNextGestureSeekTapUp = false
        ui.seekFeedbackState = null
        ui.sideGestureFeedbackState = null
        brightness.reapply()
        volume.sync(currentPlayer)
        ui.scrubPositionMs = current.resumePositionMs.coerceAtLeast(0L)
        ui.playbackSnapshot = VideoPlayerPlaybackSnapshot(positionMs = ui.scrubPositionMs)
        ui.controllerInteractionSequence += 1L
        playback.attach(currentPlayer)
        background.attach(currentPlayer, seekStepSeconds, hasNextEpisode = current.nextChapterId != null)
        currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
        currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
        currentPlayer.playWhenReady = true
        currentPlayer.prepare()
        publishSnapshot(currentPlayer.capturePlaybackSnapshot())
    }

    // After the effect above, which starts every new player playing.
    LaunchedEffect(currentPlayer, visit.pendingPausedStartMs) {
        val positionMs = visit.pendingPausedStartMs ?: return@LaunchedEffect
        visit.pendingPausedStartMs = null
        currentPlayer.playWhenReady = false
        currentPlayer.seekTo(positionMs)
    }

    LaunchedEffect(current.playback.currentAdaptiveQuality, currentPlayer) {
        currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
    }

    LaunchedEffect(current.playback.currentSubtitle, currentPlayer) {
        currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
    }

    LaunchedEffect(current.playback.currentAudioTrackKey, currentPlayer) {
        currentPlayer.applyAudioTrack(current.playback.currentAudioTrackKey)
    }

    LaunchedEffect(effectivePlaybackSpeed, currentPlayer) {
        currentPlayer.setPlaybackSpeed(effectivePlaybackSpeed)
    }

    LaunchedEffect(currentPlayer) {
        while (isActive) {
            publishSnapshot(currentPlayer.capturePlaybackSnapshot())
            if (!ui.isScrubbing) {
                ui.scrubPositionMs = ui.playbackSnapshot.positionMs
            }
            delay(
                if (ui.playbackSnapshot.isPlaying || ui.playbackSnapshot.isLoading || ui.isScrubbing) {
                    PLAYBACK_SNAPSHOT_INTERVAL_MS
                } else {
                    PAUSED_PLAYBACK_SNAPSHOT_INTERVAL_MS
                },
            )
        }
    }
    val seekPreview = rememberVideoPlayerSeekPreviewSession(
        current = current,
        subtitlePayloadKey = subtitlePayloadKey,
        enabled = seekPreviewEnabled,
        host = host,
    )

    LaunchedEffect(
        ui.controlsVisible,
        ui.controllerInteractionSequence,
        ui.playbackSnapshot.isPlaying,
        ui.playbackSnapshot.isLoading,
        ui.isScrubbing,
        ui.settingsVisible,
        ui.episodesDrawerVisible,
        ui.startupOverlayVisible,
    ) {
        if (
            !ui.controlsVisible ||
            !ui.playbackSnapshot.isPlaying ||
            ui.playbackSnapshot.isLoading ||
            ui.isScrubbing ||
            ui.settingsVisible ||
            ui.episodesDrawerVisible ||
            ui.startupOverlayVisible
        ) {
            return@LaunchedEffect
        }

        delay(CONTROLS_AUTO_HIDE_DELAY_MS)
        ui.controlsVisible = false
    }

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
    val latestSettingsVisible = rememberUpdatedState(ui.settingsVisible)
    val latestControlsVisible = rememberUpdatedState(ui.controlsVisible)
    val latestControlsLocked = rememberUpdatedState(visit.controlsLocked)
    val latestPerformGestureSeek = rememberUpdatedState(performGestureSeek)
    val latestSeekGestureModeActive = rememberUpdatedState(shouldHideChromeForSeekFeedback)
    val latestSpeedBoostEligibility = rememberUpdatedState(
        !ui.controlsVisible &&
            !visit.controlsLocked &&
            !ui.settingsVisible &&
            !ui.subtitleEditorVisible &&
            !shouldHideChromeForSeekFeedback,
    )
    val latestApplyBrightnessLevel = rememberUpdatedState(applyBrightnessLevel)
    val latestApplyVolumeLevel = rememberUpdatedState(applyVolumeLevel)
    val touchTarget = remember(ui) {
        object : VideoPlayerTouchTarget {
            override val subtitleEditorVisible get() = ui.subtitleEditorVisible
            override val controlsLocked get() = latestControlsLocked.value
            override val controlsVisible get() = latestControlsVisible.value
            override val settingsVisible get() = latestSettingsVisible.value
            override val seekGestureModeActive get() = latestSeekGestureModeActive.value
            override val speedBoostEligible get() = latestSpeedBoostEligibility.value
            override val speedBoostActive get() = latestTemporarySpeedBoostActive.value
            override var ignoreNextSeekTapUp
                get() = ui.ignoreNextGestureSeekTapUp
                set(value) {
                    ui.ignoreNextGestureSeekTapUp = value
                }

            override fun setSpeedBoostActive(active: Boolean) {
                temporarySpeedBoost.value = active
            }

            override fun hideControls() {
                ui.controlsVisible = false
            }

            override fun registerControllerInteraction(showControls: Boolean) {
                ui.registerControllerInteraction(showControls)
            }

            override fun performGestureSeek(direction: VideoPlayerSeekDirection) {
                latestPerformGestureSeek.value(direction)
            }

            override fun sideGestureLevel(type: VideoPlayerSideGestureType): Int = when (type) {
                VideoPlayerSideGestureType.Brightness -> brightness.level
                VideoPlayerSideGestureType.Volume -> volume.level
            }

            override fun sideGestureMaxLevel(type: VideoPlayerSideGestureType): Int = when (type) {
                VideoPlayerSideGestureType.Brightness -> VideoPlayerBrightness.MAX_LEVEL
                VideoPlayerSideGestureType.Volume -> volume.maxLevel.coerceAtLeast(1)
            }

            override fun applySideGestureLevel(type: VideoPlayerSideGestureType, level: Int) {
                when (type) {
                    VideoPlayerSideGestureType.Brightness -> latestApplyBrightnessLevel.value(level)
                    VideoPlayerSideGestureType.Volume -> latestApplyVolumeLevel.value(level)
                }
            }
        }
    }
    BackHandler(enabled = ui.episodesDrawerVisible) {
        ui.episodesDrawerVisible = false
        registerControllerInteraction(true)
    }
    val dismissSettings = {
        temporarySpeedBoostActive = false
        ui.settingsVisible = false
        registerControllerInteraction(true)
    }
    // The landscape side panel is drawn in the activity's window; the portrait sheet handles back itself.
    BackHandler(enabled = ui.settingsVisible, onBack = dismissSettings)
    val actions = VideoPlayerWatchActions(
        previousEpisode = onPreviousEpisode,
        nextEpisode = onNextEpisode,
        openSettings = openSettings,
        dismissSettings = dismissSettings,
        togglePlayback = togglePlayback,
        seekFromControls = { direction ->
            registerControllerInteraction(true)
            seekInDirection(direction)
            triggerSeekFeedback(direction, false)
        },
        endSpeedBoost = { temporarySpeedBoostActive = false },
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
            nextEpisode.advanceInFlight = false
            currentPlayer.pause()
        }
    }
    nextEpisode.Effects(
        ui = ui,
        current = current,
        viewModel = viewModel,
        autoPlayNext = autoPlayNext,
        endScreenVisible = endScreenVisible,
        isInPictureInPictureMode = isInPictureInPictureMode,
        onNextEpisode = onNextEpisode,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceContainer.Media),
    ) {
        VideoPlayerSurface(
            player = controllerPlayer,
            current = current,
            ui = ui,
            touchTarget = touchTarget,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceContainer.Media),
        )

        VideoPlayerContentOverlay(
            brightness = brightness.overlayValue,
            color = null,
            colorBlendMode = null,
            modifier = Modifier.fillMaxSize(),
        )

        nextEpisode.EndScreen(
            visible = endScreenVisible,
            nextEpisodeName = current.childWindow.next?.name.orEmpty(),
            autoPlayNext = autoPlayNext,
            onPlayNext = onNextEpisode,
        )

        VideoPlayerControlsLayer(
            current = current,
            ui = ui,
            seekPreview = seekPreview,
            currentPlayer = currentPlayer,
            host = host,
            viewModel = viewModel,
            actions = actions,
            isInPictureInPictureMode = isInPictureInPictureMode,
            endScreenVisible = endScreenVisible,
            displayedPositionMs = displayedPositionMs,
            hidePlayerChrome = hidePlayerChrome,
            seekStepSeconds = seekStepSeconds,
        )

        VideoPlayerStatusOverlays(
            current = current,
            ui = ui,
            host = host,
            viewModel = viewModel,
            actions = actions,
            playerProblem = playerProblem,
            temporarySpeedBoost = if (temporarySpeedBoostActive) TEMPORARY_SPEED_BOOST else null,
            isInPictureInPictureMode = isInPictureInPictureMode,
            endScreenVisible = endScreenVisible,
            seekStepSeconds = seekStepSeconds,
        )

        VideoPlayerSettingsLayer(
            current = current,
            ui = ui,
            host = host,
            viewModel = viewModel,
            actions = actions,
            currentPlayer = currentPlayer,
            settingsSection = settingsSection,
            isInPictureInPictureMode = isInPictureInPictureMode,
        )

        VideoPlayerSubtitleEditorLayer(
            ui = ui,
            viewModel = viewModel,
            actions = actions,
            currentPlayer = currentPlayer,
            isInPictureInPictureMode = isInPictureInPictureMode,
        )
    }
}
