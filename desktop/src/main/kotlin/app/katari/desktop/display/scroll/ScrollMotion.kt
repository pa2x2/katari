package app.katari.desktop.display.scroll

import app.katari.desktop.display.scroll.natural.DisplayTouchpads

/**
 * A step of scrolling over a window as the display itself reports it, finer than the whole wheel notches the window
 * toolkit makes of it, and telling a touchpad's fingers from a wheel.
 */
internal sealed interface ScrollMotion {
    val at: ScrollPoint

    /** A wheel turned by [horizontal] and [vertical] notches, down and right positive; a smooth wheel turns fractions. */
    data class Wheel(override val at: ScrollPoint, val horizontal: Double, val vertical: Double) : ScrollMotion

    /**
     * A touchpad's two fingers scrolled by [horizontal] and [vertical], down and right positive, in the pixels libinput
     * reports a touchpad's scrolling in, which the session's own windows scroll by several times over. That is the way
     * the fingers went, or the opposite way where the system scrolls naturally ([DisplayTouchpads]). [starts] is set
     * for the first step after the fingers came down.
     */
    data class Fingers(
        override val at: ScrollPoint,
        val horizontal: Double,
        val vertical: Double,
        val starts: Boolean,
    ) : ScrollMotion

    data class FingersLifted(override val at: ScrollPoint) : ScrollMotion
}

/**
 * When and where a [ScrollMotion] happened: [timeMillis] on the display's own clock, the pointer in the screen's
 * pixels, and the keys held as AWT's extended modifiers.
 */
internal data class ScrollPoint(val timeMillis: Long, val xOnScreen: Double, val yOnScreen: Double, val modifiers: Int)
