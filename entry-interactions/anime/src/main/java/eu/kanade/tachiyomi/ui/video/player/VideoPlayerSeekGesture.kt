package eu.kanade.tachiyomi.ui.video.player

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
): Long {
    val deltaMs = when (direction) {
        VideoPlayerSeekDirection.Backward -> -VIDEO_PLAYER_SEEK_INCREMENT_MS
        VideoPlayerSeekDirection.Forward -> VIDEO_PLAYER_SEEK_INCREMENT_MS
    }
    return (positionMs + deltaMs).coerceToPlaybackDuration(durationMs)
}

internal fun nextVideoPlayerSeekFeedbackState(
    previousState: VideoPlayerSeekFeedbackState?,
    direction: VideoPlayerSeekDirection,
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
            previousState.totalSeconds + VIDEO_PLAYER_SEEK_INCREMENT_SECONDS
        } else {
            VIDEO_PLAYER_SEEK_INCREMENT_SECONDS
        },
        hidePlayerChrome = hidePlayerChrome,
        sequence = sequence,
        updatedAtMillis = updatedAtMillis,
    )
}

private const val VIDEO_PLAYER_SEEK_INCREMENT_MS = 5_000L
private const val VIDEO_PLAYER_SEEK_INCREMENT_SECONDS =
    (VIDEO_PLAYER_SEEK_INCREMENT_MS / 1_000L).toInt()
private const val VIDEO_PLAYER_SEEK_FEEDBACK_BURST_WINDOW_MS = 900L
