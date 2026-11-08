package eu.kanade.tachiyomi.ui.video.player.gesture

import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration

internal enum class VideoPlayerSeekDirection {
    Backward,
    Forward,
}

internal data class VideoPlayerSeekFeedbackState(
    val direction: VideoPlayerSeekDirection,
    /** How far the burst has moved the position, which a seek clamped at either end leaves short of its steps. */
    val totalMs: Long,
    val hidePlayerChrome: Boolean,
    val sequence: Long,
    val updatedAtMillis: Long,
)

internal fun resolveVideoPlayerSeekDirectionFromTap(
    tapX: Float,
    viewportWidth: Float,
): VideoPlayerSeekDirection? {
    return when {
        viewportWidth <= 0f -> null
        tapX <= viewportWidth / 3f -> VideoPlayerSeekDirection.Backward
        tapX >= viewportWidth * 2f / 3f -> VideoPlayerSeekDirection.Forward
        else -> null
    }
}

internal fun resolveVideoPlayerSeekPosition(
    positionMs: Long,
    durationMs: Long,
    direction: VideoPlayerSeekDirection,
    stepSeconds: Int,
): Long {
    val stepMs = stepSeconds * 1_000L
    val deltaMs = when (direction) {
        VideoPlayerSeekDirection.Backward -> -stepMs
        VideoPlayerSeekDirection.Forward -> stepMs
    }
    return (positionMs + deltaMs).coerceToPlaybackDuration(durationMs)
}

internal fun nextVideoPlayerSeekFeedbackState(
    previousState: VideoPlayerSeekFeedbackState?,
    direction: VideoPlayerSeekDirection,
    movedMs: Long,
    hidePlayerChrome: Boolean,
    sequence: Long,
    updatedAtMillis: Long,
): VideoPlayerSeekFeedbackState {
    val isBurstContinuation = previousState != null &&
        previousState.direction == direction &&
        updatedAtMillis - previousState.updatedAtMillis <= VIDEO_PLAYER_SEEK_FEEDBACK_BURST_WINDOW_MS
    return VideoPlayerSeekFeedbackState(
        direction = direction,
        totalMs = if (isBurstContinuation) previousState.totalMs + movedMs else movedMs,
        hidePlayerChrome = hidePlayerChrome,
        sequence = sequence,
        updatedAtMillis = updatedAtMillis,
    )
}

private const val VIDEO_PLAYER_SEEK_FEEDBACK_BURST_WINDOW_MS = 900L
