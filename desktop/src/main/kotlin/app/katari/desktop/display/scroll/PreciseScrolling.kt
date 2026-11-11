package app.katari.desktop.display.scroll

import androidx.compose.ui.geometry.Offset
import app.katari.desktop.display.scroll.natural.TouchpadScrollDirection
import app.katari.presentation.kit.pointer.TouchpadScroll
import app.katari.presentation.kit.pointer.TouchpadScrollStage
import app.katari.presentation.kit.pointer.TouchpadWheelEvent
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Scrolls [window] by the display's own [ScrollMotion]s, as wheel events it hands the window in place of the
 * toolkit's. A wheel's notches scroll as the toolkit's do. A touchpad's fingers move the content by [FINGER_GAIN]
 * times the travel the display reports for them, [sessionScale] screen pixels for each of the session's, or the
 * toolkit's own scale where that is null, and a flick leaves it gliding once they lift, until the next scroll, a
 * click, a key or the pointer moving off. Their lifting comes as a step of its own that scrolls by nothing, for what
 * follows the fingers and stops with them. Each swipe scrolls in the [direction] that holds when it starts, turning
 * the display's steps around where the app scrolls the other way than the system, and its steps say whether content
 * moves with the fingers, so what follows the fingers knows which way they go.
 *
 * Compose scrolls a wheel's turn by a distance that grows with the root of the scrolling content's size, which the
 * window can't know for the content under the pointer, so that distance is exact for content that takes most of the
 * window, as a screen's list does, and falls short in smaller content. Used by the event dispatch thread.
 */
