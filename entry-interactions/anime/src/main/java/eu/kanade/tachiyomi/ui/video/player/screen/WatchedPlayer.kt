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
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackUiState
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.WatchedPlayerOptions
import eu.kanade.tachiyomi.ui.video.player.applyAudioTrack
import eu.kanade.tachiyomi.ui.video.player.applySubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.availableAdaptiveQualities
import eu.kanade.tachiyomi.ui.video.player.availableAudioTracks
import eu.kanade.tachiyomi.ui.video.player.availableSubtitleTracks
import eu.kanade.tachiyomi.ui.video.player.buildVideoPlayer
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.resolveAppliedSubtitleSelection
import eu.kanade.tachiyomi.ui.video.player.toProblem
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * The player the viewer watches for [current], made anew for each episode, stream, playback revision and subtitle set.
 * It starts at the resume position and reports to the screen through [WatchedPlayerListener]; its active playback
 * clock is registered with the playback controller, which owns the player once the screen attaches it.
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
    onProblem: (VideoPlayerProblem) -> Unit,
): ExoPlayer {
    val context = LocalContext.current
    return remember(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        val activePlaybackClock = VideoActivePlaybackClock()
        buildVideoPlayer(
            context = context,
            networkHelper = host.networkHelper,
            mediaCache = host.mediaCache,
            stream = current.stream,
            subtitles = current.playback.subtitles,
            watched = WatchedPlayerOptions(
                metadata = MediaMetadata.Builder()
                    .setTitle(current.chapterName)
                    .setArtist(current.entryTitle)
                    .setArtworkUri(current.entry.thumbnailUrl?.toUri())
                    .build(),
                seekStepMs = seekStepSeconds * 1000L,
            ),
        ).also { exoPlayer ->
            host.playback.track(exoPlayer, activePlaybackClock)
            exoPlayer.addListener(
                WatchedPlayerListener(
                    player = exoPlayer,
                    activePlaybackClock = activePlaybackClock,
                    current = current,
                    host = host,
                    viewModel = viewModel,
                    ui = ui,
                    latestPlayback = latestPlayback,
                    publishSnapshot = publishSnapshot,
                    onProblem = onProblem,
                ),
            )
            if (current.resumePositionMs > 0L) {
                exoPlayer.seekTo(current.resumePositionMs)
            }
            publishSnapshot(exoPlayer.capturePlaybackSnapshot())
        }
    }
}

/**
 * Maps the watched player's events onto the screen: the playback snapshot, the first frame, the video's shape,
 * playback errors and device volume. When the tracks change it reapplies the chosen subtitle and audio track and hands
 * the tracks the stream offers to the view model.
 */
private class WatchedPlayerListener(
    private val player: ExoPlayer,
    private val activePlaybackClock: VideoActivePlaybackClock,
    private val current: VideoPlayerViewModel.State.Ready,
    private val host: VideoPlayerScreenHost,
    private val viewModel: VideoPlayerViewModel,
    private val ui: VideoPlayerWatchUiState,
    private val latestPlayback: State<VideoPlaybackUiState>,
    private val publishSnapshot: (VideoPlayerPlaybackSnapshot) -> Unit,
    private val onProblem: (VideoPlayerProblem) -> Unit,
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

    override fun onRenderedFirstFrame() {
        ui.startupOverlayVisible = false
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        videoSize.displayAspectRatioOrNull()?.let { host.visit.videoAspectRatio = it }
    }

    override fun onPlayerError(error: PlaybackException) {
        ui.startupOverlayVisible = false
        onProblem(error.toProblem())
        logcat(LogPriority.ERROR, error) {
            "VideoPlayer: playback failed episodeId=${current.chapterId} " +
                "stream=${current.streamLabel} code=${error.errorCodeName}"
        }
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
