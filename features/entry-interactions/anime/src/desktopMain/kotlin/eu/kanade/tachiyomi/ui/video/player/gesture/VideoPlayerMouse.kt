package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The mouse's buttons and moves on the picture. Moving shows the controls, which hide again on their own. A click
 * plays or pauses as soon as the button is let go; the second click of a double-click plays or pauses back and switches
 * full screen, so a double-click leaves playback and the controls as they were, as browsers' players do. Holding the
 * left button plays at 2× until it is let go. While the picture is zoomed past the player, dragging with the left
 * button moves it, and a hold that doesn't move still plays at 2×. The right button opens the settings panel; the
 * middle one does nothing, and the back button is the window's, which goes back as Esc does. [geometry] is the
 * picture's shape in the player, null until the video has one.
 */
internal fun Modifier.videoPlayerMouse(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
): Modifier = pointerInput(target) {
    coroutineScope {
        var lastClickAtMs: Long? = null
        var lastClickAt = Offset.Zero
        var controlsVisibleBeforeClick = false
        var lastAt = Offset.Unspecified
        var press: Press? = null
        var hold: Job? = null
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                val current = press
                when (event.type) {
                    PointerEventType.Move -> {
                        // The window repeats the last move whenever what is under a still pointer is laid out anew
                        if (change.position == lastAt) continue
                        lastAt = change.position
                        target.registerControllerInteraction(showControls = true)
                        if (current == null || current.boosted) continue
                        val moved = (change.position - current.at).getDistance() > viewConfiguration.touchSlop
                        if (!current.dragging && moved) {
                            val picture = geometry()
                            if (picture != null && picture.overflows(target.pictureFraming)) current.dragging = true
                            current.clicks = false
                            hold?.cancel()
                        }
                        if (current.dragging) {
                            val picture = geometry() ?: continue
                            val delta = change.position - change.previousPosition
                            target.previewPictureFraming(picture.pan(target.pictureFraming, delta.x, delta.y))
                        }
                    }
                    PointerEventType.Press -> when (event.button) {
                        PointerButton.Secondary -> if (!target.subtitleEditorVisible) target.openSettings()
                        PointerButton.Primary -> {
                            if (target.subtitleEditorVisible) continue
                            val sinceClickMs = lastClickAtMs?.let { change.uptimeMillis - it }
                            val double = sinceClickMs != null &&
                                sinceClickMs <= viewConfiguration.doubleTapTimeoutMillis &&
                                (change.position - lastClickAt).getDistance() <= viewConfiguration.touchSlop
                            if (double) {
                                lastClickAtMs = null
                                press = Press(change.position, clicks = false)
                                target.togglePlayback()
                                if (!controlsVisibleBeforeClick) target.hideControls()
                                if (target.hasFullScreen) target.toggleFullScreen()
                                continue
                            }
                            val started = Press(change.position, clicks = true)
                            press = started
                            hold = launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                started.clicks = false
                                if (!target.panelOpen) {
                                    started.boosted = true
                                    target.setSpeedBoostActive(true)
                                    target.registerControllerInteraction(showControls = false)
                                }
                            }
                        }
                        else -> Unit
                    }
                    PointerEventType.Release -> {
                        if (event.button != PointerButton.Primary || current == null) continue
                        press = null
                        hold?.cancel()
                        when {
                            current.boosted -> target.setSpeedBoostActive(false)
                            current.dragging -> target.commitPictureFraming(target.pictureFraming)
                            current.clicks -> {
                                lastClickAtMs = change.uptimeMillis
                                lastClickAt = change.position
                                controlsVisibleBeforeClick = target.controlsVisible
                                target.togglePlayback()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A left button press on the picture, until it is let go. */
private class Press(val at: Offset, var clicks: Boolean) {
    var dragging = false
    var boosted = false
}
