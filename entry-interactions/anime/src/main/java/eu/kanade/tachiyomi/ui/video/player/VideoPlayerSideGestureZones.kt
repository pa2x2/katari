package eu.kanade.tachiyomi.ui.video.player

/**
 * Which side gesture a vertical drag starting at ([startX], [startY]) drives: brightness on the left half of the
 * player, volume on the right. Drags that start within [edgeExclusionPx] of the top or bottom are left to the system
 * gestures, the top bar and the timeline.
 */
internal fun resolveSideGestureType(
    startX: Float,
    startY: Float,
    playerWidth: Int,
    playerHeight: Int,
    edgeExclusionPx: Float,
): VideoPlayerSideGestureType? {
    if (playerWidth <= 0 || playerHeight <= 0) return null
    if (startY < edgeExclusionPx || startY > playerHeight - edgeExclusionPx) return null
    return if (startX < playerWidth / 2f) VideoPlayerSideGestureType.Brightness else VideoPlayerSideGestureType.Volume
}
