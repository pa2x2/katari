package eu.kanade.tachiyomi.ui.video.player.gesture

internal enum class VideoPlayerSideGestureType {
    Brightness,
    Volume,
}

/**
 * A side gesture's level to show. Levels above [boostFromLevel], when set, are a boost past full volume, shown as
 * [boostPercent].
 */
internal data class VideoPlayerSideGestureFeedbackState(
    val type: VideoPlayerSideGestureType,
    val level: Int,
    val maxLevel: Int = 100,
    val boostFromLevel: Int? = null,
    val boostPercent: Int? = null,
    val sequence: Long,
)
