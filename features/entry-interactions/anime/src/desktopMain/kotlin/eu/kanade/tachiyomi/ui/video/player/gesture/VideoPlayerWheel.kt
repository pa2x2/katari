package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.control.mediaLevelsSwiped
import app.katari.presentation.kit.pointer.TouchpadSwipes
import app.katari.presentation.kit.pointer.isFingerOrWheelScroll
import app.katari.presentation.kit.pointer.touchpadScroll
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * Scrolling over the picture. The wheel sets the volume, one percent a notch, with the side volume feedback
 * of Android's swipe; with Shift held it sets the extra dim instead, a level a notch, with the brightness feedback, and
 * with Ctrl held it zooms the picture about the [pointer], which shows. A touchpad's swipe up or down is a drag, as a
 * finger's on Android is, setting the volume, or with Shift held at its start the extra dim, as far as the fingers go.
 * Scrolling sideways, by a tilting wheel or a touchpad, does nothing.
 */
internal fun Modifier.videoPlayerWheel(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
    pointer: VideoPlayerPointer,
): Modifier = pointerInput(target, pointer) {
    coroutineScope {
        var wheelSteps = 0f
        var wheelType = VideoPlayerSideGestureType.Volume
        var zoomEnd: Job? = null
        var shiftHeld = false
        var swipeType = VideoPlayerSideGestureType.Volume
        var swipeFrom = 0
        val swipes = TouchpadSwipes(
            slop = viewConfiguration.touchSlop,
            onStart = { axis ->
                if (axis == Orientation.Vertical) {
                    swipeType = sideGestureFor(shiftHeld)
                    swipeFrom = target.sideGestureLevel(swipeType)
                }
                axis == Orientation.Vertical
            },
            onMove = { _, travel -> target.applySideGestureLevel(swipeType, swipeFrom + mediaLevelsSwiped(travel)) },
        )
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Exit) swipes.end()
                if (event.type != PointerEventType.Scroll) continue
                if (target.panelOpen || target.subtitleEditorVisible) {
                    swipes.end()
                    continue
                }
                val change = event.changes.firstOrNull() ?: continue
                val delta = change.scrollDelta
                change.consume()
                val touchpad = event.touchpadScroll
                shiftHeld = event.keyboardModifiers.isShiftPressed
                // AWT turns a wheel's notches sideways while Shift is held, so with Shift either axis is the wheel's
                val wheelDelta = if (shiftHeld && delta.y == 0f) delta.x else delta.y
                when {
                    event.keyboardModifiers.isCtrlPressed -> {
                        if (!event.isFingerOrWheelScroll) continue
                        val picture = geometry() ?: continue
                        pointer.moved()
                        val factor = PICTURE_ZOOM_STEP.pow(-delta.y)
                        target.previewPictureFraming(
                            picture.zoom(target.pictureFraming, factor, change.position.x, change.position.y),
                        )
                        zoomEnd?.cancel()
                        zoomEnd = launch {
                            delay(SCROLL_END_MS)
                            target.commitPictureFraming(target.pictureFraming)
                        }
                    }
                    touchpad != null -> swipes.onStep(touchpad)
                    wheelDelta != 0f -> {
                        val type = sideGestureFor(shiftHeld)
                        if (type != wheelType) {
                            wheelType = type
                            wheelSteps = 0f
                        }
                        // Up is negative, as the wheel scrolls toward the start
                        wheelSteps -= wheelDelta
                        val steps = wheelSteps.toInt()
                        if (steps == 0) continue
                        wheelSteps -= steps
                        target.applySideGestureLevel(type, target.sideGestureLevel(type) + steps)
                    }
                }
            }
        }
    }
}

private fun sideGestureFor(shiftHeld: Boolean) =
    if (shiftHeld) VideoPlayerSideGestureType.Brightness else VideoPlayerSideGestureType.Volume

// How long after the last scroll the zoom it made is taken as done
private const val SCROLL_END_MS = 500L
