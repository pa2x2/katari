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
 * The mouse's buttons and moves on the picture. Moving shows the controls, which hide again on their own, but not
 * while the left button is held, as it drags the picture or plays at 2× then, nor away from the bars over a picture
 * zoomed past the player, where it shows only the [pointer]. A click plays or pauses once it can no longer become a
 * double-click ([PictureClicks]), and a double-click only switches full screen, leaving playback and the controls as
 * they were. Holding the left button plays at 2× until it is let go. While the picture is zoomed past the player,
 * dragging with the left button moves it, and a hold that doesn't move still plays at 2×. The right button opens the
 * settings panel; the middle one does nothing, and the back button is the window's, which goes back as Esc does.
 * [geometry] is the picture's shape in the player, null until the video has one.
 */
internal fun Modifier.videoPlayerMouse(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
    pointer: VideoPlayerPointer,
): Modifier = pointerInput(target, pointer) {
    coroutineScope {
        val clicks = PictureClicks(this, viewConfiguration.doubleTapTimeoutMillis, viewConfiguration.touchSlop)
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
                        pointer.moved()
                        if (current == null) {
                            if (pointer.showsControlsAt(change.position.y, target, geometry())) {
                                target.registerControllerInteraction(showControls = true)
                            }
                            continue
                        }
                        if (current.boosted) continue
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
                            if (clicks.completesDoubleClick(change.position)) {
                                press = Press(change.position, clicks = false)
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
                            current.clicks -> clicks.clicked(change.position) {
                                target.togglePlayback(showControls = true)
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
