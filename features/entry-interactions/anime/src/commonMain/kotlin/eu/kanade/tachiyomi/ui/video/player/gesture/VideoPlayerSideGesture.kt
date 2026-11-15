package eu.kanade.tachiyomi.ui.video.player.gesture

internal enum class VideoPlayerSideGestureType {
    Brightness,
    Volume,
}

/**
 * A side gesture's level to show, among [levels]. Levels outside [normalLevels] are past the normal range: volume
 * boosted past the device's full volume, shown as [boostPercent], or a picture extra dimmed past the screen's lowest.
 */
internal data class VideoPlayerSideGestureFeedbackState(
    val type: VideoPlayerSideGestureType,
    val level: Int,
    val levels: IntRange,
    val normalLevels: IntRange,
    val boostPercent: Int? = null,
    val sequence: Long,
)
