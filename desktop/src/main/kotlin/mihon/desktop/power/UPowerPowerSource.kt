package mihon.desktop.power

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mihon.core.platform.power.PowerSource
import mihon.desktop.dbus.Gdbus
import kotlin.math.roundToInt

/**
 * Linux: the charger as UPower's `OnBattery` reports it, and the charge as its display device's `Percentage` does, each
 * read again on each change of that object's properties. A computer with no battery is never on battery, so it counts
 * as plugged in, and so does one without UPower, so work limited to charging isn't held back by a system that can't
 * tell; it has no charge to show. One monitor per object serves every collector at once.
 */
class UPowerPowerSource(scope: CoroutineScope) : PowerSource {
    private val pluggedIn = UPower.watch(scope) {
        // gdbus prints the property as `(<true>,)`
        UPower.property(INTERFACE, "OnBattery")?.contains("true") != true
    }

    private val batteryPercent = DisplayDevice.watch(scope) {
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

        /** [read] now and again on each change of the object's properties, while anyone collects it. */
        fun <T> Gdbus.watch(scope: CoroutineScope, read: () -> T): Flow<T> = callbackFlow {
            val monitor = monitor()
            send(read())
            if (monitor != null) {
                launch(Dispatchers.IO) {
                    monitor.signals.filter { CHANGED_SIGNAL in it }.forEach { send(read()) }
                }
            }
            awaitClose { monitor?.stop() }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            // Started for the first collector and stopped after the last, so a new one never gets a stale state.
            .shareIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)
    }
}
