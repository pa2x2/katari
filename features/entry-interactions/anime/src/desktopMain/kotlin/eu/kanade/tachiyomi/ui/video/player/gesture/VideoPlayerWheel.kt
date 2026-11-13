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

/**
 * Scrolling over the picture. The wheel sets the volume, one percent a notch, with the side volume feedback
 * of Android's swipe; with Ctrl held the wheel zooms the picture about the [pointer], which shows. A touchpad's swipe
 * up or down is a drag, as a finger's on Android is, setting the volume as far as the fingers go. Scrolling sideways,
 * by a tilting wheel or a touchpad, does nothing.
 */
internal fun Modifier.videoPlayerWheel(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
    pointer: VideoPlayerPointer,
): Modifier = pointerInput(target, pointer) {
    coroutineScope {
        var volumeSteps = 0f
        var zoomEnd: Job? = null
        var swipeVolumeFrom = 0
        val swipes = TouchpadSwipes(
            slop = viewConfiguration.touchSlop,
            onStart = { axis ->
                if (axis == Orientation.Vertical) {
                    swipeVolumeFrom = target.sideGestureLevel(VideoPlayerSideGestureType.Volume)
                }
                axis == Orientation.Vertical
            },
            onMove = { _, travel ->
                val level = swipeVolumeFrom + mediaVolumeLevelsSwiped(travel)
                target.applySideGestureLevel(VideoPlayerSideGestureType.Volume, level)
            },
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

// How long after the last scroll the zoom it made is taken as done
private const val SCROLL_END_MS = 500L
