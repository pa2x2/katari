package app.katari.desktop.display.x11

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference

/**
 * The part of the system's Xlib (`Xlib.h`) that the app calls itself, for what AWT has no call for: telling the window
 * manager about the app's windows, and reading the touchpad pinches over them ([X11TouchpadPinches]). Each function is the C one without its `X` prefix: [openDisplay] is
 * `XOpenDisplay`. An X window or atom is a C `unsigned long`.
 */
internal interface Xlib : Library {
    /** A connection of its own to the display [name] names, or to `DISPLAY`'s for null; null when there is none. */
    fun openDisplay(name: String?): Pointer?

    fun closeDisplay(display: Pointer): Int

    fun defaultRootWindow(display: Pointer): NativeLong

    fun internAtom(display: Pointer, name: String, onlyIfExists: Boolean): NativeLong

    fun changeProperty(
        display: Pointer,
        window: NativeLong,
        property: NativeLong,
        type: NativeLong,
        format: Int,
        mode: Int,
        data: ByteArray,
        elements: Int,
    ): Int

    fun sendEvent(
        display: Pointer,
        window: NativeLong,
        propagate: Boolean,
        eventMask: NativeLong,
        event: XClientMessageEvent,
    ): Int

    fun flush(display: Pointer): Int

    /** Whether the display has the extension [name]; its major opcode goes into [opcode]. */
    fun queryExtension(
        display: Pointer,
        name: String,
        opcode: IntByReference,
        firstEvent: IntByReference,
        firstError: IntByReference,
    ): Boolean

    fun createSimpleWindow(
        display: Pointer,
        parent: NativeLong,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        borderWidth: Int,
        border: NativeLong,
        background: NativeLong,
    ): NativeLong

    fun destroyWindow(display: Pointer, window: NativeLong): Int

    /** Waits for the next event and copies it into [event], memory of [XEVENT_SIZE] bytes. */
    fun nextEvent(display: Pointer, event: Pointer): Int

    /** Fills the [cookie] of an extension's event with the event's data, which [freeEventData] lets go of. */
    fun getEventData(display: Pointer, cookie: XGenericEventCookie): Boolean

    fun freeEventData(display: Pointer, cookie: XGenericEventCookie)
}

internal const val PROP_MODE_REPLACE = 0

internal const val CLIENT_MESSAGE = 33

internal const val GENERIC_EVENT = 35

internal const val PROPERTY_CHANGE_MASK = 1L shl 22

/** The size of the `XEvent` union: 24 longs. */
internal val XEVENT_SIZE = 24L * NativeLong.SIZE

/**
 * `XClientMessageEvent`, padded to the size of the `XEvent` union, which `XSendEvent` reads whatever the event's type.
 * C lays [data] out as a union of 20 bytes, 10 shorts and 5 longs, aligned as the longs; [writeBytes] fills it as
 * bytes.
 */
@Structure.FieldOrder("type", "serial", "sendEvent", "display", "window", "messageType", "format", "data", "pad")
internal class XClientMessageEvent : Structure() {
    @JvmField var type: Int = 0

    @JvmField var serial: NativeLong = NativeLong(0)

    @JvmField var sendEvent: Int = 0

    @JvmField var display: Pointer? = null

    @JvmField var window: NativeLong = NativeLong(0)

    @JvmField var messageType: NativeLong = NativeLong(0)

    @JvmField var format: Int = 0

    @JvmField var data: Array<NativeLong> = Array(5) { NativeLong(0) }

    // XEvent is 24 longs; the fields above take 12 of them
    @JvmField var pad: Array<NativeLong> = Array(12) { NativeLong(0) }

    /**
     * Writes the fields to native memory with [bytes], at most 20, as the data of a message of format 8. A call then
     * passes the memory as it is, since writing the fields again before the call would replace the bytes.
     */
    fun writeBytes(bytes: ByteArray) {
        require(bytes.size <= MESSAGE_BYTES) { "A client message holds $MESSAGE_BYTES bytes, not ${bytes.size}" }
        format = 8
        autoWrite = false
        write()
        pointer.write(fieldOffset("data").toLong(), bytes, 0, bytes.size)
    }

    companion object {
        const val MESSAGE_BYTES = 20
    }
}

/**
 * `XGenericEventCookie`, which an extension's event, read as an `XEvent`, is: [extension] is the extension's major
 * opcode, [evtype] its own event type, and [data] the event itself once [Xlib.getEventData] has filled it.
 */
@Structure.FieldOrder("type", "serial", "sendEvent", "display", "extension", "evtype", "cookie", "data")
internal class XGenericEventCookie(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var serial: NativeLong = NativeLong(0)

    @JvmField var sendEvent: Int = 0

    @JvmField var display: Pointer? = null

    @JvmField var extension: Int = 0

    @JvmField var evtype: Int = 0

    @JvmField var cookie: Int = 0

    @JvmField var data: Pointer? = null

    init {
        read()
    }
}

/** The system's Xlib, or null where none loads, such as on a system without X11. */
internal val xlib: Xlib? by lazy {
    LIBRARY_NAMES.firstNotNullOfOrNull { name ->
        try {
            Native.load(name, Xlib::class.java, LIBRARY_OPTIONS)
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }
}

// The versioned name first: distributions ship the unversioned one only with Xlib's development files
private val LIBRARY_NAMES = listOf("libX11.so.6", "X11")

private val LIBRARY_OPTIONS = mapOf(
    Library.OPTION_FUNCTION_MAPPER to
        FunctionMapper { _, method -> "X" + method.name.replaceFirstChar(Char::uppercaseChar) },
)
