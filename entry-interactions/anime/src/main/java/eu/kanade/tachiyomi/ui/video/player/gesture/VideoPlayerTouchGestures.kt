package eu.kanade.tachiyomi.ui.video.player.gesture

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import tachiyomi.domain.entry.model.PictureFraming
import kotlin.math.abs

private const val SIDE_GESTURE_VERTICAL_RATIO = 1.35f

/**
 * What the touch gestures on the player view read and drive. Each event reads it anew, so an implementation answers
 * from the screen's state at that moment rather than from values captured when it was built.
 */
internal interface VideoPlayerTouchTarget {
    val subtitleEditorVisible: Boolean
    val controlsLocked: Boolean
    val controlsVisible: Boolean
    val settingsVisible: Boolean

    /** True while a seek gesture's feedback is up, when single taps on either side seek again. */
    val seekGestureModeActive: Boolean
    val speedBoostEligible: Boolean
    val speedBoostActive: Boolean

    /** Set when a double tap has seeked, so the release of that tap doesn't seek a second time. */
    var ignoreNextSeekTapUp: Boolean

    fun setSpeedBoostActive(active: Boolean)

    fun hideControls()

    fun registerControllerInteraction(showControls: Boolean)

    fun performGestureSeek(direction: VideoPlayerSeekDirection)

    fun sideGestureLevel(type: VideoPlayerSideGestureType): Int

    fun sideGestureMaxLevel(type: VideoPlayerSideGestureType): Int

    fun applySideGestureLevel(type: VideoPlayerSideGestureType, level: Int)

    /** The video's shape, null until it has one. */
    val videoAspectRatio: Float?

    /** The framing on screen, a pinch's preview included. */
    val pictureFraming: PictureFraming

    fun previewPictureFraming(framing: PictureFraming)

    fun commitPictureFraming(framing: PictureFraming)
}

/**
 * Taps, double taps, holds and drags on the player: a tap toggles the controls, a double tap on either side seeks, a
 * hold in the speed zones boosts the speed until released, a vertical drag on the left or right half changes
 * brightness or volume, and two fingers zoom and move the picture. Drags starting within [edgeExclusionPx] of the top
 * or bottom are left alone. [pinchGesture] is kept by the caller, so a pinch survives the listener being set again.
 */
