package app.katari.desktop.display.x11

import app.katari.desktop.display.scroll.ScrollMotion
import app.katari.desktop.display.scroll.ScrollPoint
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.awt.Window
import java.awt.event.InputEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * X11: the scrolling over a window as the scroll axes of XInput 2.1 report it, in fractions of a wheel's notch, which
 * X servers send for touchpads that libinput drives and Xwayland forwards from the compositor. AWT's X11 toolkit never
 * asks for them: it takes the wheel button press the server makes up for each whole notch the axes pass, which is all
 * it knows of a touchpad's scroll. They come on a connection of the app's own, read on a thread of its own, as Xlib
 * connections are used by one thread at a time.
 */
internal object X11ScrollMotions {
    private val read: MutableSet<Window> = ConcurrentHashMap.newKeySet()

    /**
     * Whether the scrolling over [window] is being read here right now, so the toolkit's wheel events for it say
     * again what a [ScrollMotion] says already. It isn't while the pointer is elsewhere, or while another connection
     * has grabbed the pointer, as AWT's does for as long as a mouse button is held, since the display then reports
     * the pointer to that connection alone.
     */
    fun reads(window: Window): Boolean = window in read

    /**
     * Reads the scrolling over [window], a window of AWT's X11 toolkit with its X11 window made, into [onMotion],
     * which the reading thread calls, until closed. [pointerOver] says whether the pointer is over the window now,
     * which the display only reports as it changes. Null where Xlib or XInput 2 doesn't load or the display has no
     * scroll axes.
     */
    fun over(window: Window, pointerOver: Boolean, onMotion: (ScrollMotion) -> Unit): AutoCloseable? {
        val xlib = xlib ?: return null
        val xInput = xInput ?: return null
        val listener = ScrollListener.open(xlib, xInput, window) ?: return null
        if (pointerOver) read += window
        thread(name = "X11 scroll motions", isDaemon = true) {
            try {
                listener.read(onMotion)
            } catch (e: RuntimeException) {
                logcat(LogPriority.ERROR, e) { "Reading the display's scrolling failed; the toolkit's wheel is back" }
            } finally {
                read -= window
            }
        }
        return AutoCloseable(listener::stop)
    }

    private class ScrollListener(
        private val xlib: Xlib,
        private val display: Pointer,
        private val opcode: Int,
        private val window: Window,
        private val devices: X11ScrollDevices,
        private val wakeWindow: X11WakeWindow,
    ) {
        private val steps = ScrollSteps()

        /** Reads the scrolling into [send] until [stop], then closes the connection. */
        fun read(send: (ScrollMotion) -> Unit) {
            val event = Memory(XEVENT_SIZE)
            try {
                while (true) {
                    xlib.nextEvent(display, event)
                    when (event.getInt(0)) {
                        CLIENT_MESSAGE -> return
                        GENERIC_EVENT -> {
                            val cookie = XGenericEventCookie(event)
                            if (cookie.extension != opcode || !xlib.getEventData(display, cookie)) continue
                            try {
                                val data = cookie.data ?: continue
                                when (cookie.evtype) {
                                    XI_MOTION -> motion(XIDeviceEvent(data))?.let(send)
                                    XI_ENTER -> {
                                        devices.refresh()
                                        read += window
                                    }
                                    // Onto a window inside this one is still over it
                                    XI_LEAVE -> if (XIDeviceEvent(data).readDetail() != NOTIFY_INFERIOR) {
                                        read -= window
                                        steps.reset()
                                    }
                                    XI_DEVICE_CHANGED -> devices.changed(XIDeviceChangedEvent(data))
                                }
                            } finally {
                                xlib.freeEventData(display, cookie)
                            }
                        }
                    }
                }
            } finally {
                wakeWindow.destroy()
                xlib.closeDisplay(display)
            }
        }

        fun stop() = wakeWindow.wake()

        private fun motion(event: XIDeviceEvent): ScrollMotion? {
            // A motion means the pointer is here, whatever came before the window's events were asked for
            read += window
            val scrolled = devices.steps(event.readSource(), event.readValuators()) ?: return null
            event.read()
            val at = ScrollPoint(event.time.toLong(), event.rootX, event.rootY, awtModifiers(event.mods.effective))
            return steps.read(scrolled.kind, scrolled.horizontal, scrolled.vertical, at)
        }

        companion object {
            /**
             * Asks for the pointer's motions over [window] on a new connection; null when the window has no X11 id or
             * the display has no XInput 2.1, the first version with scroll axes.
             */
            fun open(xlib: Xlib, xInput: XInput, window: Window): ScrollListener? {
                val windowId = NativeLong(X11Windows.idOf(window) ?: return null)
                val display = xlib.openDisplay(null) ?: return null
                val opcode = IntByReference()
                val major = IntByReference(SCROLL_AXES_MAJOR)
                val minor = IntByReference(SCROLL_AXES_MINOR)
                val hasScrollAxes =
                    xlib.queryExtension(display, XINPUT_EXTENSION, opcode, IntByReference(), IntByReference()) &&
                        xInput.queryVersion(display, major, minor) == 0 &&
                        major.value == SCROLL_AXES_MAJOR &&
                        minor.value >= SCROLL_AXES_MINOR
                if (!hasScrollAxes) {
                    logcat(LogPriority.INFO) { "The display has no XInput 2.1, so the wheel scrolls in whole notches" }
                    xlib.closeDisplay(display)
                    return null
                }
                val motions =
                    xiEventMask(XI_ALL_MASTER_DEVICES, listOf(XI_MOTION, XI_ENTER, XI_LEAVE, XI_DEVICE_CHANGED))
                xInput.selectEvents(display, windowId, motions, 1)
                val devices = X11ScrollDevices(xlib, xInput, display).apply { refresh() }
                val wakeWindow = X11WakeWindow(xlib, display)
                xlib.flush(display)
                return ScrollListener(xlib, display, opcode.value, window, devices, wakeWindow)
            }
        }
    }

    private fun awtModifiers(x11Modifiers: Int): Int {
        var modifiers = 0
        if (x11Modifiers and SHIFT_MASK != 0) modifiers = modifiers or InputEvent.SHIFT_DOWN_MASK
        if (x11Modifiers and CONTROL_MASK != 0) modifiers = modifiers or InputEvent.CTRL_DOWN_MASK
        if (x11Modifiers and ALT_MASK != 0) modifiers = modifiers or InputEvent.ALT_DOWN_MASK
        if (x11Modifiers and META_MASK != 0) modifiers = modifiers or InputEvent.META_DOWN_MASK
        return modifiers
    }

    private const val SCROLL_AXES_MAJOR = 2
    private const val SCROLL_AXES_MINOR = 1

    /** The detail of a leave onto a window inside the one left (`XINotifyInferior`). */
    private const val NOTIFY_INFERIOR = 2

    // X11's ShiftMask, ControlMask, Mod1Mask and Mod4Mask
    private const val SHIFT_MASK = 1 shl 0
    private const val CONTROL_MASK = 1 shl 2
    private const val ALT_MASK = 1 shl 3
    private const val META_MASK = 1 shl 6
}
