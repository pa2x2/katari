package eu.kanade.tachiyomi.ui.reader.viewer.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity

/**
 * Drags with a mouse's left button, which move the pages as a finger does: the foundation's pagers and lists scroll
 * only for touch. [onDrag] gets each move once the pointer has gone past the touch slop, as long as nothing under it
 * took the move, such as a zoomed page panning, and [onEnd] the velocity the button was let go at. A drag begun while
 * [enabled] answers false is left alone.
 */
internal fun Modifier.mouseDrags(
    key: Any?,
    onStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: (Velocity) -> Unit,
    enabled: () -> Boolean = { true },
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed || !enabled()) {
            return@awaitEachGesture
        }
        val velocity = VelocityTracker()
        velocity.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var travelled = Offset.Zero
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (change.isConsumed) continue
            val move = change.positionChange()
            velocity.addPosition(change.uptimeMillis, change.position)
            if (dragging) {
                onDrag(move)
            } else {
                travelled += move
                if (travelled.getDistance() <= viewConfiguration.touchSlop) continue
                dragging = true
                onStart()
                onDrag(travelled)
            }
            change.consume()
        }
        if (dragging) onEnd(velocity.calculateVelocity())
    }
}
