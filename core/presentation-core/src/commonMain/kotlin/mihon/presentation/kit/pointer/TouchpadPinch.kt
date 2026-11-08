package mihon.presentation.kit.pointer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.Flow

/** A step of two or more fingers pinching on a touchpad: a [Start], [Move]s and an [End]. */
sealed interface TouchpadPinch {
    data object Start : TouchpadPinch

    /**
     * The fingers are [scale] times as far apart as at the start, and their middle has moved by [pan] pixels since,
     * as a pinch that also slides moves it.
     */
    data class Move(val scale: Float, val pan: Offset) : TouchpadPinch

    data object End : TouchpadPinch
}

/**
 * The window's touchpad pinches as they happen, shared by every element that takes them. Null where the window gets
 * none, as on Android, where a pinch is touch.
 */
val LocalWindowTouchpadPinches = staticCompositionLocalOf<Flow<TouchpadPinch>?> { null }

/**
 * Calls [onPinch] with each step of the touchpad pinches that start while the pointer is over this element, and the
 * pointer's position in it, about which a zoom keeps its place as Ctrl and the wheel do. A touchpad doesn't move the
 * pointer while it pinches. The video player zooms its picture with it.
 */
@Composable
fun Modifier.touchpadPinch(onPinch: (pinch: TouchpadPinch, pointer: Offset) -> Unit): Modifier {
    val pinches = LocalWindowTouchpadPinches.current ?: return this
    val latestOnPinch by rememberUpdatedState(onPinch)
    val hover = remember { PinchHover() }
    LaunchedEffect(pinches, hover) {
        // A pinch stays with the element it started over
        var startedAt: Offset? = null
        pinches.collect { pinch ->
            if (pinch == TouchpadPinch.Start) startedAt = hover.pointer
            val start = startedAt ?: return@collect
            latestOnPinch(pinch, hover.pointer ?: start)
            if (pinch == TouchpadPinch.End) startedAt = null
        }
    }
    return pointerInput(hover) {
        awaitPointerEventScope {
            while (true) {
                // Watched before anything inside can consume it, and left for them
                val event = awaitPointerEvent(PointerEventPass.Initial)
                hover.pointer = if (event.type == PointerEventType.Exit) {
                    null
                } else {
                    event.changes.firstOrNull()?.position ?: hover.pointer
                }
            }
        }
    }
}

/** Where the pointer is over the element, null while it is elsewhere. */
private class PinchHover {
    var pointer: Offset? = null
}
