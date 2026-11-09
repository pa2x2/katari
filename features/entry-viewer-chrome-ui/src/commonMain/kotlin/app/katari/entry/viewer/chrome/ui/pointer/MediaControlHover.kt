package app.katari.entry.viewer.chrome.ui.pointer

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Hovers over a media viewer's controls, such as the player's or the immersive feed's, which keep them shown while a
 * mouse rests on one; null where nothing hovers, as on a touch screen. Only the controls report it: the bars and the
 * gaps around them leave a still mouse to hide with the controls, as it does over the picture.
 */
val LocalMediaControlHover = staticCompositionLocalOf<MutableInteractionSource?> { null }

/** Marks a control of a media viewer, such as a button or the timeline, so that a mouse resting on it keeps it shown. */
@Composable
fun Modifier.mediaControl(): Modifier = LocalMediaControlHover.current?.let { hoverable(it) } ?: this
