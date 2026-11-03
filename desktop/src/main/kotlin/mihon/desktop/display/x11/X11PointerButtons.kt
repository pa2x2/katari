package mihon.desktop.display.x11

import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

/**
 * X11 reports scrolling as button presses: 4 and 5 for the vertical wheel, 6 and 7 for horizontal scrolling, which a
 * touchpad's sideways swipe and a tilting wheel make; a mouse's back and forward buttons are 8 and 9. The X11 toolkit
 * turns only 4 and 5 into wheel events and numbers the buttons after them from 4 up, so horizontal scrolling reaches
 * the windows as presses of buttons 4 and 5, which Compose takes for back and forward, and back and forward as 6 and 7,
 * which Compose ignores.
 *
 * The queue installed here renumbers them before any window sees them, to what Compose expects from every toolkit:
 * horizontal scrolling becomes a wheel event with Shift held, which Compose scrolls sideways, and back and forward
 * become buttons 4 and 5.
 */
internal object X11PointerButtons {
    /** Starts the toolkit, so it runs only once the window toolkit is chosen. */
    fun install() {
        if (isX11Toolkit()) Toolkit.getDefaultToolkit().systemEventQueue.push(RenumberingQueue())
    }

    private class RenumberingQueue : EventQueue() {
        override fun dispatchEvent(event: AWTEvent) {
            // Subclasses, such as wheel and drag-and-drop events, carry no X11 button numbers to correct
            if (event.javaClass != MouseEvent::class.java) return super.dispatchEvent(event)
            renumbered(event as MouseEvent)?.let { super.dispatchEvent(it) }
        }
    }

    /** [event] as Compose expects it, or null for the release and click that end a horizontal scroll step. */
    private fun renumbered(event: MouseEvent): MouseEvent? {
        val button = event.button
        if (button == SCROLL_LEFT || button == SCROLL_RIGHT) {
            return if (event.id == MouseEvent.MOUSE_PRESSED) horizontalScroll(event, button) else null
        }
        val modifiers = renumberedModifiers(event.modifiersEx)
        if (button != BACK && button != FORWARD && modifiers == event.modifiersEx) return event
        return MouseEvent(
            event.component,
            event.id,
            event.`when`,
            modifiers,
            event.x,
            event.y,
            event.xOnScreen,
            event.yOnScreen,
            event.clickCount,
            event.isPopupTrigger,
            when (button) {
                BACK -> COMPOSE_BACK
                FORWARD -> COMPOSE_FORWARD
                else -> button
            },
        )
    }

    private fun horizontalScroll(press: MouseEvent, button: Int): MouseWheelEvent = MouseWheelEvent(
        press.component,
        MouseEvent.MOUSE_WHEEL,
        press.`when`,
        renumberedModifiers(press.modifiersEx) or InputEvent.SHIFT_DOWN_MASK,
        press.x,
        press.y,
        press.xOnScreen,
        press.yOnScreen,
        0,
        false,
        MouseWheelEvent.WHEEL_UNIT_SCROLL,
        WHEEL_SCROLL_AMOUNT,
        // Toward the start, as the vertical wheel's up (X11's 4) is
        if (button == SCROLL_LEFT) -1 else 1,
    )

    private fun renumberedModifiers(modifiers: Int): Int {
        var renumbered = modifiers and (SCROLL_MASKS or BACK_FORWARD_MASKS).inv()
        if (modifiers and InputEvent.getMaskForButton(BACK) != 0) {
            renumbered = renumbered or InputEvent.getMaskForButton(COMPOSE_BACK)
        }
        if (modifiers and InputEvent.getMaskForButton(FORWARD) != 0) {
            renumbered = renumbered or InputEvent.getMaskForButton(COMPOSE_FORWARD)
        }
        return renumbered
    }

    // The X11 toolkit's numbers for X11's buttons 6 to 9
    private const val SCROLL_LEFT = 4
    private const val SCROLL_RIGHT = 5
    private const val BACK = 6
    private const val FORWARD = 7

    private const val COMPOSE_BACK = 4
    private const val COMPOSE_FORWARD = 5

    // What the X11 toolkit gives its own wheel events
    private const val WHEEL_SCROLL_AMOUNT = 3

    private val SCROLL_MASKS = InputEvent.getMaskForButton(SCROLL_LEFT) or InputEvent.getMaskForButton(SCROLL_RIGHT)
    private val BACK_FORWARD_MASKS = InputEvent.getMaskForButton(BACK) or InputEvent.getMaskForButton(FORWARD)
}