internal fun View.setVideoPlayerTouchGestures(
    target: VideoPlayerTouchTarget,
    pinchGesture: VideoPlayerPinchGesture,
    edgeExclusionPx: Float,
) {
    val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var sideGestureState: SideGestureState? = null
    var holdSpeedGestureState: HoldSpeedGestureState? = null
    val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onLongPress(e: MotionEvent) {
                val currentHoldState = holdSpeedGestureState ?: return
                if (
                    currentHoldState.cancelled ||
                    currentHoldState.longPressTriggered ||
                    !target.speedBoostEligible
                ) {
                    return
                }
                if (
                    isTouchInHoldSpeedZone(
                        touchX = currentHoldState.startX,
                        touchY = currentHoldState.startY,
                        playerWidth = width,
                        playerHeight = height,
                    )
                ) {
                    holdSpeedGestureState = currentHoldState.copy(longPressTriggered = true)
                    target.setSpeedBoostActive(true)
                    target.registerControllerInteraction(false)
                }
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (target.speedBoostActive) {
                    return true
                }
                if (target.controlsLocked) {
                    if (target.controlsVisible) {
                        target.hideControls()
                    } else {
                        target.registerControllerInteraction(true)
                    }
                    return true
                }
                if (!target.settingsVisible && !target.subtitleEditorVisible) {
                    if (target.seekGestureModeActive) {
                        return true
                    }
                    if (target.controlsVisible) {
                        target.hideControls()
                    } else {
                        target.registerControllerInteraction(true)
                    }
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (target.speedBoostActive) {
                    return true
                }
                if (target.controlsLocked) {
                    return true
                }
                if (!target.settingsVisible && !target.subtitleEditorVisible) {
                    val direction = resolveVideoPlayerSeekDirectionFromTap(
                        tapX = e.x,
                        viewportWidth = width.toFloat(),
                    )
                    if (direction != null) {
                        target.ignoreNextSeekTapUp = true
                        target.performGestureSeek(direction)
                        return true
                    }
                }
                return false
            }
        },
    )
    setOnTouchListener { _, motionEvent ->
        if (target.subtitleEditorVisible) {
            return@setOnTouchListener true
        }

        if (target.controlsLocked) {
            val handled = gestureDetector.onTouchEvent(motionEvent)
            if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                performClick()
            }
            return@setOnTouchListener handled
        }

        if (
            motionEvent.actionMasked == MotionEvent.ACTION_POINTER_DOWN &&
            !pinchGesture.active &&
            !target.settingsVisible &&
            pinchGesture.start(motionEvent, width, height)
        ) {
            // The first finger's hold, swipe or tap ends here.
            target.setSpeedBoostActive(false)
            holdSpeedGestureState = null
            sideGestureState = null
            target.hideControls()
            val cancel = MotionEvent.obtain(motionEvent).apply { action = MotionEvent.ACTION_CANCEL }
            gestureDetector.onTouchEvent(cancel)
            cancel.recycle()
            return@setOnTouchListener true
        }
        if (pinchGesture.active) {
            pinchGesture.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                target.setSpeedBoostActive(false)
                sideGestureState = SideGestureState(
                    startX = motionEvent.x,
                    startY = motionEvent.y,
                    initialLevel = 0,
                )
                holdSpeedGestureState = HoldSpeedGestureState(
                    startX = motionEvent.x,
                    startY = motionEvent.y,
                )
            }

            MotionEvent.ACTION_MOVE -> {
                val currentHoldState = holdSpeedGestureState
                if (currentHoldState != null) {
                    val movedBeyondSlop =
                        abs(motionEvent.x - currentHoldState.startX) > touchSlop ||
                            abs(motionEvent.y - currentHoldState.startY) > touchSlop
                    if (
                        currentHoldState.longPressTriggered &&
                        !isTouchInHoldSpeedZone(
                            touchX = motionEvent.x,
                            touchY = motionEvent.y,
                            playerWidth = width,
                            playerHeight = height,
                        )
                    ) {
                        target.setSpeedBoostActive(false)
                        holdSpeedGestureState = currentHoldState.copy(
                            cancelled = true,
                            longPressTriggered = false,
                        )
                    } else if (!currentHoldState.longPressTriggered && movedBeyondSlop) {
                        holdSpeedGestureState = currentHoldState.copy(cancelled = true)
                    }
                }
                val currentSideGestureState = sideGestureState
                if (
                    currentSideGestureState != null &&
                    !target.settingsVisible &&
                    !target.seekGestureModeActive
                ) {
                    val deltaX = motionEvent.x - currentSideGestureState.startX
                    val deltaY = motionEvent.y - currentSideGestureState.startY
                    if (
                        !currentSideGestureState.active &&
                        abs(deltaY) > touchSlop &&
                        abs(deltaY) > abs(deltaX) * SIDE_GESTURE_VERTICAL_RATIO
                    ) {
                        val gestureType = resolveSideGestureType(
                            startX = currentSideGestureState.startX,
                            startY = currentSideGestureState.startY,
                            playerWidth = width,
                            playerHeight = height,
                            edgeExclusionPx = edgeExclusionPx,
                        )
                        if (gestureType != null) {
                            sideGestureState = currentSideGestureState.copy(
                                type = gestureType,
                                initialLevel = target.sideGestureLevel(gestureType),
                                active = true,
                            )
                        }
                    }

                    val activeSideGestureState = sideGestureState
                    if (activeSideGestureState?.active == true) {
                        val dragFraction = verticalGestureFraction(
                            startY = activeSideGestureState.startY,
                            currentY = motionEvent.y,
                            playerWidth = width,
                            playerHeight = height,
                        )
                        activeSideGestureState.type?.let { type ->
                            val targetLevel = activeSideGestureState.initialLevel +
                                (dragFraction * target.sideGestureMaxLevel(type)).toInt()
                            target.applySideGestureLevel(type, targetLevel)
                        }
                        target.hideControls()
                        target.registerControllerInteraction(false)
                        return@setOnTouchListener true
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                target.setSpeedBoostActive(false)
                holdSpeedGestureState = null
                if (sideGestureState?.active == true) {
                    sideGestureState = null
                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                        performClick()
                    }
                    return@setOnTouchListener true
                }
                sideGestureState = null
            }
        }

        val handled = gestureDetector.onTouchEvent(motionEvent)
        if (target.speedBoostActive) {
            return@setOnTouchListener true
        }
        if (
            !target.settingsVisible &&
            target.seekGestureModeActive &&
            motionEvent.actionMasked == MotionEvent.ACTION_UP
        ) {
            val direction = resolveVideoPlayerSeekDirectionFromTap(
                tapX = motionEvent.x,
                viewportWidth = width.toFloat(),
            )
            if (direction != null) {
                if (target.ignoreNextSeekTapUp) {
                    target.ignoreNextSeekTapUp = false
                } else {
                    target.performGestureSeek(direction)
                }
                return@setOnTouchListener true
            }
        }
        if (motionEvent.actionMasked == MotionEvent.ACTION_UP && !handled) {
            performClick()
        }
        handled
    }
}

private data class SideGestureState(
    val startX: Float,
    val startY: Float,
    val type: VideoPlayerSideGestureType? = null,
    val initialLevel: Int,
    val active: Boolean = false,
)

private data class HoldSpeedGestureState(
    val startX: Float,
    val startY: Float,
    val longPressTriggered: Boolean = false,
    val cancelled: Boolean = false,
)
