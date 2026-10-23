package eu.kanade.tachiyomi.ui.video.player.gesture

internal enum class VideoPlayerSideGestureType {
    Brightness,
    Volume,
}

internal data class VideoPlayerSideGestureFeedbackState(
    val type: VideoPlayerSideGestureType,
    val level: Int,
    val maxLevel: Int = 100,
    val sequence: Long,
)
