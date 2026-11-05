package eu.kanade.tachiyomi.ui.reader.viewer.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Taps, double taps and long presses on a reader's pages. Unlike the foundation's tap detector, which only gives up a
 * press once something consumes it, a press that moves past the touch slop is no tap: a reader scrolls only along its
 * own axis, so a sideways drag on a long strip, or a vertical one on a horizontal pager, would otherwise end as a tap
 * and toggle the menu. With [onDoubleTap] null a tap is reported at once; otherwise once no second press follows in
 * time, and a second press that turns into a drag is quick zoom, not a double tap.
 */
internal suspend fun PointerInputScope.detectReaderTaps(
    onTap: (Offset) -> Unit,
    onDoubleTap: ((Offset) -> Unit)?,
    onLongPress: (Offset) -> Unit,
) = awaitEachGesture {
    val longPressMillis = viewConfiguration.longPressTimeoutMillis
    val down = awaitFirstDown()
    var released = false
    val up = withTimeoutOrNull(longPressMillis) { awaitTapUp(down).also { released = true } }
    if (!released) {
        // Held in place past the timeout: anything that moved or took the press would have ended the wait
        onLongPress(down.position)
        consumeUntilUp()
        return@awaitEachGesture
    }
    if (up == null) return@awaitEachGesture
    if (onDoubleTap == null) {
        onTap(up)
        return@awaitEachGesture
    }
    val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) { awaitFirstDown() }
    if (secondDown == null) {
        onTap(up)
        return@awaitEachGesture
    }
    if (withTimeoutOrNull(longPressMillis) { awaitTapUp(secondDown) } != null) onDoubleTap(secondDown.position)
}

/**
 * Waits for [down] to lift as a tap and returns where it lifted, or null once it moves past the touch slop, something
 * else consumes it, or another finger joins.
 */
private suspend fun AwaitPointerEventScope.awaitTapUp(down: PointerInputChange): Offset? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: return null
        if (change.isConsumed || event.changes.count { it.pressed } > 1) return null
        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) return null
        if (!change.pressed) {
            change.consume()
            return change.position
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) return
    }
}
