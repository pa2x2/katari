package app.katari.webview.jcef.surface

import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pointer.TouchpadWheelEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import kotlin.math.floor

/**
 * Hands the mouse over the page to Chromium as the AWT events JCEF takes, at the page's CSS pixels: the three buttons
 * with the window system's click counts, so double and triple clicks select words and lines, moves and drags, which
 * keep going to the page outside it until the button is let go, entering and leaving, and the wheel. [onPress] runs as
 * a button goes down, and [onPointer] gets the pointer's position in pixels with every event.
 */
internal fun Modifier.pagePointerInput(
    page: PagePointerTarget,
    onPress: () -> Unit,
    onPointer: (Offset) -> Unit,
): Modifier = pointerInput(page) {
    val wheel = WheelPixels()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val awt = event.awtEventOrNull ?: continue
            val position = event.changes.firstOrNull()?.position ?: continue
            onPointer(position)
            val scale = page.scale
            val x = floor(position.x / scale).toInt()
            val y = floor(position.y / scale).toInt()
            val pressed = event.buttons.areAnyPressed
            val id = when (event.type) {
                PointerEventType.Press -> {
                    onPress()
                    MouseEvent.MOUSE_PRESSED.takeIf { awt.button in PAGE_BUTTONS }
                }
                PointerEventType.Release -> MouseEvent.MOUSE_RELEASED.takeIf { awt.button in PAGE_BUTTONS }
                PointerEventType.Move -> if (pressed) MouseEvent.MOUSE_DRAGGED else MouseEvent.MOUSE_MOVED
                PointerEventType.Enter -> MouseEvent.MOUSE_ENTERED
                // Leaving while a button is held would end a selection being dragged past the page's edge
                PointerEventType.Exit -> MouseEvent.MOUSE_EXITED.takeIf { !pressed }
                PointerEventType.Scroll -> {
                    (awt as? MouseWheelEvent)?.let { wheel.send(page, it, x, y) }
                    null
                }
                else -> null
            }
            if (id != null) {
                page.send(
                    MouseEvent(
                        awt.component,
                        id,
                        awt.`when`,
                        awt.modifiersEx,
                        x,
                        y,
                        awt.clickCount,
                        awt.isPopupTrigger,
                        if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED) awt.button else 0,
                    ),
                )
            }
            event.changes.forEach { it.consume() }
        }
    }
}

/**
 * The wheel in the pixels Chromium scrolls by, as JCEF passes a wheel event's steps on to it as pixels, scrolling up
 * for positive ones. A wheel step scrolls as far as Chromium scrolls for one on X11, and a touchpad's fingers by as
 * far as they travel, their small scrolls adding up until they make a pixel. Scrolling sideways, which the window system sends with Shift held, adds up
 * apart.
 */
private class WheelPixels {
    private var pixels = 0.0
    private var sideways = false

    fun send(page: PagePointerTarget, event: MouseWheelEvent, x: Int, y: Int) {
        if (event.isShiftDown != sideways) {
            sideways = event.isShiftDown
            pixels = 0.0
        }
        // The fingers' travel is in the window's pixels, [PagePointerTarget.scale] of them to one of the page's
        pixels -= (event as? TouchpadWheelEvent)?.let { it.pixels / page.scale }
            ?: (event.preciseWheelRotation * PIXELS_PER_STEP)
        val whole = pixels.toInt()
        if (whole == 0) return
        pixels -= whole
        page.send(
            MouseWheelEvent(
                event.component,
                MouseEvent.MOUSE_WHEEL,
                event.`when`,
                event.modifiersEx,
                x,
                y,
                event.xOnScreen,
                event.yOnScreen,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                whole,
                whole.toDouble(),
            ),
        )
    }

    private companion object {
        // Chromium's kWheelScrollAmount for X11
        const val PIXELS_PER_STEP = 53
    }
}

// JCEF passes the left, middle and right buttons on; the back and forward buttons are the window's
private val PAGE_BUTTONS = setOf(MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3)
