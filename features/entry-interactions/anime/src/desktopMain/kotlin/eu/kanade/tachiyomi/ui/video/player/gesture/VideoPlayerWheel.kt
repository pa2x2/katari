package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Scrolling over the picture. The wheel sets the volume, one percent a notch, with the side volume feedback
 * of Android's swipe; scrolling sideways, as a touchpad or a tilting wheel does, moves a target time as Android's
 * sideways swipe does, and seeks there once scrolling stops; with Ctrl held the wheel zooms the picture about the
 * pointer. Notches come as whole units of scrolling and touchpads as fractions of one, which add up.
 */
internal fun Modifier.videoPlayerWheel(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
): Modifier = pointerInput(target) {
    coroutineScope {
        var volumeSteps = 0f
        var seekUnits = 0f
        var seeking = false
        var seekEnd: Job? = null
        var zoomEnd: Job? = null
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type != PointerEventType.Scroll || target.panelOpen || target.subtitleEditorVisible) continue
                val change = event.changes.firstOrNull() ?: continue
                val delta = change.scrollDelta
                change.consume()
                when {
                    event.keyboardModifiers.isCtrlPressed -> {
                        val picture = geometry() ?: continue
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

// How long after the last scroll the seek or zoom it made is taken as done
private const val SCROLL_END_MS = 500L
