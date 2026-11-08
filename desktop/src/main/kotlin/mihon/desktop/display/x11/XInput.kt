package mihon.desktop.display.x11

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference

/**
 * The part of the system's XInput 2 library (`libXi`, `XInput2.h`) that the app calls itself: asking for the touchpad
 * gestures of XInput 2.4, which AWT never asks for. Each function is the C one without its `XI` prefix: [queryVersion]
 * is `XIQueryVersion`.
 */
internal interface XInput : Library {
    /**
     * Tells the display the version of XInput 2 this client speaks, [major].[minor], which the display lowers to its
     * own where that is older; returns 0 on success.
     */
    fun queryVersion(display: Pointer, major: IntByReference, minor: IntByReference): Int

    fun selectEvents(display: Pointer, window: NativeLong, masks: XIEventMask, count: Int): Int
}

internal const val XI_ALL_MASTER_DEVICES = 1

internal const val XI_GESTURE_PINCH_BEGIN = 27
internal const val XI_GESTURE_PINCH_UPDATE = 28
internal const val XI_GESTURE_PINCH_END = 29

/** The last event type XInput 2.4 has (`XI_LASTEVENT`), which sizes an event mask. */
internal const val XI_LAST_EVENT = 32

/** `XIEventMask`: the event types [mask] has a bit set for, of the devices [deviceid] names. */
@Structure.FieldOrder("deviceid", "maskLen", "mask")
internal class XIEventMask(@JvmField var deviceid: Int, mask: ByteArray) : Structure() {
    @JvmField var maskLen: Int = mask.size

    @JvmField var mask: Pointer = Memory(mask.size.toLong()).apply { write(0, mask, 0, mask.size) }
}

/**
 * `XIGesturePinchEvent` up to [scale], the fields read: [deltaX] and [deltaY] are how far the fingers' middle moved
 * since the last event, and [scale] is their span over the span at the pinch's begin.
 */
@Structure.FieldOrder(
    "type", "serial", "sendEvent", "display", "extension", "evtype", "time", "deviceid", "sourceid", "detail", "root",
    "event", "child", "rootX", "rootY", "eventX", "eventY", "deltaX", "deltaY", "deltaUnacceleratedX",
    "deltaUnacceleratedY", "scale",
)
internal class XIGesturePinchEvent(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var serial: NativeLong = NativeLong(0)

    @JvmField var sendEvent: Int = 0

    @JvmField var display: Pointer? = null

    @JvmField var extension: Int = 0

    @JvmField var evtype: Int = 0

    @JvmField var time: NativeLong = NativeLong(0)

    @JvmField var deviceid: Int = 0

    @JvmField var sourceid: Int = 0

    @JvmField var detail: Int = 0

    @JvmField var root: NativeLong = NativeLong(0)

    @JvmField var event: NativeLong = NativeLong(0)

    @JvmField var child: NativeLong = NativeLong(0)

    @JvmField var rootX: Double = 0.0

    @JvmField var rootY: Double = 0.0

    @JvmField var eventX: Double = 0.0

    @JvmField var eventY: Double = 0.0

    @JvmField var deltaX: Double = 0.0

    @JvmField var deltaY: Double = 0.0

    @JvmField var deltaUnacceleratedX: Double = 0.0

    @JvmField var deltaUnacceleratedY: Double = 0.0

    @JvmField var scale: Double = 0.0

    init {
        read()
    }
}

/** The system's XInput 2 library, or null where none loads. */
internal val xInput: XInput? by lazy {
    LIBRARY_NAMES.firstNotNullOfOrNull { name ->
        try {
            Native.load(name, XInput::class.java, LIBRARY_OPTIONS)
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }
}

// The versioned name first: distributions ship the unversioned one only with the library's development files
private val LIBRARY_NAMES = listOf("libXi.so.6", "Xi")

private val LIBRARY_OPTIONS = mapOf(
    Library.OPTION_FUNCTION_MAPPER to
        FunctionMapper { _, method -> "XI" + method.name.replaceFirstChar(Char::uppercaseChar) },
)
