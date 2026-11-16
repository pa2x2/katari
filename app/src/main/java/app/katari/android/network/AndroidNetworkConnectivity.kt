package app.katari.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.network.NetworkState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The default network: there is one while it has a transport, validated or not; online once Android has validated
 * that it reaches the internet and it isn't suspended, metered unless Android calls it not metered, and counting as
 * Wi-Fi while its transport is Wi-Fi.
 *
 * Each change is read from what its callback reports rather than from the active network: while a lost network is
 * being torn down, the active network can still be that one, and a state read from it then would stay until the next
 * network comes.
 */
@Inject
class AndroidNetworkConnectivity(private val context: Context) : NetworkConnectivity {
    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService()!!

    override fun state(): Flow<NetworkState> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(stateOf(networkCapabilities))
            }

            // Of the default network: until another becomes the default, there is none
            override fun onLost(network: Network) {
                trySend(stateOf(null))
            }
        }
        trySend(current())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    override fun current(): NetworkState = connectivityManager.run { stateOf(getNetworkCapabilities(activeNetwork)) }

    private fun stateOf(capabilities: NetworkCapabilities?) = NetworkState(
        // The transports source errors have always counted, cellular to LoWPAN with a VPN among them; later ones, such
        // as USB or satellite, stay out so an unresolved host reads as it always did
        hasNetwork = (NetworkCapabilities.TRANSPORT_CELLULAR..NetworkCapabilities.TRANSPORT_LOWPAN)
            .any { capabilities?.hasTransport(it) == true },
        isOnline = capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED),
        isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true,
        countsAsWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
    )
}
