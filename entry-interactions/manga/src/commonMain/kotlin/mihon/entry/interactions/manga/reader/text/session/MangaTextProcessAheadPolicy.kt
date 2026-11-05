package mihon.entry.interactions.manga.reader.text.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.power.PowerSource

/**
 * Whether pages the reader has not reached yet may be processed now. Pages on screen are always processed; work
 * ahead waits for an unmetered network or a charger when the user asked for that.
 */
internal class MangaTextProcessAheadPolicy(
    private val connectivity: NetworkConnectivity,
    power: PowerSource,
    scope: CoroutineScope,
) {
    // Off until the platform first answers, so work ahead waits rather than starting on battery
    private val pluggedIn = power.isPluggedIn().stateIn(scope, SharingStarted.Eagerly, false)

    fun allows(onlyOnUnmeteredNetwork: Boolean, onlyWhileCharging: Boolean): Boolean {
        if (onlyOnUnmeteredNetwork) {
            val network = connectivity.current()
            if (!network.hasNetwork || network.isMetered) return false
        }
        if (onlyWhileCharging && !pluggedIn.value) return false
        return true
    }
}
