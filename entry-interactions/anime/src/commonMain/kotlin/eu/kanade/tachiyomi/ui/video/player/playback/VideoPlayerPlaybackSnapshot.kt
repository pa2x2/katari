package eu.kanade.tachiyomi.ui.video.player.playback

import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerState

internal data class VideoPlayerPlaybackSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackEnded: Boolean = false,
)

/** The snapshot at the position [state] last reported. */
internal fun VideoPlayerState.playbackSnapshot(positionMs: Long = this.positionMs): VideoPlayerPlaybackSnapshot {
    val resolvedDurationMs = durationMs?.takeIf { it > 0L } ?: 0L
    val resolvedPositionMs = positionMs
        .coerceAtLeast(0L)
        .coerceToPlaybackDuration(resolvedDurationMs)
    val resolvedBufferedPositionMs = bufferedPositionMs
        .coerceAtLeast(resolvedPositionMs)
        .coerceToPlaybackDuration(resolvedDurationMs)

    return VideoPlayerPlaybackSnapshot(
        positionMs = resolvedPositionMs,
        durationMs = resolvedDurationMs,
        bufferedPositionMs = resolvedBufferedPositionMs,
        isPlaying = isPlaying,
        isLoading = playback == VideoPlayerState.Playback.Buffering,
        playbackEnded = playback == VideoPlayerState.Playback.Ended,
    )
}

/** The snapshot at this moment's position. */
internal fun VideoPlayer.capturePlaybackSnapshot(): VideoPlayerPlaybackSnapshot =
    state.value.playbackSnapshot(positionMs = currentPositionMs)

internal fun Long.coerceToPlaybackDuration(durationMs: Long): Long {
    return if (durationMs > 0L) {
        coerceIn(0L, durationMs)
    } else {
        coerceAtLeast(0L)
    }
}
