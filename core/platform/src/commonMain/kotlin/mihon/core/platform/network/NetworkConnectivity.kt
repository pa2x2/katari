package mihon.core.platform.network

import kotlinx.coroutines.flow.Flow

/** The network the device is on, as background work that needs one waits for it. */
interface NetworkConnectivity {
    /** The network now, then each change, for as long as it is collected. */
    fun state(): Flow<NetworkState>
}

/**
 * @property isOnline whether the network reaches the internet, not only a local network or a sign-in page.
 * @property isMetered whether the system says data on it may cost the user, so large transfers nobody waits for
 * should not start on it.
 * @property countsAsWifi whether work the user limited to Wi-Fi may use it: whether it is Wi-Fi where the platform
 * knows the network's transport, otherwise whether the system doesn't call it metered.
 */
data class NetworkState(val isOnline: Boolean, val isMetered: Boolean, val countsAsWifi: Boolean)
