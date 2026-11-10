package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerError
import app.katari.video.engine.spi.player.VideoPlayerEvent
import app.katari.video.engine.spi.player.VideoPlayerListener
import app.katari.video.engine.spi.player.VideoSize
import app.katari.video.engine.spi.tracks.VideoTracks
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import eu.kanade.tachiyomi.ui.video.player.engine.pickedTrackSignal
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.playbackSnapshot
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import eu.kanade.tachiyomi.ui.video.player.tracks.applyAudioTrack
import eu.kanade.tachiyomi.ui.video.player.tracks.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.tracks.availableAdaptiveQualities
import eu.kanade.tachiyomi.ui.video.player.tracks.availableAudioTracks
import eu.kanade.tachiyomi.ui.video.player.tracks.availableSubtitleTracks
import eu.kanade.tachiyomi.ui.video.player.tracks.resolveAppliedSubtitleSelection
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * The player the viewer watches for [current], made anew for each episode, stream, playback revision and subtitle set,
 * in the engine [engine] runs. It starts at the resume position and reports to the screen through
 * [WatchedPlayerEvents]; its active playback clock is registered with the playback controller, which owns the player
 * once the screen attaches it.
 */
@Composable
internal fun rememberWatchedPlayer(
    current: VideoPlayerViewModel.State.Ready,
    subtitlePayloadKey: String,
    seekStepSeconds: Int,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    ui: VideoPlayerWatchUiState,
    latestPlayback: State<VideoPlaybackUiState>,
    publishSnapshot: (VideoPlayerPlaybackSnapshot) -> Unit,
    engine: WatchedPlayerEngine,
): VideoPlayer {
    val events = remember(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        val activePlaybackClock = VideoActivePlaybackClock()
        val videoEngine = when (engine.run.engine) {
            VideoPlayerEngine.BuiltIn -> host.engines.builtIn
            VideoPlayerEngine.Mpv -> checkNotNull(host.engines.mpvAddon) { "Only the mpv add-on plays in mpv" }.engine
        }
        val player = videoEngine.createPlayer(
            stream = current.stream,
            role = VideoPlayerRole.Watched(seekStepMs = seekStepSeconds * 1000L),
        )
        host.playback.track(player, activePlaybackClock)
        WatchedPlayerEvents(
            player = player,
            activePlaybackClock = activePlaybackClock,
            current = current,
            host = host,
            viewModel = viewModel,
            ui = ui,
            latestPlayback = latestPlayback,
            publishSnapshot = publishSnapshot,
            engine = engine,
            engineName = videoEngine.name,
        ).also { events ->
            player.addListener(events)
            if (current.resumePositionMs > 0L) {
                player.seekTo(current.resumePositionMs)
            }
            publishSnapshot(player.capturePlaybackSnapshot())
        }
    }
    LaunchedEffect(events) { events.followState() }
    return events.player
}

/**
 * Maps the watched player onto the screen: the playback snapshot, the first frame, the video's shape, and playback
 * errors, which go to [engine] to switch engines or show a problem. When the tracks change it reapplies the chosen
 * subtitle and audio track, hands the tracks the stream offers to the view model, and lets [engine] look for what mpv
 * would show better.
 */
private class WatchedPlayerEvents(
    val player: VideoPlayer,
    private val activePlaybackClock: VideoActivePlaybackClock,
    private val current: VideoPlayerViewModel.State.Ready,
    private val host: VideoPlayerScreenHost,
    private val viewModel: VideoPlayerViewModel,
    private val ui: VideoPlayerWatchUiState,
    private val latestPlayback: State<VideoPlaybackUiState>,
    private val publishSnapshot: (VideoPlayerPlaybackSnapshot) -> Unit,
    private val engine: WatchedPlayerEngine,
    private val engineName: String,
) : VideoPlayerListener {
    private var renderedFirstFrame = false

    override fun onEvent(event: VideoPlayerEvent) {
        when (event) {
            is VideoPlayerEvent.PositionJumped -> {
                if (event.bySeek) {
                    viewModel.resetPlaybackBaseline(event.positionMs)
                }
                ui.scrubPositionMs = event.positionMs.coerceAtLeast(0L)
                publishSnapshot(player.capturePlaybackSnapshot())
            }
            VideoPlayerEvent.FirstFrameRendered -> {
                renderedFirstFrame = true
                ui.startupOverlayVisible = false
                ui.frameShown = true
                viewModel.firstFrameShown(current, engineName)
            }
            is VideoPlayerEvent.Failed -> onError(event.error)
        }
    }

    private fun onError(error: VideoPlayerError) {
        ui.startupOverlayVisible = false
        logcat(LogPriority.ERROR, error) {
            "VideoPlayer: playback failed episodeId=${current.chapterId} engine=${engine.run.engine} " +
                "stream=${current.streamLabel} code=${error.codeName}"
        }
        engine.onError(error, player.currentPositionMs, beforeFirstFrame = !renderedFirstFrame)
    }

    /**
     * Follows the player's state while the screen shows the player. The scrub position follows playback while the
     * viewer isn't scrubbing.
     */
    suspend fun followState() {
        var playing = false
        var videoSize: VideoSize? = null
        // Media3 started with no tracks too, and only a change of them was news.
        var tracks = VideoTracks.Empty
        player.state.collect { state ->
            if (state.isPlaying != playing) {
                playing = state.isPlaying
                activePlaybackClock.setActive(playing)
            }
            if (state.videoSize != videoSize) {
                videoSize = state.videoSize
                state.videoSize?.displayAspectRatio?.let { host.visit.videoAspectRatio = it }
            }
            if (state.tracks != tracks) {
                tracks = state.tracks
                onTracksChanged(tracks)
            }
            publishSnapshot(state.playbackSnapshot())
            if (!ui.isScrubbing) {
                ui.scrubPositionMs = ui.playbackSnapshot.positionMs
            }
        }
    }

    private fun onTracksChanged(tracks: VideoTracks) {
        val latestPlaybackState = latestPlayback.value
        player.applySubtitleSelection(latestPlaybackState.currentSubtitle)
        val externalAudio = latestPlaybackState.currentStream.audioTracks
        player.applyAudioTrack(latestPlaybackState.currentAudioTrackKey, externalAudio)
        // As they are after the choices above, which some engines take in at once.
        val applied = player.state.value.tracks
        val audioTracks = applied.availableAudioTracks(externalAudio)
        viewModel.updateAudioTracks(audioTracks)
        viewModel.updateAdaptiveQualities(applied.availableAdaptiveQualities())
        val subtitleOptions = applied.availableSubtitleTracks(latestPlaybackState.subtitles)
        viewModel.updateSubtitleOptions(subtitleOptions)
        val resolvedSubtitleSelection = applied.resolveAppliedSubtitleSelection(
            latestPlaybackState.currentSubtitle,
            latestPlaybackState.subtitles,
        )
        if (resolvedSubtitleSelection != latestPlaybackState.currentSubtitle) {
            viewModel.updateResolvedSubtitle(resolvedSubtitleSelection)
        }
        val picked = pickedTrackSignal(
            audioKey = latestPlaybackState.currentAudioTrackKey,
            audio = audioTracks,
            subtitleKey = subtitleSelectionKey(resolvedSubtitleSelection),
            subtitles = subtitleOptions,
        )
        engine.onTracks(tracks, picked, player.currentPositionMs)
    }
}
