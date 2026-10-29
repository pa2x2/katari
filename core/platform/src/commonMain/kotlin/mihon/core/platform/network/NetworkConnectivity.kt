package mihon.core.platform.network

import kotlinx.coroutines.flow.Flow

/** The network the device is on, as background work that needs one waits for it. */
interface NetworkConnectivity {
    /** The network now, then each change, for as long as it is collected. */
    fun state(): Flow<NetworkState>
}

/**
 * @property isOnline whether the network reaches the internet, not only a local network or a sign-in page.
 * @property isUnmetered whether data on it costs nothing extra, which work limited to Wi-Fi waits for.
 */
data class NetworkState(val isOnline: Boolean, val isUnmetered: Boolean)
