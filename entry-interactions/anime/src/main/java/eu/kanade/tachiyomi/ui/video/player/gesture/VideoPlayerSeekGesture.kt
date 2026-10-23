package eu.kanade.tachiyomi.ui.video.player.gesture

import eu.kanade.tachiyomi.ui.video.player.coerceToPlaybackDuration

internal enum class VideoPlayerSeekDirection {
    Backward,
    Forward,
}

internal data class VideoPlayerSeekFeedbackState(
    val direction: VideoPlayerSeekDirection,
    val totalSeconds: Int,
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
    stepSeconds: Int,
    hidePlayerChrome: Boolean,
    sequence: Long,
    updatedAtMillis: Long,
): VideoPlayerSeekFeedbackState {
    val isBurstContinuation = previousState != null &&
        previousState.direction == direction &&
        updatedAtMillis - previousState.updatedAtMillis <= VIDEO_PLAYER_SEEK_FEEDBACK_BURST_WINDOW_MS
    return VideoPlayerSeekFeedbackState(
        direction = direction,
        totalSeconds = if (isBurstContinuation) {
            previousState.totalSeconds + stepSeconds
        } else {
            stepSeconds
        },
        hidePlayerChrome = hidePlayerChrome,
        sequence = sequence,
        updatedAtMillis = updatedAtMillis,
    )
}

private const val VIDEO_PLAYER_SEEK_FEEDBACK_BURST_WINDOW_MS = 900L
