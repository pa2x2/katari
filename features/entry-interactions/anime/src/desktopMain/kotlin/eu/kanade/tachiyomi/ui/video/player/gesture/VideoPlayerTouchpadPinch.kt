package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPicturePlacement
import mihon.presentation.kit.pointer.TouchpadPinch
import mihon.presentation.kit.pointer.touchpadPinch

/**
 * A touchpad pinch over the picture zooms it about the [pointer], which shows, and moves it as the fingers slide, as
 * two fingers do on Android's picture. Each step previews its framing, and the end commits it.
 */
@Composable
internal fun Modifier.videoPlayerTouchpadPinch(
    target: VideoPlayerInputTarget,
    geometry: () -> VideoPictureGeometry?,
    pointer: VideoPlayerPointer,
): Modifier {
    val pinch = remember(target, geometry, pointer) { PicturePinch(target, geometry, pointer) }
    return touchpadPinch(pinch::onPinch)
}

private class PicturePinch(
    private val target: VideoPlayerInputTarget,
    private val geometry: () -> VideoPictureGeometry?,
    private val pointer: VideoPlayerPointer,
) {
    private var start: Start? = null

    fun onPinch(pinch: TouchpadPinch, at: Offset) {
        when (pinch) {
            TouchpadPinch.Start -> start = startAt(at)
            is TouchpadPinch.Move -> {
                val start = start ?: return
                pointer.moved()
                val placement = start.geometry.pinch(
                    base = start.placement,
                    factor = pinch.scale,
                    fromX = start.pointer.x,
                    fromY = start.pointer.y,
                    toX = start.pointer.x + pinch.pan.x,
                    toY = start.pointer.y + pinch.pan.y,
                )
                target.previewPictureFraming(start.geometry.framingOf(placement))
            }
            TouchpadPinch.End -> if (start != null) {
                start = null
                target.commitPictureFraming(target.pictureFraming)
            }
        }
    }

    // None while a panel or the subtitle editor takes the pointer, as for the wheel, or the video has no shape yet
    private fun startAt(at: Offset): Start? {
        if (target.panelOpen || target.subtitleEditorVisible) return null
        val geometry = geometry() ?: return null
        return Start(geometry, geometry.place(target.pictureFraming), at)
    }

    private class Start(val geometry: VideoPictureGeometry, val placement: VideoPicturePlacement, val pointer: Offset)
}
