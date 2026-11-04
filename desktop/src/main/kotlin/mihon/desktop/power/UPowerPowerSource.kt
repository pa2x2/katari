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

/**
 * Linux: the charger as UPower's `OnBattery` reports it, read again on each change of UPower's properties. A computer
 * with no battery is never on battery, so it counts as plugged in, and so does one without UPower, so work limited to
 * charging isn't held back by a system that can't tell. One monitor serves every collector at once.
 */
class UPowerPowerSource(scope: CoroutineScope) : PowerSource {
    private val pluggedIn = callbackFlow {
        val monitor = UPower.monitor()
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

    override fun isPluggedIn(): Flow<Boolean> = pluggedIn

    private fun read(): Boolean {
        // gdbus prints the property as `(<true>,)`
        val onBattery = UPower.call(
            "org.freedesktop.DBus.Properties.Get",
            INTERFACE,
            "OnBattery",
            timeoutSeconds = READ_TIMEOUT_SECONDS,
        )
        return onBattery?.contains("true") != true
    }

    private object UPower : Gdbus(
        bus = Bus.System,
        destination = INTERFACE,
        objectPath = "/org/freedesktop/UPower",
    )

    private companion object {
        const val INTERFACE = "org.freedesktop.UPower"
        const val CHANGED_SIGNAL = "org.freedesktop.DBus.Properties.PropertiesChanged"
        const val READ_TIMEOUT_SECONDS = 2
    }
}
