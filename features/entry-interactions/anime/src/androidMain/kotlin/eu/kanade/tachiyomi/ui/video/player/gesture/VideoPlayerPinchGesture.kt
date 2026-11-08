package eu.kanade.tachiyomi.ui.video.player.gesture

import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPicturePlacement
import kotlin.math.hypot

/**
 * Two fingers zoom and move the picture, keeping the point of the picture between them under them. Once a second finger
 * lands the touch is the pinch's until every finger lifts, so the finger left behind neither swipes nor taps. Each move
 * previews its framing on [target], and the last one is committed when the fingers lift.
 */
internal class VideoPlayerPinchGesture(private val target: VideoPlayerInputTarget) {
    var active = false
        private set

    private var geometry: VideoPictureGeometry? = null
    private var base = VideoPicturePlacement(1f, 0f, 0f)
    private var baseSpan = 0f
    private var baseFocusX = 0f
    private var baseFocusY = 0f
    private var latest: VideoPicturePlacement? = null

    /** Starts on a second finger; false while the video has no shape to zoom yet. */
    fun start(event: MotionEvent, playerWidth: Int, playerHeight: Int): Boolean {
        val aspectRatio = target.videoAspectRatio ?: return false
        if (playerWidth <= 0 || playerHeight <= 0) return false
        val geometry = VideoPictureGeometry(playerWidth.toFloat(), playerHeight.toFloat(), aspectRatio)
        this.geometry = geometry
        active = true
        latest = null
        rebase(event, geometry.place(target.pictureFraming), liftingIndex = -1)
        return true
    }

    fun onTouchEvent(event: MotionEvent) {
        val geometry = geometry ?: return
        when (event.actionMasked) {
            // The pinch carries on from where it is with the fingers now down.
            MotionEvent.ACTION_POINTER_DOWN -> rebase(event, latest ?: base, liftingIndex = -1)
            MotionEvent.ACTION_POINTER_UP -> rebase(event, latest ?: base, liftingIndex = event.actionIndex)
            MotionEvent.ACTION_MOVE -> if (event.pointerCount >= 2) move(event, geometry)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                latest?.let { target.commitPictureFraming(geometry.framingOf(it)) }
                active = false
                this.geometry = null
                latest = null
            }
        }
    }

    private fun move(event: MotionEvent, geometry: VideoPictureGeometry) {
        if (baseSpan <= 0f) return
        val placement = geometry.pinch(
            base = base,
            factor = span(event, liftingIndex = -1) / baseSpan,
            fromX = baseFocusX,
            fromY = baseFocusY,
            toX = focusX(event, liftingIndex = -1),
            toY = focusY(event, liftingIndex = -1),
        )
        latest = placement
        target.previewPictureFraming(geometry.framingOf(placement))
    }

    private fun rebase(event: MotionEvent, from: VideoPicturePlacement, liftingIndex: Int) {
        base = from
        val fingers = event.pointerCount - if (liftingIndex >= 0) 1 else 0
        if (fingers < 2) {
            baseSpan = 0f
            return
        }
        baseSpan = span(event, liftingIndex)
        baseFocusX = focusX(event, liftingIndex)
        baseFocusY = focusY(event, liftingIndex)
    }

    // The first two fingers still down; [liftingIndex] is one lifting in this event, or -1.
    private fun fingers(event: MotionEvent, liftingIndex: Int): Pair<Int, Int> {
        val down = (0 until event.pointerCount).filter { it != liftingIndex }
        return down[0] to down[1]
    }

    private fun span(event: MotionEvent, liftingIndex: Int): Float {
        val (a, b) = fingers(event, liftingIndex)
        return hypot(event.getX(a) - event.getX(b), event.getY(a) - event.getY(b))
    }

    private fun focusX(event: MotionEvent, liftingIndex: Int): Float {
        val (a, b) = fingers(event, liftingIndex)
        return (event.getX(a) + event.getX(b)) / 2f
    }

    private fun focusY(event: MotionEvent, liftingIndex: Int): Float {
        val (a, b) = fingers(event, liftingIndex)
        return (event.getY(a) + event.getY(b)) / 2f
    }
}
