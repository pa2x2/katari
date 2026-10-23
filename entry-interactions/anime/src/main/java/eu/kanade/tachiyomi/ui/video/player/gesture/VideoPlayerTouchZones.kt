package eu.kanade.tachiyomi.ui.video.player.gesture

private const val HOLD_SPEED_ZONE_START_FRACTION = 0.25f
private const val HOLD_SPEED_ZONE_END_FRACTION = 0.75f
private const val HOLD_SPEED_ZONE_TOP_FRACTION = 0.18f
private const val HOLD_SPEED_ZONE_BOTTOM_FRACTION = 0.78f
private const val HOLD_SPEED_TOP_ZONE_LEFT_FRACTION = 0.1f
private const val HOLD_SPEED_TOP_ZONE_RIGHT_FRACTION = 0.9f
private const val HOLD_SPEED_TOP_ZONE_TOP_FRACTION = 0f
private const val HOLD_SPEED_TOP_ZONE_BOTTOM_FRACTION = 0.2f

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

internal data class PlayerTouchZoneBounds(
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
) {
    fun contains(xFraction: Float, yFraction: Float): Boolean {
        return xFraction in leftFraction..rightFraction && yFraction in topFraction..bottomFraction
    }
}

internal fun holdSpeedZoneBounds(): List<PlayerTouchZoneBounds> {
    return listOf(
        PlayerTouchZoneBounds(
            leftFraction = HOLD_SPEED_TOP_ZONE_LEFT_FRACTION,
            topFraction = HOLD_SPEED_TOP_ZONE_TOP_FRACTION,
            rightFraction = HOLD_SPEED_TOP_ZONE_RIGHT_FRACTION,
            bottomFraction = HOLD_SPEED_TOP_ZONE_BOTTOM_FRACTION,
        ),
        PlayerTouchZoneBounds(
            leftFraction = HOLD_SPEED_ZONE_START_FRACTION,
            topFraction = HOLD_SPEED_ZONE_TOP_FRACTION,
            rightFraction = HOLD_SPEED_ZONE_END_FRACTION,
            bottomFraction = HOLD_SPEED_ZONE_BOTTOM_FRACTION,
        ),
    )
}

internal fun isTouchInHoldSpeedZone(
    touchX: Float,
    touchY: Float,
    playerWidth: Int,
    playerHeight: Int,
): Boolean {
    if (playerWidth <= 0 || playerHeight <= 0) {
        return false
    }

    val width = playerWidth.toFloat()
    val height = playerHeight.toFloat()
    val xFraction = (touchX / width).coerceIn(0f, 1f)
    val yFraction = (touchY / height).coerceIn(0f, 1f)
    return holdSpeedZoneBounds().any { zone ->
        zone.contains(xFraction = xFraction, yFraction = yFraction)
    }
}

internal fun verticalGestureFraction(
    startY: Float,
    currentY: Float,
    playerWidth: Int,
    playerHeight: Int,
): Float {
    if (playerWidth <= 0 || playerHeight <= 0) {
        return 0f
    }

    val gestureDistance = minOf(playerWidth, playerHeight).toFloat()
    return ((startY - currentY) / gestureDistance).coerceIn(-1f, 1f)
}