internal class PreciseScrolling(
    private val window: Window,
    private val sessionScale: Float?,
    private val direction: () -> TouchpadScrollDirection,
) : AutoCloseable {
    private val velocity = FingerVelocity()
    private val vertical = FingerTravel()
    private val horizontal = FingerTravel()
    private val verticalNotches = WheelTurn()
    private val horizontalNotches = WheelTurn()

    // Numbers the fingers' scrolls, for what counts them
    private var swipe = 0
    private var swipeDirection = TouchpadScrollDirection(turnsContent = false, contentFollowsFingers = false)

    // What turns a time on the display's clock into AWT's, the system's
    private var clockOffset: Long? = null

    private var glide: RunningGlide? = null
    private val glideTimer = Timer(GLIDE_TICK_MS) { glideOn() }

    // While gliding: what the user does to stop the content
    private val glideEnds = AWTEventListener { event ->
        val at = glide?.at ?: return@AWTEventListener
        val stops = when (event.id) {
            MouseEvent.MOUSE_PRESSED, KeyEvent.KEY_PRESSED -> true
            MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED -> {
                val mouse = event as MouseEvent
                val scale = awtScale()
                hypot(mouse.xOnScreen * scale - at.xOnScreen, mouse.yOnScreen * scale - at.yOnScreen) >
                    POINTER_SLOP * screenPixelsPerSessionPixel()
            }
            else -> false
        }
        if (stops) endGlide()
    }

    fun on(motion: ScrollMotion) {
        endGlide()
        when (motion) {
            is ScrollMotion.Wheel -> {
                verticalNotches.turn(motion.vertical)?.let {
                    send(wheelEvent(motion.at, motion.vertical, it, sideways = false))
                }
                horizontalNotches.turn(motion.horizontal)?.let {
                    send(wheelEvent(motion.at, motion.horizontal, it, sideways = true))
                }
            }
            is ScrollMotion.Fingers -> {
                if (motion.starts) {
                    swipe++
                    swipeDirection = direction()
                    vertical.reset()
                    horizontal.reset()
                    velocity.start()
                }
                val gain = if (swipeDirection.turnsContent) -FINGER_GAIN else FINGER_GAIN
                val x = motion.horizontal * gain
                val y = motion.vertical * gain
                velocity.add(motion.at.timeMillis, x, y)
                moveBy(motion.at, x, y, TouchpadScrollStage.Fingers)
            }
            is ScrollMotion.FingersLifted -> {
                send(fingersEvent(motion.at, pixels = 0.0, TouchpadScrollStage.Lifted, sideways = false))
                val (velocityX, velocityY) = velocity.atLift(motion.at.timeMillis)
                val leaves = ScrollGlide.from(velocityX, velocityY) ?: return
                glide = RunningGlide(leaves, motion.at, System.nanoTime())
                Toolkit.getDefaultToolkit().addAWTEventListener(glideEnds, GLIDE_END_EVENTS)
                glideTimer.start()
            }
        }
    }

    override fun close() = endGlide()

    private fun glideOn() {
        val running = glide ?: return
        val elapsedMillis = (System.nanoTime() - running.startedNanos) / NANOS_PER_MILLI
        val (x, y) = running.glide.travelled(elapsedMillis)
        val at = running.at.copy(timeMillis = running.at.timeMillis + elapsedMillis.toLong())
        moveBy(at, x - running.travelledX, y - running.travelledY, TouchpadScrollStage.Gliding)
        running.travelledX = x
        running.travelledY = y
        if (running.glide.isOver(elapsedMillis)) endGlide()
    }

    private fun endGlide() {
        if (glide == null) return
        glide = null
        glideTimer.stop()
        Toolkit.getDefaultToolkit().removeAWTEventListener(glideEnds)
    }

    /** Moves the content by [x] and [y] of the session's pixels, once they come to a screen pixel. */
    private fun moveBy(at: ScrollPoint, x: Double, y: Double, stage: TouchpadScrollStage) {
        val scale = screenPixelsPerSessionPixel()
        val verticalPixels = vertical.move(y * scale)
        val horizontalPixels = horizontal.move(x * scale)
        if (verticalPixels != null) send(fingersEvent(at, verticalPixels, stage, sideways = false))
        if (horizontalPixels != null) send(fingersEvent(at, horizontalPixels, stage, sideways = true))
    }

    private fun fingersEvent(
        at: ScrollPoint,
        pixels: Double,
        stage: TouchpadScrollStage,
        sideways: Boolean,
    ): MouseWheelEvent {
        val scale = awtScale()
        val insets = window.insets
        val extent = if (sideways) {
            window.width - insets.left - insets.right
        } else {
            window.height - insets.top - insets.bottom
        }
        // What Compose's Linux scrolling multiplies a rotation by, for content that takes most of the window
        val contentExtent = (extent * scale * CONTENT_SHARE).coerceAtLeast(1.0)
        val pixelsPerRotation = TouchpadWheelEvent.NOTCH_SCROLL_AMOUNT * sqrt(contentExtent)
        val place = place(at, scale)
        val modifiers = modifiers(at, sideways)
        // Along the axis Compose takes the event for, which Shift turns sideways
        val distance = pixels.toFloat().let {
            if (modifiers and InputEvent.SHIFT_DOWN_MASK != 0) Offset(it, 0f) else Offset(0f, it)
        }
        return TouchpadWheelEvent(
            source = place.target,
            whenMillis = awtTime(at),
            modifiers = modifiers,
            x = place.x,
            y = place.y,
            xOnScreen = place.xOnScreen,
            yOnScreen = place.yOnScreen,
            rotation = pixels / pixelsPerRotation,
            scroll = TouchpadScroll(swipe, stage, distance, swipeDirection.contentFollowsFingers),
        )
    }

    private fun wheelEvent(at: ScrollPoint, notches: Double, wholeNotches: Int, sideways: Boolean): MouseWheelEvent {
        val place = place(at, awtScale())
        return MouseWheelEvent(
            place.target,
            MouseEvent.MOUSE_WHEEL,
            awtTime(at),
            modifiers(at, sideways),
            place.x,
            place.y,
            place.xOnScreen,
            place.yOnScreen,
            0,
            false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL,
            TouchpadWheelEvent.NOTCH_SCROLL_AMOUNT,
            wholeNotches,
            notches,
        )
    }

    // Not through the event queue, which would take it for one of the toolkit's
    private fun send(event: MouseWheelEvent) {
        if (window.isShowing) event.component.dispatchEvent(event)
    }

    /** Where an event goes: [x] and [y] in [target]. */
    private class Place(val target: Component, val x: Int, val y: Int, val xOnScreen: Int, val yOnScreen: Int)

    /**
     * The component under the pointer that listens for the wheel, which is where the toolkit's wheel events end up.
     * AWT hands an event sent to the window down to it as a copy, a plain wheel event that has lost what a
     * [TouchpadWheelEvent] adds, so the event goes there itself.
     */
    private fun place(at: ScrollPoint, scale: Double): Place {
        val xOnScreen = (at.xOnScreen / scale).roundToInt()
        val yOnScreen = (at.yOnScreen / scale).roundToInt()
        val corner = if (window.isShowing) window.locationOnScreen else window.location
        val inWindow = Point(xOnScreen - corner.x, yOnScreen - corner.y)
        val target =
            generateSequence(SwingUtilities.getDeepestComponentAt(window, inWindow.x, inWindow.y)) { it.parent }
                .firstOrNull { it.mouseWheelListeners.isNotEmpty() }
                ?: window
        val inTarget = SwingUtilities.convertPoint(window, inWindow, target)
        return Place(target, inTarget.x, inTarget.y, xOnScreen, yOnScreen)
    }

    // Compose scrolls sideways for a wheel event with Shift held, which is how AWT tells a sideways wheel
    private fun modifiers(at: ScrollPoint, sideways: Boolean): Int =
        if (sideways) at.modifiers or InputEvent.SHIFT_DOWN_MASK else at.modifiers

    private fun awtTime(at: ScrollPoint): Long {
        val now = System.currentTimeMillis()
        val offset = clockOffset?.takeIf { abs(at.timeMillis + it - now) <= CLOCK_DRIFT_MS } ?: (now - at.timeMillis)
        clockOffset = offset
        return at.timeMillis + offset
    }

    private fun screenPixelsPerSessionPixel(): Double = sessionScale?.toDouble() ?: awtScale()

    /** Screen pixels for each of AWT's, which are the toolkit's whole-number scale apart. */
    private fun awtScale(): Double = window.graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0

    private class RunningGlide(val glide: ScrollGlide, val at: ScrollPoint, val startedNanos: Long) {
        var travelledX = 0.0
        var travelledY = 0.0
    }

    /** What the fingers have moved an axis by that no event has carried yet. */
    private class FingerTravel {
        private var pending = 0.0

        fun reset() {
            pending = 0.0
        }

        /**
         * Adds [pixels] of the fingers' travel and returns what to scroll by now; null until it comes to a pixel, as
         * Compose drops a scroll of less than half of one rather than keeping it for the next.
         */
        fun move(pixels: Double): Double? {
            pending += pixels
            if (abs(pending) < 1) return null
            return pending.also { pending = 0.0 }
        }
    }

    /** The whole notches a wheel has turned along an axis, which a wheel event carries beside the turn itself. */
    private class WheelTurn {
        private var pending = 0.0

        /** Adds a turn of [notches] and returns the whole notches it has come to since the last; null for no turn. */
        fun turn(notches: Double): Int? {
            if (notches == 0.0) return null
            pending += notches
            return pending.toInt().also { pending -= it }
        }
    }

    private companion object {
        /**
         * How far content moves for the travel libinput reports for a touchpad's fingers, which is a fraction of the
         * distance they cover, so no toolkit scrolls by it as it is: GTK 4 scrolls 2.5 times as far, Firefox 4 times,
         * Qt's lists about 6. Firefox's, as the app scrolls what a browser does: pages and long lists.
         */
        const val FINGER_GAIN = 4.0

        // The share of the window a screen's list takes beside its bars, for which the fingers' travel is exact
        const val CONTENT_SHARE = 0.85

        const val GLIDE_TICK_MS = 8
        const val GLIDE_END_EVENTS =
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or AWTEvent.KEY_EVENT_MASK

        // How far, in the session's pixels, the pointer may stray under fingers that only lift
        const val POINTER_SLOP = 8.0
        const val NANOS_PER_MILLI = 1e6

        // The two clocks apart by more than this have been set anew, or the display's has wrapped around
        const val CLOCK_DRIFT_MS = 1_000L
    }
}
