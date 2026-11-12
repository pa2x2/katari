package app.katari.desktop.display.x11

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.DoubleByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * The part of the system's XInput 2 library (`libXi`, `XInput2.h`) that the app calls itself, for the input AWT never
 * asks for: the touchpad gestures of XInput 2.4, and the scroll axes and raw button presses of XInput 2.1, which carry
 * scrolling finer than a wheel's notch and tell a wheel's own clicks from the ones the display makes up for it. Each
 * function is the C one without its `XI` prefix: [queryVersion] is `XIQueryVersion`.
 */
internal interface XInput : Library {
    /**
     * Tells the display the version of XInput 2 this client speaks, [major].[minor], which the display lowers to its
     * own where that is older; returns 0 on success.
     */
    fun queryVersion(display: Pointer, major: IntByReference, minor: IntByReference): Int

    fun selectEvents(display: Pointer, window: NativeLong, masks: XIEventMask, count: Int): Int

    /**
     * The devices [deviceid] names, as an array of [count] `XIDeviceInfo` that [freeDeviceInfo] lets go of; null when
     * the display knows no such device.
     */
    fun queryDevice(display: Pointer, deviceid: Int, count: IntByReference): Pointer?

    fun freeDeviceInfo(info: Pointer)

    /**
     * The atoms of the properties the device has, [count] of them as C `unsigned long`s that [Xlib.free] lets go of;
     * null when it has none.
     */
    fun listProperties(display: Pointer, deviceid: Int, count: IntByReference): Pointer?

    /**
     * Reads up to [length] 32-bit units of the device's [property] from [offset], of any type when [type] is 0: its
     * [typeReturn], [formatReturn] (8, 16 or 32 bits an item), [itemCount] items and [bytesAfter] left unread. The
     * items in [data] are the caller's to [Xlib.free]; returns 0 on success, where a device without the property
     * answers with no type and no data.
     */
    fun getProperty(
        display: Pointer,
        deviceid: Int,
        property: NativeLong,
        offset: NativeLong,
        length: NativeLong,
        deleteProperty: Boolean,
        type: NativeLong,
        typeReturn: NativeLongByReference,
        formatReturn: IntByReference,
        itemCount: NativeLongByReference,
        bytesAfter: NativeLongByReference,
        data: PointerByReference,
    ): Int

    /**
     * Where the pointer [deviceid] is and what is held, with [rootX] and [rootY] in the screen of [root]; false when
     * it is on another screen than [window]'s. The mask [buttons] comes back with is the caller's to [Xlib.free].
     */
    fun queryPointer(
        display: Pointer,
        deviceid: Int,
        window: NativeLong,
        root: NativeLongByReference,
        child: NativeLongByReference,
        rootX: DoubleByReference,
        rootY: DoubleByReference,
        windowX: DoubleByReference,
        windowY: DoubleByReference,
        buttons: XIButtonState,
        mods: XIModifierState,
        group: XIModifierState,
    ): Boolean
}

/** The extension's name, by which a display says whether it has it. */
internal const val XINPUT_EXTENSION = "XInputExtension"

internal const val XI_ALL_DEVICES = 0

internal const val XI_ALL_MASTER_DEVICES = 1

internal const val XI_DEVICE_CHANGED = 1
internal const val XI_MOTION = 6
internal const val XI_ENTER = 7
internal const val XI_LEAVE = 8
internal const val XI_RAW_BUTTON_PRESS = 15
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

/** An event mask with a bit for each of [types], in as many bytes as all of XInput's types take (`XIMaskLen`). */
internal fun xiEventMask(deviceid: Int, types: Iterable<Int>): XIEventMask {
    val mask = ByteArray(XI_LAST_EVENT / Byte.SIZE_BITS + 1)
    for (type in types) mask.setBit(type)
    return XIEventMask(deviceid, mask)
}

