package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.tracks.applyAudioTrack
import eu.kanade.tachiyomi.ui.video.player.tracks.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.tracks.availableAdaptiveQualities
import eu.kanade.tachiyomi.ui.video.player.tracks.availableAudioTracks
import eu.kanade.tachiyomi.ui.video.player.tracks.availableSubtitleTracks
import eu.kanade.tachiyomi.ui.video.player.tracks.resolveAppliedSubtitleSelection
import logcat.LogPriority
import mihon.video.engine.spi.VideoPlayerRole
import tachiyomi.core.common.util.system.logcat

/**
 * The player the viewer watches for [current], made anew for each episode, stream, playback revision and subtitle set,
 * in the engine [engine] runs. It starts at the resume position and reports to the screen through
 * [WatchedPlayerListener]; its active playback clock is registered with the playback controller, which owns the player
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
): Player {
    val context = LocalContext.current
    return remember(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        val activePlaybackClock = VideoActivePlaybackClock()
        val videoEngine = when (engine.run.engine) {
            VideoPlayerEngine.BuiltIn -> host.engine
            VideoPlayerEngine.Mpv -> host.mpvEngine
        }
        videoEngine.createPlayer(
            context = context,
            stream = current.stream,
            role = VideoPlayerRole.Watched(
                metadata = MediaMetadata.Builder()
                    .setTitle(current.chapterName)
                    .setArtist(current.entryTitle)
                    .setArtworkUri(current.entry.thumbnailUrl?.toUri())
                    .build(),
                seekStepMs = seekStepSeconds * 1000L,
            ),
        ).also { player ->
            host.playback.track(player, activePlaybackClock)
            player.addListener(
                WatchedPlayerListener(
                    player = player,
                    activePlaybackClock = activePlaybackClock,
                    current = current,
                    host = host,
                    viewModel = viewModel,
                    ui = ui,
                    latestPlayback = latestPlayback,
                    publishSnapshot = publishSnapshot,
                    engine = engine,
                ),
            )
            if (current.resumePositionMs > 0L) {
                player.seekTo(current.resumePositionMs)
            }
            publishSnapshot(player.capturePlaybackSnapshot())
        }
    }
}

/**
 * Maps the watched player's events onto the screen: the playback snapshot, the first frame, the video's shape,
 * playback errors, which go to [engine] to switch engines or show a problem, and device volume. When the tracks change
 * it reapplies the chosen subtitle and audio track, hands the tracks the stream offers to the view model, and lets
 * [engine] look for what mpv would show better.
 */
private class WatchedPlayerListener(
    private val player: Player,
    private val activePlaybackClock: VideoActivePlaybackClock,
    private val current: VideoPlayerViewModel.State.Ready,
    private val host: VideoPlayerScreenHost,
    private val viewModel: VideoPlayerViewModel,
    private val ui: VideoPlayerWatchUiState,
    private val latestPlayback: State<VideoPlaybackUiState>,
    private val publishSnapshot: (VideoPlayerPlaybackSnapshot) -> Unit,
    private val engine: WatchedPlayerEngine,
) : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        activePlaybackClock.setActive(isPlaying)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            viewModel.resetPlaybackBaseline(newPosition.positionMs)
        }
        ui.scrubPositionMs = newPosition.positionMs.coerceAtLeast(0L)
        publishSnapshot(player.capturePlaybackSnapshot())
    }

    private var renderedFirstFrame = false

    override fun onRenderedFirstFrame() {
        renderedFirstFrame = true
        ui.startupOverlayVisible = false
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        videoSize.displayAspectRatioOrNull()?.let { host.visit.videoAspectRatio = it }
    }

    override fun onPlayerError(error: PlaybackException) {
        ui.startupOverlayVisible = false
        logcat(LogPriority.ERROR, error) {
            "VideoPlayer: playback failed episodeId=${current.chapterId} engine=${engine.run.engine} " +
                "stream=${current.streamLabel} code=${error.errorCodeName}"
        }
        engine.onError(error, player.currentPosition, beforeFirstFrame = !renderedFirstFrame)
    }

    override fun onTracksChanged(tracks: Tracks) {
        val latestPlaybackState = latestPlayback.value
        player.applySubtitleSelection(latestPlaybackState.currentSubtitle)
        player.applyAudioTrack(latestPlaybackState.currentAudioTrackKey)
        viewModel.updateAudioTracks(player.availableAudioTracks())
        viewModel.updateAdaptiveQualities(player.availableAdaptiveQualities())
        viewModel.updateSubtitleOptions(
            player.availableSubtitleTracks(latestPlaybackState.subtitles),
        )
        val resolvedSubtitleSelection = player.resolveAppliedSubtitleSelection(
            latestPlaybackState.currentSubtitle,
            latestPlaybackState.subtitles,
        )
        if (resolvedSubtitleSelection != latestPlaybackState.currentSubtitle) {
            viewModel.updateResolvedSubtitle(resolvedSubtitleSelection)
        }
        publishSnapshot(player.capturePlaybackSnapshot())
        engine.onTracks(tracks, player.currentPosition)
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        host.volume.sync(player, deviceInfo)
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        host.volume.sync(player)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        publishSnapshot(this.player.capturePlaybackSnapshot())
    }
}

internal fun VideoSize.displayAspectRatioOrNull(): Float? {
    if (width <= 0 || height <= 0 || !pixelWidthHeightRatio.isFinite() || pixelWidthHeightRatio <= 0f) {
        return null
    }

    return (width * pixelWidthHeightRatio / height)
        .takeIf { it.isFinite() && it > 0f }
}
