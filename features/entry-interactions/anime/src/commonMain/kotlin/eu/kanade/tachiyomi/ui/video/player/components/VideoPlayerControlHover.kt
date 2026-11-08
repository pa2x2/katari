package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Hovers over the player's controls, which keep them shown while a mouse rests on one; null where nothing hovers, as on
 * a touch screen. Only the controls report it: the bars and the gaps around them leave a still mouse to hide with the
 * controls, as it does over the picture.
 */
internal val LocalVideoPlayerControlHover = staticCompositionLocalOf<MutableInteractionSource?> { null }

/** Marks a control of the player, such as a button or the timeline, so that a mouse resting on it keeps it shown. */
@Composable
internal fun Modifier.videoPlayerControl(): Modifier =
    LocalVideoPlayerControlHover.current?.let { hoverable(it) } ?: this
