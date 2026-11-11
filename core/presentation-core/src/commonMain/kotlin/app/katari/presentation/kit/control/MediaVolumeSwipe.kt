package app.katari.presentation.kit.control

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * The volume levels, each a percent, that a touchpad swipe of [distance] pixels sets a volume up by, down being
 * positive as a scroll's distance is: one for each [SwipePerLevel] of it, whatever the window's size or how fast the
 * fingers go, so a quick swipe sets it by some 30. Used by [MediaVolumeControl] and the video player's picture.
 */
fun Density.mediaVolumeLevelsSwiped(distance: Float): Int = (-distance / SwipePerLevel.toPx()).toInt()

private val SwipePerLevel = 40.dp
