package app.katari.presentation.kit.control

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.util.fastForEach
import app.katari.presentation.kit.pointer.LocalWindowHasMouse
import app.katari.presentation.kit.pointer.MovePointer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Makes an element something a drag picks up and moves, such as a destination in the navigation editor. In a window
 * worked with a mouse, the left button drags it as soon as the pointer moves, under the move cursor; a finger first
 * presses and holds it, so a swipe still scrolls what it sits in. Positions are in the element's own coordinates, and
 * the callbacks are always the latest given, so a drag keeps going while its screen recomposes. Usable outside
 * composition, such as in a per-item modifier lambda.
 */
fun Modifier.dragSource(
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
): Modifier = this
    // Where windows count as touch, MovePointer is the default pointer, which is what moveCursor leaves there.
    .pointerHoverIcon(MovePointer)
    .then(DragSourceElement(onDragStart, onDrag, onDragEnd, onDragCancel))

private data class DragSourceElement(
    val onDragStart: (Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDragEnd: () -> Unit,
    val onDragCancel: () -> Unit,
) : ModifierNodeElement<DragSourceNode>() {
    override fun create() = DragSourceNode(onDragStart, onDrag, onDragEnd, onDragCancel)

    override fun update(node: DragSourceNode) {
        node.onDragStart = onDragStart
        node.onDrag = onDrag
        node.onDragEnd = onDragEnd
        node.onDragCancel = onDragCancel
    }
}

private class DragSourceNode(
    var onDragStart: (Offset) -> Unit,
    var onDrag: (Offset) -> Unit,
    var onDragEnd: () -> Unit,
    var onDragCancel: () -> Unit,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(SuspendingPointerInputModifierNode { detectDrags() })
    }

    private suspend fun PointerInputScope.detectDrags() = awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pickedUp = if (down.type == PointerType.Mouse && currentValueOf(LocalWindowHasMouse)) {
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        } else {
            awaitLongPressOrCancellation(down.id)
        } ?: return@awaitEachGesture
        moveUntilDropped(pickedUp)
    }

    private suspend fun AwaitPointerEventScope.moveUntilDropped(
        pickedUp: PointerInputChange,
    ) {
        onDragStart(pickedUp.position)
        try {
            val dropped = drag(pickedUp.id) { change ->
                onDrag(change.position)
                change.consume()
            }
            if (dropped) {
                // The release ends the drag, not a click on what sits under it.
                currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
                onDragEnd()
            } else {
                onDragCancel()
            }
        } catch (cancelled: CancellationException) {
            onDragCancel()
            throw cancelled
        }
    }
}
