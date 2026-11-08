package mihon.desktop.display.x11

import androidx.compose.ui.geometry.Offset
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import logcat.LogPriority
import mihon.presentation.kit.pointer.TouchpadPinch
import tachiyomi.core.common.util.system.logcat
import java.awt.Window
import kotlin.concurrent.thread

/**
 * X11: the touchpad pinches over a window, the gesture events of XInput 2.4, which X servers send for touchpads that
 * libinput drives and Xwayland forwards from the compositor. AWT's X11 toolkit never asks for them, so they come on a
 * connection of the app's own while the flow is collected, read on a thread of its own, as Xlib connections are used
 * by one thread at a time. A display takes a window's pinches from one client only, so collectors share one
 * collection.
 */
internal object X11TouchpadPinches {
    /** The pinches over [window], a shown window of AWT's X11 toolkit; null where Xlib or XInput 2 doesn't load. */
    fun over(window: Window): Flow<TouchpadPinch>? {
        val xlib = xlib ?: return null
        val xInput = xInput ?: return null
        return callbackFlow {
            val listener = PinchListener.open(xlib, xInput, window)
            if (listener == null) {
                close()
                return@callbackFlow
            }
            thread(name = "X11 touchpad pinches", isDaemon = true) { listener.read { trySend(it) } }
            awaitClose(listener::stop)
        }
            // Pinches come at the touchpad's pace; none is dropped, so every start meets its end
            .buffer(Channel.UNLIMITED)
            .flowOn(Dispatchers.IO)
    }

    /**
     * A connection of its own that has asked for the pinches over the window [windowId]. It also made [wakeWindow], an
     * unmapped window that no one else knows, for [stop] to send a message to, which ends the wait for the next event.
     */
    private class PinchListener(
        private val xlib: Xlib,
        private val display: Pointer,
        private val opcode: Int,
        private val wakeWindow: NativeLong,
    ) {
        /** Reads the pinches into [send] until [stop], then closes the connection. */
        fun read(send: (TouchpadPinch) -> Unit) {
            val event = Memory(XEVENT_SIZE)
            var panX = 0.0
            var panY = 0.0
            try {
                while (true) {
                    xlib.nextEvent(display, event)
                    when (event.getInt(0)) {
                        CLIENT_MESSAGE -> return
                        GENERIC_EVENT -> {
                            val cookie = XGenericEventCookie(event)
                            if (cookie.extension != opcode || !xlib.getEventData(display, cookie)) continue
                            try {
                                val pinch = XIGesturePinchEvent(cookie.data ?: continue)
                                when (cookie.evtype) {
                                    XI_GESTURE_PINCH_BEGIN -> {
                                        panX = 0.0
                                        panY = 0.0
                                        send(TouchpadPinch.Start)
                                    }
                                    XI_GESTURE_PINCH_UPDATE -> {
                                        panX += pinch.deltaX
                                        panY += pinch.deltaY
                                        val pan = Offset(panX.toFloat(), panY.toFloat())
                                        send(TouchpadPinch.Move(pinch.scale.toFloat(), pan))
                                    }
                                    XI_GESTURE_PINCH_END -> send(TouchpadPinch.End)
                                }
                            } finally {
                                xlib.freeEventData(display, cookie)
                            }
                        }
                    }
                }
            } finally {
                xlib.destroyWindow(display, wakeWindow)
                xlib.closeDisplay(display)
            }
        }

        // From a connection of its own: this one is the reading thread's
        fun stop() {
            val other = xlib.openDisplay(null) ?: return
            try {
                val message = XClientMessageEvent().apply {
                    type = CLIENT_MESSAGE
                    this.display = other
                    window = wakeWindow
                }
                message.writeBytes(ByteArray(0))
                // With no event mask the message goes to the window's creator, the reading connection
                xlib.sendEvent(other, wakeWindow, false, NativeLong(0), message)
                xlib.flush(other)
            } finally {
                xlib.closeDisplay(other)
            }
        }

        companion object {
            /**
             * Asks for the pinches over [window] on a new connection; null when the window has no X11 id or the
             * display has no XInput 2.4, the first version with gestures.
             */
            fun open(xlib: Xlib, xInput: XInput, window: Window): PinchListener? {
                val windowId = NativeLong(X11Windows.idOf(window) ?: return null)
                val display = xlib.openDisplay(null) ?: return null
                val opcode = IntByReference()
                val major = IntByReference(GESTURES_MAJOR)
                val minor = IntByReference(GESTURES_MINOR)
                val hasGestures = xlib.queryExtension(display, XINPUT, opcode, IntByReference(), IntByReference()) &&
                    xInput.queryVersion(display, major, minor) == 0 &&
                    major.value == GESTURES_MAJOR &&
                    minor.value >= GESTURES_MINOR
                if (!hasGestures) {
                    logcat(LogPriority.INFO) { "The display has no XInput 2.4, so no touchpad pinches" }
                    xlib.closeDisplay(display)
                    return null
                }
                xInput.selectEvents(display, windowId, XIEventMask(XI_ALL_MASTER_DEVICES, PINCH_MASK), 1)
                val wakeWindow = xlib.createSimpleWindow(
                    display,
                    xlib.defaultRootWindow(display),
                    0,
                    0,
                    1,
                    1,
                    0,
                    NativeLong(0),
                    NativeLong(0),
                )
                xlib.flush(display)
                return PinchListener(xlib, display, opcode.value, wakeWindow)
            }
        }
    }

    private const val XINPUT = "XInputExtension"
    private const val GESTURES_MAJOR = 2
    private const val GESTURES_MINOR = 4

    // A bit for each pinch event type, in as many bytes as all of XInput's types take (XIMaskLen)
    private val PINCH_MASK = ByteArray(XI_LAST_EVENT / 8 + 1).apply {
        for (type in XI_GESTURE_PINCH_BEGIN..XI_GESTURE_PINCH_END) {
            this[type / 8] = (this[type / 8].toInt() or (1 shl (type % 8))).toByte()
        }
    }
}
