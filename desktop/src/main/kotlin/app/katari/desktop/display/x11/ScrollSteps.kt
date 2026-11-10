package app.katari.desktop.display.x11

import app.katari.desktop.display.scroll.ScrollMotion
import app.katari.desktop.display.scroll.ScrollPoint
import kotlin.math.abs
import kotlin.math.roundToLong

/** What scrolls a device's axes, which says what a step of them is. */
internal enum class ScrollDeviceKind {
    /** A step is a notch. */
    Wheel,

    /** A touchpad that libinput drives on an X server: a step is [LIBINPUT_STEP_PIXELS] of the fingers' travel. */
    Touchpad,

    /**
     * Xwayland's one pointer for every device of the session: a step is a wheel's notch, or [XWAYLAND_STEP_PIXELS] of
     * a touchpad's fingers, and only the steps' values tell which.
     */
    Xwayland,
}

/**
 * Turns the steps a device's scroll axes moved by into what they mean: a wheel's notches, or the pixels a touchpad's
 * fingers moved by, with the fingers' first step and their lifting, which both X servers report as a scroll of
 * nothing.
 *
 * Xwayland doesn't say which device scrolled. A wheel's steps reach it as 120ths of a notch, so they are multiples of
 * 1/120; a finger's are the compositor's pixels over ten, in 256ths of a pixel, which only one in 64 is. So a step
 * that is no such multiple starts the fingers' scroll, and every step counts as theirs until they lift.
 */
internal class ScrollSteps {
    // When the fingers last moved, while their scroll runs
    private var fingersAt: Long? = null

    /** Forgets a scroll of the fingers that the display stopped reporting, as when the pointer left the window. */
    fun reset() {
        fingersAt = null
    }

    /** The meaning of [horizontal] and [vertical] steps of a [kind] of device at [at]; null when they mean nothing. */
    fun read(kind: ScrollDeviceKind, horizontal: Double, vertical: Double, at: ScrollPoint): ScrollMotion? {
        val still = horizontal == 0.0 && vertical == 0.0
        if (kind == ScrollDeviceKind.Wheel) {
            return if (still) null else ScrollMotion.Wheel(at, horizontal, vertical)
        }
        val running = fingersAt?.let { at.timeMillis - it <= FINGERS_KEPT_MS } ?: false
        if (still) {
            fingersAt = null
            return if (running) ScrollMotion.FingersLifted(at) else null
        }
        val wheel = kind == ScrollDeviceKind.Xwayland && !running && horizontal.isWheelStep() && vertical.isWheelStep()
        if (wheel) return ScrollMotion.Wheel(at, horizontal, vertical)
        fingersAt = at.timeMillis
        val stepPixels = if (kind == ScrollDeviceKind.Xwayland) XWAYLAND_STEP_PIXELS else LIBINPUT_STEP_PIXELS
        return ScrollMotion.Fingers(at, horizontal * stepPixels, vertical * stepPixels, starts = !running)
    }

    private fun Double.isWheelStep(): Boolean {
        val parts = this * WHEEL_STEP_PARTS
        return abs(parts - parts.roundToLong()) < WHEEL_STEP_TOLERANCE
    }
}

// Xwayland divides the compositor's scroll distance by this to make steps
private const val XWAYLAND_STEP_PIXELS = 10.0

// The libinput X driver's ScrollPixelDistance: a step for this much of the fingers' travel. A session that sets
// another distance has asked for touchpads to scroll that much slower or faster, which reading it as this one keeps.
private const val LIBINPUT_STEP_PIXELS = 15.0

private const val WHEEL_STEP_PARTS = 120
private const val WHEEL_STEP_TOLERANCE = 1e-6

// Fingers always lift with a report; one that got lost must not turn a wheel's later notches into finger steps
private const val FINGERS_KEPT_MS = 1_000L
