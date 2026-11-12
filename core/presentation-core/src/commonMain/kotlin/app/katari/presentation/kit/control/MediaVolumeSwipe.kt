package app.katari.presentation.kit.control

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * The volume levels, each a percent, that a touchpad swipe whose fingers went [fingerTravel] pixels down sets a volume
 * up by, so going up raises it: one for each [SwipePerLevel] of it, whatever the window's size or how fast the fingers
 * go, so a quick swipe sets it by some 30. Used by [MediaVolumeControl] and the video player's picture.
 */
fun Density.mediaVolumeLevelsSwiped(fingerTravel: Float): Int = (-fingerTravel / SwipePerLevel.toPx()).toInt()

private val SwipePerLevel = 40.dp
