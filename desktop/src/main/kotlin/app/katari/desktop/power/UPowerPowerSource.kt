package app.katari.desktop.power

import app.katari.core.platform.power.PowerSource
import app.katari.desktop.dbus.Gdbus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * Linux: the charger as UPower's `OnBattery` reports it, and the charge as its display device's `Percentage` does, each
 * read again on each change of that object's properties. A computer with no battery is never on battery, so it counts
 * as plugged in, and so does one without UPower, so work limited to charging isn't held back by a system that can't
 * tell; it has no charge to show.
 */
class UPowerPowerSource(scope: CoroutineScope) : PowerSource {
    private val pluggedIn = UPower.watch(scope, CHANGED_SIGNAL) {
        // gdbus prints the property as `(<true>,)`
        UPower.property(INTERFACE, "OnBattery")?.contains("true") != true
    }

    private val batteryPercent = DisplayDevice.watch(scope, CHANGED_SIGNAL) {
        // gdbus prints the properties as `(<true>,)` and `(<85.0>,)`
        val present = DisplayDevice.property(DEVICE_INTERFACE, "IsPresent")?.contains("true") == true
        DisplayDevice.property(DEVICE_INTERFACE, "Percentage")
            ?.takeIf { present }
            ?.let { NUMBER.find(it)?.value?.toDoubleOrNull() }
            ?.roundToInt()
            ?.coerceIn(0, PERCENT)
    }

    override fun isPluggedIn(): Flow<Boolean> = pluggedIn

    override fun batteryPercent(): Flow<Int?> = batteryPercent

    private object UPower : Gdbus(
        bus = Bus.System,
        destination = INTERFACE,
        objectPath = "/org/freedesktop/UPower",
    )

    /** UPower's summary of the batteries the computer runs on. */
    private object DisplayDevice : Gdbus(
        bus = Bus.System,
        destination = INTERFACE,
        objectPath = "/org/freedesktop/UPower/devices/DisplayDevice",
    )

    private companion object {
        const val INTERFACE = "org.freedesktop.UPower"
        const val DEVICE_INTERFACE = "org.freedesktop.UPower.Device"
        const val CHANGED_SIGNAL = "org.freedesktop.DBus.Properties.PropertiesChanged"
        const val READ_TIMEOUT_SECONDS = 2
        const val PERCENT = 100
        val NUMBER = Regex("""\d+(\.\d+)?""")

        fun Gdbus.property(interfaceName: String, name: String): String? = call(
            "org.freedesktop.DBus.Properties.Get",
            interfaceName,
            name,
            timeoutSeconds = READ_TIMEOUT_SECONDS,
        )
    }
}
