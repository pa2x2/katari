package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import eu.kanade.tachiyomi.ui.video.player.defaultSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.externalSubtitleOptions
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerInputTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSideGestureType
import eu.kanade.tachiyomi.ui.video.player.steppedPlaybackSpeed
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The watch screen as its input sees it. The screen hands it what it is composing on every composition, through
 * [update], so each touch, click or key acts on the screen as it is then.
 */
internal class VideoPlayerWatchInputTarget(
    private val ui: VideoPlayerWatchUiState,
    private val host: VideoPlayerScreenHost,
    private val viewModel: VideoPlayerViewModel,
    private val speedBoost: MutableState<Boolean>,
) : VideoPlayerInputTarget {
    // Observable, as the picture follows the framing it holds from outside composition
    private var current by mutableStateOf<VideoPlayerViewModel.State.Ready?>(null)
    private lateinit var player: VideoPlayer
    private lateinit var actions: VideoPlayerWatchActions
    private var controlsPinned = false
    override var skipOffered = false
        private set
    override var seekStepSeconds = 0
        private set
    override var seekGestureModeActive = false
        private set

    private val ready: VideoPlayerViewModel.State.Ready
        get() = checkNotNull(current) { "The screen updates its input target before composing the input" }

    fun update(
        current: VideoPlayerViewModel.State.Ready,
        player: VideoPlayer,
        actions: VideoPlayerWatchActions,
        controlsPinned: Boolean,
        seekGestureModeActive: Boolean,
        skipOffered: Boolean,
        seekStepSeconds: Int,
    ) {
        this.seekStepSeconds = seekStepSeconds
        this.current = current
        this.player = player
        this.actions = actions
        this.controlsPinned = controlsPinned
        this.seekGestureModeActive = seekGestureModeActive
        this.skipOffered = skipOffered
    }

    override val subtitleEditorVisible get() = ui.subtitleEditorVisible
    override val controlsLocked get() = host.visit.controlsLocked
    override val controlsVisible get() = ui.controlsVisible
    override val settingsVisible get() = ui.settingsVisible
    override val panelOpen get() = ui.settingsVisible || ui.episodesDrawerVisible || ui.subtitleEditorVisible
    override val speedBoostEligible
        get() = (!ui.controlsVisible || controlsPinned) &&
            !host.visit.controlsLocked &&
            !ui.settingsVisible &&
            !ui.subtitleEditorVisible &&
            !seekGestureModeActive
    override val speedBoostActive get() = speedBoost.value
    override var ignoreNextSeekTapUp
        get() = ui.ignoreNextGestureSeekTapUp
        set(value) {
            ui.ignoreNextGestureSeekTapUp = value
        }

    override fun setSpeedBoostActive(active: Boolean) {
        speedBoost.value = active
    }

    override fun hideControls() {
        if (!controlsPinned) ui.controlsVisible = false
    }

    override fun registerControllerInteraction(showControls: Boolean) {
        ui.registerControllerInteraction(showControls)
    }

    override fun performGestureSeek(direction: VideoPlayerSeekDirection) {
        actions.gestureSeek(direction)
    }

    override fun sideGestureLevel(type: VideoPlayerSideGestureType): Int = when (type) {
        VideoPlayerSideGestureType.Brightness -> host.brightness?.level ?: 0
        VideoPlayerSideGestureType.Volume -> host.volume.level
    }

    override fun sideGestureMaxLevel(type: VideoPlayerSideGestureType): Int = when (type) {
        VideoPlayerSideGestureType.Brightness -> VideoPlayerBrightness.MAX_LEVEL
        VideoPlayerSideGestureType.Volume -> host.volume.gestureMaxLevel
    }

    override fun applySideGestureLevel(type: VideoPlayerSideGestureType, level: Int) {
        when (type) {
            VideoPlayerSideGestureType.Brightness -> host.brightness?.let { brightness ->
                ui.triggerSideGestureFeedback(type, brightness.adjust(level), VideoPlayerBrightness.MAX_LEVEL)
            }
            VideoPlayerSideGestureType.Volume -> showVolume(host.volume.adjust(player, level))
        }
    }

    private fun showVolume(level: Int) {
        val volume = host.volume
        ui.triggerSideGestureFeedback(
            type = VideoPlayerSideGestureType.Volume,
            level = level,
            maxLevel = volume.gestureMaxLevel,
            boostFromLevel = volume.maxLevel.takeIf { volume.boostLevels > 0 },
            boostPercent = volume.boostPercent.takeIf { level > volume.maxLevel },
        )
    }

    override val videoAspectRatio get() = host.visit.videoAspectRatio
    override val barReach get() = ui.barReach
    override val pictureFraming get() = ui.picturePreview ?: ready.playback.pictureFraming

    override fun previewPictureFraming(framing: PictureFraming) {
        hideControls()
        ui.picturePreview = framing
    }

    override fun commitPictureFraming(framing: PictureFraming) {
        viewModel.setPictureFraming(framing)
        // A changed framing reaches the screen only when it next composes, and the preview is dropped then; dropping
        // it now would show the framing from before for a frame.
        if (ready.playback.pictureFraming == framing) ui.picturePreview = null
    }

    override fun togglePlayback(showControls: Boolean) = actions.togglePlayback(showControls)

    override fun seekStep(direction: VideoPlayerSeekDirection) = actions.seekStep(direction)

    override val hasFullScreen get() = host.fullScreen != null

    override fun toggleFullScreen() = actions.toggleFullScreen()

    override val hasPictureInPicture get() = host.pictureInPicture.showsButton

    override fun enterPictureInPicture() = host.pictureInPicture.enterFromControls()

    override fun openSettings() = actions.openSettings(null)

    override val episodesVisible get() = ui.episodesDrawerVisible

    override fun toggleEpisodes() =
        if (ui.episodesDrawerVisible) actions.dismissEpisodes() else actions.openEpisodes()

    override fun toggleMute() {
        val control = host.volumeControl ?: return
        control.setMuted(player, !control.muted)
        showVolume(if (control.muted) 0 else host.volume.level)
    }

    override fun stepSpeed(faster: Boolean) {
        actions.endSpeedBoost()
        viewModel.updateSessionPlaybackSpeed(steppedPlaybackSpeed(ready.playback.sessionPlaybackSpeed, faster))
        ui.registerControllerInteraction(true)
    }

    override val stepsFrames get() = player.capabilities.stepsFrames && !player.state.value.isPlaying

    override fun stepFrame(forward: Boolean) {
        if (!stepsFrames) return
        ui.registerControllerInteraction(false)
        player.stepFrame(forward)
    }

    override val canPlayPreviousEpisode get() = ready.canPlayPrevious
    override val canPlayNextEpisode get() = ready.canPlayNext

    override fun previousEpisode() = actions.previousEpisode()

    override fun nextEpisode() = actions.nextEpisode()

    override val hasSubtitles get() = ready.playback.hasSubtitleSelection

    override fun toggleSubtitles() {
        val playing = ready.playback.currentSubtitle.let { selection ->
            // The source's default, which the settings panel shows as Off when there is none
            if (selection == VideoPlayerSubtitleSelection.Default) {
                defaultSubtitleSelection(ready.playback.subtitles)
            } else {
                selection
            }
        }
        if (playing != VideoPlayerSubtitleSelection.None) {
            host.visit.subtitleTurnedOffKey = subtitleSelectionKey(playing)
            viewModel.selectSubtitle(VideoPlayerSubtitleSelection.None)
            return
        }
        val tracks = (externalSubtitleOptions(ready.playback.subtitles) + ready.playback.subtitleOptions)
            .filter { it.selection != VideoPlayerSubtitleSelection.None }
        // Not Default: that leaves the track to the source, whose default may be none
        val track = tracks.firstOrNull { it.key == host.visit.subtitleTurnedOffKey } ?: tracks.firstOrNull()
        viewModel.selectSubtitle(track?.selection ?: VideoPlayerSubtitleSelection.Default)
    }

    override fun skip() = actions.skip()
}

/** The input target of [ui]'s playback, told on every composition what the screen composed. */
@Composable
internal fun rememberVideoPlayerWatchInputTarget(
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    speedBoost: MutableState<Boolean>,
    current: VideoPlayerViewModel.State.Ready,
    player: VideoPlayer,
    actions: VideoPlayerWatchActions,
    controlsPinned: Boolean,
    seekGestureModeActive: Boolean,
    skipOffered: Boolean,
    seekStepSeconds: Int,
): VideoPlayerWatchInputTarget {
    val target = remember(ui) { VideoPlayerWatchInputTarget(ui, host, viewModel, speedBoost) }
    target.update(current, player, actions, controlsPinned, seekGestureModeActive, skipOffered, seekStepSeconds)
    return target
}
