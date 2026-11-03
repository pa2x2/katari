package mihon.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.network.NetworkState

/**
 * The default network: there is one while it has a transport, validated or not; online once it is connected and
 * Android has validated that it reaches the internet, metered as Android judges the active network, and counting as
 * Wi-Fi while its transport is Wi-Fi.
 */
@Inject
class AndroidNetworkConnectivity(private val context: Context) : NetworkConnectivity {
    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService()!!

    override fun state(): Flow<NetworkState> = callbackFlow {
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(current())
            }

            override fun onLost(network: Network) {
                trySend(current())
            }
        }
        trySend(current())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    @Suppress("DEPRECATION")
    override fun current(): NetworkState {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false
        val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
        return NetworkState(
            // The transports source errors have always counted, cellular to LoWPAN with a VPN among them; later ones,
            // such as USB or satellite, stay out so an unresolved host reads as it always did
            hasNetwork = (NetworkCapabilities.TRANSPORT_CELLULAR..NetworkCapabilities.TRANSPORT_LOWPAN)
                .any { capabilities?.hasTransport(it) == true },
            isOnline = isConnected && isValidated,
            isMetered = connectivityManager.isActiveNetworkMetered,
            countsAsWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
        )
    }
}
