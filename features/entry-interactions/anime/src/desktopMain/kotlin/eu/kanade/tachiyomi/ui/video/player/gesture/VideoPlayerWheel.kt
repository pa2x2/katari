package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.control.mediaVolumeLevelsSwiped
import app.katari.presentation.kit.pointer.TouchpadSwipes
import app.katari.presentation.kit.pointer.isFingerOrWheelScroll
import app.katari.presentation.kit.pointer.touchpadScroll
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Scrolling over the picture. The wheel sets the volume, one percent a notch, with the side volume feedback
 * of Android's swipe; scrolling sideways, as a tilting wheel does, moves a target time as Android's sideways swipe
 * does, and seeks there once scrolling stops; with Ctrl held the wheel zooms the picture about the [pointer], which
 * shows. A touchpad's swipe is a drag, as a finger's on Android is: up or down it sets the volume and sideways the
 * target time, each as far as the fingers go, and the seek happens when they lift.
 */
internal fun Modifier.videoPlayerWheel(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
    pointer: VideoPlayerPointer,
): Modifier = pointerInput(target, pointer) {
    coroutineScope {
        var volumeSteps = 0f
        var seekUnits = 0f
        var seeking = false
        var seekEnd: Job? = null
        var zoomEnd: Job? = null
        var swipeVolumeFrom = 0
        var swipeSeeks = false
        val swipes = TouchpadSwipes(
            slop = viewConfiguration.touchSlop,
            onStart = { axis ->
                swipeSeeks = axis == Orientation.Horizontal
                if (swipeSeeks) {
                    target.startSeekSwipe()
                } else {
                    swipeVolumeFrom = target.sideGestureLevel(VideoPlayerSideGestureType.Volume)
                    true
                }
            },
            onMove = { _, distance ->
                if (swipeSeeks) {
                    target.seekSwipeBy((distance.toDp().value * SEEK_MS_PER_SWIPE_DP).roundToLong())
                    target.registerControllerInteraction(showControls = true)
                } else {
                    val level = swipeVolumeFrom + mediaVolumeLevelsSwiped(distance)
                    target.applySideGestureLevel(VideoPlayerSideGestureType.Volume, level)
                }
            },
            onEnd = { if (swipeSeeks) target.finishSeekSwipe(commit = true) },
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
                    delta.x != 0f -> {
                        if (!seeking) {
                            seeking = target.startSeekSwipe()
                            seekUnits = 0f
                        }
                        if (!seeking) continue
                        seekUnits += delta.x
                        target.seekSwipeBy((seekUnits * SEEK_MS_PER_UNIT).roundToLong())
                        target.registerControllerInteraction(showControls = true)
                        seekEnd?.cancel()
                        seekEnd = launch {
                            delay(SCROLL_END_MS)
                            seeking = false
                            target.finishSeekSwipe(commit = true)
                        }
                    }
                    delta.y != 0f -> {
                        // Up is negative, as the wheel scrolls toward the start
                        volumeSteps -= delta.y
                        val steps = volumeSteps.toInt()
                        if (steps == 0) continue
                        volumeSteps -= steps
                        val type = VideoPlayerSideGestureType.Volume
                        val level = target.sideGestureLevel(type) + steps
                        target.applySideGestureLevel(type, level)
                    }
                }
            }
        }
    }
}

private const val SEEK_MS_PER_UNIT = 5_000f

// A quick swipe is some 1,200 dp of scrolling, which seeks a minute
private const val SEEK_MS_PER_SWIPE_DP = 50f

// How long after the last scroll the seek or zoom it made is taken as done
private const val SCROLL_END_MS = 500L
