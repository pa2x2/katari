package mihon.presentation.kit.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced

/**
 * Where the window was last touched, so a dialog or sheet can open on the side of a half-open fold the user is acting
 * on without each caller saying where it was opened from. Plain fields rather than state: it is read once, as a
 * surface opens, and nothing should recompose on every touch.
 */
internal class LastTouch {
    /** In window pixels; null until the first touch. */
    var positionInWindow: Offset? = null
        private set

    private var coordinates: LayoutCoordinates? = null

    /** Records touches that reach this layout, before any child sees them and without consuming them. */
    fun Modifier.recordTouches(): Modifier = this
        .onPlaced { coordinates = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val down = event.changes.firstOrNull { it.changedToDown() } ?: continue
                    positionInWindow = coordinates?.takeIf { it.isAttached }?.localToWindow(down.position)
                }
            }
        }
}

internal val LocalLastTouch = staticCompositionLocalOf<LastTouch?> { null }
