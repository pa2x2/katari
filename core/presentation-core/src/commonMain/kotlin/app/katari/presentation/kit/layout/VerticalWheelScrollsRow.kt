package app.katari.presentation.kit.layout

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.pointer.touchpadScroll

/**
 * Lets the vertical mouse wheel scroll [state], a row that only scrolls sideways, as GTK's sideways-only scrolled views
 * do. Compose itself scrolls such a row only with Shift held or a sideways wheel or swipe, which a plain mouse wheel
 * never sends. Once the row has no further to go in the wheel's direction, the wheel scrolls the page around it again.
 * A touchpad's fingers scroll sideways of their own, so their scrolling up and down stays the page's.
 */
internal fun Modifier.verticalWheelScrollsRow(state: ScrollableState): Modifier = pointerInput(state) {
    val step = WheelNotchDistance.toPx()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            if (event.touchpadScroll != null) continue
            val change = event.changes.firstOrNull()?.takeUnless { it.isConsumed } ?: continue
            val delta = change.scrollDelta
            // A sideways delta is the row's own scrolling, already handled by it.
            if (delta.x != 0f || delta.y == 0f) continue
            val canMove = if (delta.y > 0f) state.canScrollForward else state.canScrollBackward
            if (!canMove) continue
            state.dispatchRawDelta(delta.y * step)
            change.consume()
        }
    }
}

private val WheelNotchDistance = 64.dp
