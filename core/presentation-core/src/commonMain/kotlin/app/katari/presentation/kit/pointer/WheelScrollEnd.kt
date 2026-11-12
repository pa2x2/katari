package app.katari.presentation.kit.pointer

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Calls [onEnd] once a scroll that a mouse wheel or a touchpad made in the content has ended, for what follows the
 * content's scrolling and has to come to rest at one of its ends when it stops, as a collapsing app bar or a sheet
 * dragged by its list does. A touch drag ends with a fling that tells them so, but Compose never flings after wheel
 * input, which a touchpad's scrolling reaches the window as, so they would stay wherever its last step left them.
 * [onEnd] gets the scroll's last step as nested scrolling offers it, before anything took a part: y above 0 went toward
 * the content's start. Placed outside what follows the scrolling, so it sees whole steps.
 *
 * A wheel sends nothing when it stops, so the scroll ends once the content has moved by none of its steps for a moment,
 * as Compose finds the end of it too. A touchpad's fingers resting on it keep the scroll going until they lift. A press
 * hands the content back to touch, whose own fling ends it, and a new step stops [onEnd] if it is still running.
 */
internal fun Modifier.onWheelScrollEnd(onEnd: suspend (lastStep: Offset) -> Unit): Modifier =
    this then WheelScrollEndElement(onEnd)

private data class WheelScrollEndElement(val onEnd: suspend (lastStep: Offset) -> Unit) :
    ModifierNodeElement<WheelScrollEndNode>() {
    override fun create() = WheelScrollEndNode(onEnd)

    override fun update(node: WheelScrollEndNode) {
        node.onEnd = onEnd
    }
}

private class WheelScrollEndNode(var onEnd: suspend (lastStep: Offset) -> Unit) :
    DelegatingNode(),
    PointerInputModifierNode,
    NestedScrollConnection {
    private var wheelDriven = false
    private var fingersResting = false

    // The last step of a wheel's scroll that hasn't ended yet; null when there is none
    private var lastStep: Offset? = null
    private var ending: Job? = null

    init {
        delegate(nestedScrollModifierNode(this, null))
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        when (pointerEvent.type) {
            PointerEventType.Scroll -> {
                wheelDriven = true
                fingersResting = pointerEvent.touchpadScroll?.fingersMoving == true
                // The lift moves nothing, so no step of the content's would start the wait for the glide to stop.
                waitForEnd()
            }
            PointerEventType.Press -> {
                wheelDriven = false
                lastStep = null
                ending?.cancel()
            }
        }
    }

    override fun onCancelPointerInput() = Unit

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (wheelDriven && source == NestedScrollSource.UserInput && available != Offset.Zero) {
            lastStep = available
            waitForEnd()
        }
        return Offset.Zero
    }

    private fun waitForEnd() {
        val step = lastStep ?: return
        ending?.cancel()
        if (fingersResting) return
        ending = coroutineScope.launch {
            delay(SCROLL_END_QUIET_MILLIS)
            lastStep = null
            onEnd(step)
        }
    }

    override fun onDetach() {
        lastStep = null
        ending = null
    }
}

// Longer than the gap between the steps of a scroll still going: a frame while Compose slides through a notch or a
// touchpad glides, and the 50 ms Compose waits after its slide for the wheel's next notch.
private const val SCROLL_END_QUIET_MILLIS = 100L
