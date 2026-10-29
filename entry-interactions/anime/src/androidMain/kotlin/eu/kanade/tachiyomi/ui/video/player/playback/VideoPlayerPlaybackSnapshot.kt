package eu.kanade.tachiyomi.ui.video.player.playback

import androidx.media3.common.C
import androidx.media3.common.Player

internal data class VideoPlayerPlaybackSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackEnded: Boolean = false,
)

internal fun Player.capturePlaybackSnapshot(): VideoPlayerPlaybackSnapshot {
    val resolvedDurationMs = duration
        .takeIf { it > 0L && it != C.TIME_UNSET }
        ?: 0L
    val resolvedPositionMs = currentPosition
        .coerceAtLeast(0L)
        .coerceToPlaybackDuration(resolvedDurationMs)
    val resolvedBufferedPositionMs = bufferedPosition
        .coerceAtLeast(resolvedPositionMs)
        .coerceToPlaybackDuration(resolvedDurationMs)

    return VideoPlayerPlaybackSnapshot(
        positionMs = resolvedPositionMs,
        durationMs = resolvedDurationMs,
        bufferedPositionMs = resolvedBufferedPositionMs,
        isPlaying = isPlaying,
        isLoading = playbackState == Player.STATE_BUFFERING,
        playbackEnded = playbackState == Player.STATE_ENDED,
    )
}

internal fun Long.coerceToPlaybackDuration(durationMs: Long): Long {
    return if (durationMs > 0L) {
        coerceIn(0L, durationMs)
    } else {
        coerceAtLeast(0L)
    }
}
