package mihon.desktop.network

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
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.network.NetworkState
import mihon.desktop.portal.PortalGdbus

/**
 * Linux: the network as the XDG desktop portal's `NetworkMonitor` reports it, read again on each of its `changed`
 * signals. Online means the network reaches the internet, as the portal's full connectivity says, and metered is the
 * portal's own answer. The portal doesn't say whether the network is Wi-Fi, so an unmetered network counts as Wi-Fi:
 * the Wi-Fi-only settings keep work off mobile data, which desktops reach through mobile broadband or a phone's
 * hotspot, connections the system marks metered, while a wired network is as free to use as Wi-Fi. Without the
 * portal, or with one that has no network monitor, the network counts as online, unmetered and Wi-Fi, so work waiting
 * for a network isn't held back by a desktop that can't tell. One monitor serves every collector at once.
 */
class PortalNetworkConnectivity(scope: CoroutineScope) : NetworkConnectivity {
    private val state = callbackFlow {
        val monitor = PortalGdbus.monitor()
        send(read())
        if (monitor != null) {
            launch(Dispatchers.IO) {
                monitor.inputStream.bufferedReader().useLines { lines ->
                    lines.filter { CHANGED_SIGNAL in it }.forEach { send(read()) }
                }
            }
        }
        awaitClose { monitor?.destroy() }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        // Started for the first collector and stopped after the last, so a new one never gets a stale state.
        .shareIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

    override fun state(): Flow<NetworkState> = state

    private fun read(): NetworkState {
        PortalGdbus.call("$INTERFACE.GetStatus", timeoutSeconds = READ_TIMEOUT_SECONDS)?.let { status ->
            val available = AVAILABLE.find(status)?.groupValues?.get(1)?.toBoolean() ?: true
            val metered = METERED.find(status)?.groupValues?.get(1)?.toBoolean() ?: false
            val connectivity = CONNECTIVITY.find(status)?.groupValues?.get(1)?.toIntOrNull() ?: FULL_CONNECTIVITY
            return networkState(online = available && connectivity == FULL_CONNECTIVITY, metered = metered)
        }
        // Portals before version 3 answer each part on its own
        val available = PortalGdbus.call("$INTERFACE.GetAvailable", timeoutSeconds = READ_TIMEOUT_SECONDS)
            ?: return networkState(online = true, metered = false)
        val metered = PortalGdbus.call("$INTERFACE.GetMetered", timeoutSeconds = READ_TIMEOUT_SECONDS)
        val connectivity = PortalGdbus.call("$INTERFACE.GetConnectivity", timeoutSeconds = READ_TIMEOUT_SECONDS)
            ?.let { UINT.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: FULL_CONNECTIVITY
        return networkState(
            online = "true" in available && connectivity == FULL_CONNECTIVITY,
            metered = metered?.contains("true") == true,
        )
    }

    private fun networkState(online: Boolean, metered: Boolean) =
        NetworkState(isOnline = online, isMetered = metered, countsAsWifi = !metered)

    private companion object {
        const val INTERFACE = "org.freedesktop.portal.NetworkMonitor"
        const val CHANGED_SIGNAL = "$INTERFACE.changed"
        const val READ_TIMEOUT_SECONDS = 2

        /** The portal's `connectivity` when the network reaches the internet, rather than a local network or a login. */
        const val FULL_CONNECTIVITY = 4

        // gdbus prints the status as `({'available': <true>, 'metered': <false>, 'connectivity': <uint32 4>},)`
        val AVAILABLE = Regex("""'available': <(true|false)>""")
        val METERED = Regex("""'metered': <(true|false)>""")
        val CONNECTIVITY = Regex("""'connectivity': <uint32 (\d+)>""")
        val UINT = Regex("""uint32 (\d+)""")
    }
}
