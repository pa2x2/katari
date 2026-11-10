package app.katari.desktop.display.x11

import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

/**
 * The display's devices that scroll, and how far each event moved their scroll axes. An axis reports where it is, not
 * how far it moved. It moves on while the pointer is over other clients' windows, so [refresh] reads where every axis
 * is afresh whenever the pointer comes back, as Qt does; and when another device takes the pointer over, the display
 * says where that one's axes are ([changed]), before its first event. Used by the thread that reads [display].
 */
internal class X11ScrollDevices(private val xlib: Xlib, private val xInput: XInput, private val display: Pointer) {
    private var devices: Map<Int, Device> = emptyMap()

    // Every device of the last look, scrolling or not
    private var known: Set<Int> = emptySet()

    fun refresh() {
        val count = IntByReference()
        val infos = xInput.queryDevice(display, XI_ALL_DEVICES, count) ?: return
        try {
            val tapping = xlib.internAtom(display, TAPPING_PROPERTY, true).toLong()
            val all = if (count.value == 0) {
                emptyList()
            } else {
                val infoSize = XIDeviceInfo(infos).size().toLong()
                (0 until count.value).map { XIDeviceInfo(infos.share(it * infoSize)) }
            }
            known = all.mapTo(HashSet()) { it.deviceid }
            devices = all.mapNotNull { info -> device(info, tapping)?.let { info.deviceid to it } }.toMap()
        } finally {
            xInput.freeDeviceInfo(infos)
        }
    }

    /**
     * Takes where the axes of the device that now moves the pointer are from [event], which holds them as they were
     * when it took over. Asking the display instead would answer with where they are by now, past the steps that are
     * still on their way here, which would then count for nothing.
     */
    fun changed(event: XIDeviceChangedEvent) {
        if (event.sourceid !in known) refresh()
        val device = devices[event.sourceid] ?: return
        val classes = event.classes?.getPointerArray(0, event.numClasses) ?: return
        val values = valuesByAxis(classes)
        for (axis in device.axes) values[axis.number]?.let { axis.value = it }
    }

    /**
     * How many steps the device [sourceId] scrolled by in an event that reports [valuators]; null when the device
     * doesn't scroll or the event reports none of its scroll axes, as a pointer only moving does.
     */
    fun steps(sourceId: Int, valuators: XIValuatorState): Steps? {
        if (sourceId !in known) {
            // A device that came unannounced scrolls from where it is found, so its first step is lost
            refresh()
            known = known + sourceId
        }
        val device = devices[sourceId] ?: return null
        if (device.axes.none { valuators.reports(it.number) }) return null
        val values = valuators.byAxis()
        var horizontal = 0.0
        var vertical = 0.0
        for (axis in device.axes) {
            val value = values[axis.number] ?: continue
            val steps = (value - axis.value) / axis.increment
            axis.value = value
            if (axis.sideways) horizontal += steps else vertical += steps
        }
        return Steps(device.kind, horizontal, vertical)
    }

    data class Steps(val kind: ScrollDeviceKind, val horizontal: Double, val vertical: Double)

    private class Device(val kind: ScrollDeviceKind, val axes: List<Axis>)

    private class Axis(val number: Int, val sideways: Boolean, val increment: Double, var value: Double)

    private fun device(info: XIDeviceInfo, tapping: Long): Device? {
        val classes = info.classes?.getPointerArray(0, info.numClasses) ?: return null
        val values = valuesByAxis(classes)
        val axes = classes
            .filter { xiClassType(it) == XI_SCROLL_CLASS }
            .map(::XIScrollClassInfo)
            .filter { it.increment != 0.0 }
            .mapNotNull { scroll ->
                val value = values[scroll.number] ?: return@mapNotNull null
                Axis(scroll.number, scroll.scrollType == XI_SCROLL_TYPE_HORIZONTAL, scroll.increment, value)
            }
        if (axes.isEmpty()) return null
        val kind = when {
            info.name?.startsWith(XWAYLAND_DEVICE_PREFIX) == true -> ScrollDeviceKind.Xwayland
            tapping != 0L && hasProperty(info.deviceid, tapping) -> ScrollDeviceKind.Touchpad
            else -> ScrollDeviceKind.Wheel
        }
        return Device(kind, axes)
    }

    private fun valuesByAxis(classes: Array<Pointer>): Map<Int, Double> = classes
        .filter { xiClassType(it) == XI_VALUATOR_CLASS }
        .map(::XIValuatorClassInfo)
        .associate { it.number to it.value }

    private fun hasProperty(deviceId: Int, property: Long): Boolean {
        val count = IntByReference()
        val atoms = xInput.listProperties(display, deviceId, count) ?: return false
        try {
            return (0 until count.value).any { atoms.getNativeLong(it.toLong() * NativeLong.SIZE).toLong() == property }
        } finally {
            xlib.free(atoms)
        }
    }

    private companion object {
        // Only touchpads tap, so libinput's X driver gives the property to them alone, as GTK tells them by
        const val TAPPING_PROPERTY = "libinput Tapping Enabled"

        const val XWAYLAND_DEVICE_PREFIX = "xwayland-"
    }
}
