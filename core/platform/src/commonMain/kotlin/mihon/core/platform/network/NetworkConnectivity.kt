package mihon.core.platform.network

import kotlinx.coroutines.flow.Flow
import mihon.core.common.platform.PlatformFacility

/** The network the device is on, as background work that needs one waits for it. */
@PlatformFacility
interface NetworkConnectivity {
    /** The network now, then each change, for as long as it is collected. */
    fun state(): Flow<NetworkState>

    /** The network now, for a caller that can't wait for [state], such as wording an error while it is shown. */
    fun current(): NetworkState

    /**
     * Whether the platform knows a network's transport, so [NetworkState.countsAsWifi] means Wi-Fi itself. Where it
     * doesn't, it means a network the system doesn't call metered, and settings for Wi-Fi-only work say so.
     */
    val knowsWifi: Boolean
}

/**
 * @property hasNetwork whether the device is on a network at all, whether or not it reaches the internet: a local
 * network or one behind a sign-in page counts. Without one, a host that didn't resolve means the device is offline;
 * with one, the host itself may be wrong or blocked.
 * @property isOnline whether the network reaches the internet, not only a local network or a sign-in page.
 * @property isMetered whether the system says data on it may cost the user, so large transfers nobody waits for
 * should not start on it.
 * @property countsAsWifi whether work the user limited to Wi-Fi may use it: whether it is Wi-Fi where the platform
 * knows the network's transport, otherwise whether the system doesn't call it metered.
 */
data class NetworkState(
    val hasNetwork: Boolean,
    val isOnline: Boolean,
    val isMetered: Boolean,
    val countsAsWifi: Boolean,
)