private fun ByteArray.setBit(bit: Int) {
    val index = bit / Byte.SIZE_BITS
    this[index] = (this[index].toInt() or (1 shl (bit % Byte.SIZE_BITS))).toByte()
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

/**
 * The flag of an event the display made up from another (`XIPointerEmulated`): a wheel button's press for a step of
 * a scroll axis, which it sends beside the axis's own motion for clients that only know buttons.
 */
internal const val XI_POINTER_EMULATED = 1 shl 16

/**
 * `XIRawEvent` up to [flags], the fields read. A raw event is a device's own, before any window is picked for it, and
 * comes to every client that asked for it on the root window, wherever the pointer is: [detail] is the button of a
 * press.
 */
@Structure.FieldOrder(
    "type", "serial", "sendEvent", "display", "extension", "evtype", "time", "deviceid", "sourceid", "detail", "flags",
)
internal class XIRawEvent(memory: Pointer) : Structure(memory) {
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

    @JvmField var flags: Int = 0

    init {
        read()
    }
}

/** `XIButtonState`: the pressed buttons as a mask of [maskLen] bytes, a bit for each button's number. */
@Structure.FieldOrder("maskLen", "mask")
internal class XIButtonState : Structure() {
    @JvmField var maskLen: Int = 0

    @JvmField var mask: Pointer? = null

    fun isPressed(button: Int): Boolean {
        val mask = mask ?: return false
        if (button >= maskLen * Byte.SIZE_BITS) return false
        return mask.getByte((button / Byte.SIZE_BITS).toLong()).toInt() and (1 shl (button % Byte.SIZE_BITS)) != 0
    }
}

/**
 * `XIValuatorState`: the axes an event reports, a bit each in the [maskLen] bytes of [mask], and their [values], one
 * double for each set bit in the bits' order.
 */
@Structure.FieldOrder("maskLen", "mask", "values")
internal class XIValuatorState : Structure() {
    @JvmField var maskLen: Int = 0

    @JvmField var mask: Pointer? = null

    @JvmField var values: Pointer? = null

    fun reports(axis: Int): Boolean {
        val mask = mask ?: return false
        if (axis >= maskLen * Byte.SIZE_BITS) return false
        return mask.getByte((axis / Byte.SIZE_BITS).toLong()).toInt() and (1 shl (axis % Byte.SIZE_BITS)) != 0
    }

    /** The value of each axis the event reports, by the axis's number. */
    fun byAxis(): Map<Int, Double> {
        val values = values ?: return emptyMap()
        val byAxis = HashMap<Int, Double>()
        for (axis in 0 until maskLen * Byte.SIZE_BITS) {
            if (reports(axis)) byAxis[axis] = values.getDouble(byAxis.size.toLong() * Double.SIZE_BYTES)
        }
        return byAxis
    }
}

/** `XIModifierState`, and `XIGroupState`, which has the same fields: [effective] is the modifiers held, as X11 masks. */
@Structure.FieldOrder("base", "latched", "locked", "effective")
internal class XIModifierState : Structure() {
    @JvmField var base: Int = 0

    @JvmField var latched: Int = 0

    @JvmField var locked: Int = 0

    @JvmField var effective: Int = 0
}

/**
 * `XIDeviceEvent`, which a pointer's motion comes as, and the start of `XIEnterEvent`, which has the same fields up to
 * [eventY]. Every motion over a window comes, a mouse's many times a frame, and only the few that scroll are of use,
 * so nothing is read until asked for: [readValuators] for the axes, [read] for the rest.
 */
@Structure.FieldOrder(
    "type", "serial", "sendEvent", "display", "extension", "evtype", "time", "deviceid", "sourceid", "detail", "root",
    "event", "child", "rootX", "rootY", "eventX", "eventY", "flags", "buttons", "valuators", "mods", "group",
)
internal class XIDeviceEvent(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var serial: NativeLong = NativeLong(0)

    @JvmField var sendEvent: Int = 0

    @JvmField var display: Pointer? = null

    @JvmField var extension: Int = 0

    @JvmField var evtype: Int = 0

    @JvmField var time: NativeLong = NativeLong(0)

    @JvmField var deviceid: Int = 0

    /** The device that made the event, where [deviceid] is the pointer it moves. */
    @JvmField var sourceid: Int = 0

    @JvmField var detail: Int = 0

    @JvmField var root: NativeLong = NativeLong(0)

    @JvmField var event: NativeLong = NativeLong(0)

    @JvmField var child: NativeLong = NativeLong(0)

    @JvmField var rootX: Double = 0.0

    @JvmField var rootY: Double = 0.0

    @JvmField var eventX: Double = 0.0

    @JvmField var eventY: Double = 0.0

    @JvmField var flags: Int = 0

    @JvmField var buttons: XIButtonState = XIButtonState()

    @JvmField var valuators: XIValuatorState = XIValuatorState()

    @JvmField var mods: XIModifierState = XIModifierState()

    @JvmField var group: XIModifierState = XIModifierState()

    fun readValuators(): XIValuatorState {
        readField("valuators")
        return valuators
    }

    fun readSource(): Int = readField("sourceid") as Int

    fun readDetail(): Int = readField("detail") as Int
}

/**
 * `XIDeviceChangedEvent`: another device, [sourceid], now moves the pointer [deviceid], or the device's own axes
 * changed. [classes] are what the pointer has from then on, as in [XIDeviceInfo], with every axis where it is at that
 * moment.
 */
@Structure.FieldOrder(
    "type", "serial", "sendEvent", "display", "extension", "evtype", "time", "deviceid", "sourceid", "reason",
    "numClasses", "classes",
)
internal class XIDeviceChangedEvent(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var serial: NativeLong = NativeLong(0)

    @JvmField var sendEvent: Int = 0

    @JvmField var display: Pointer? = null

    @JvmField var extension: Int = 0

    @JvmField var evtype: Int = 0

    @JvmField var time: NativeLong = NativeLong(0)

    @JvmField var deviceid: Int = 0

    @JvmField var sourceid: Int = 0

    @JvmField var reason: Int = 0

    @JvmField var numClasses: Int = 0

    @JvmField var classes: Pointer? = null

    init {
        read()
    }
}

/** `XIDeviceInfo`: a device, and what it has as [numClasses] pointers to `XIAnyClassInfo` at [classes]. */
@Structure.FieldOrder("deviceid", "name", "use", "attachment", "enabled", "numClasses", "classes")
internal class XIDeviceInfo(memory: Pointer) : Structure(memory) {
    @JvmField var deviceid: Int = 0

    @JvmField var name: String? = null

    @JvmField var use: Int = 0

    @JvmField var attachment: Int = 0

    @JvmField var enabled: Int = 0

    @JvmField var numClasses: Int = 0

    @JvmField var classes: Pointer? = null

    init {
        read()
    }
}

/** The `type` every class of a device starts with (`XIAnyClassInfo`), which says which of the classes below it is. */
internal fun xiClassType(classInfo: Pointer): Int = classInfo.getInt(0)

internal const val XI_VALUATOR_CLASS = 2
internal const val XI_SCROLL_CLASS = 3

/** `XIValuatorClassInfo`: the axis [number] of a device and the [value] it is at. */
@Structure.FieldOrder("type", "sourceid", "number", "label", "min", "max", "value", "resolution", "mode")
internal class XIValuatorClassInfo(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var sourceid: Int = 0

    @JvmField var number: Int = 0

    @JvmField var label: NativeLong = NativeLong(0)

    @JvmField var min: Double = 0.0

    @JvmField var max: Double = 0.0

    @JvmField var value: Double = 0.0

    @JvmField var resolution: Int = 0

    @JvmField var mode: Int = 0

    init {
        read()
    }
}

/**
 * `XIScrollClassInfo`: the axis [number] of a device scrolls, up and down or sideways by its [scrollType], and its
 * value moving by [increment] is one step of scrolling, as far as a wheel's notch.
 */
@Structure.FieldOrder("type", "sourceid", "number", "scrollType", "increment", "flags")
internal class XIScrollClassInfo(memory: Pointer) : Structure(memory) {
    @JvmField var type: Int = 0

    @JvmField var sourceid: Int = 0

    @JvmField var number: Int = 0

    @JvmField var scrollType: Int = 0

    @JvmField var increment: Double = 0.0

    @JvmField var flags: Int = 0

    init {
        read()
    }
}

internal const val XI_SCROLL_TYPE_HORIZONTAL = 2

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
