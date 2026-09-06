package mihon.entry.interactions.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun isEntryDownloadNetworkAllowed(isOnline: Boolean, isWifi: Boolean, requireWifi: Boolean): Boolean =
    isOnline && (!requireWifi || isWifi)

internal data class EntryDownloadNetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isWifi: Boolean,
) {
    val isOnline: Boolean = isConnected && isValidated
}

private val Context.connectivityManager: ConnectivityManager
    get() = getSystemService()!!

@Suppress("DEPRECATION")
private fun Context.activeEntryDownloadNetworkState(): EntryDownloadNetworkState {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return EntryDownloadNetworkState(
        isConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false,
        isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
        isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
    )
}

internal fun Context.entryDownloadNetworkStateFlow(): Flow<EntryDownloadNetworkState> = callbackFlow {
    val callback = object : NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            trySend(activeEntryDownloadNetworkState())
        }

        override fun onLost(network: Network) {
            trySend(activeEntryDownloadNetworkState())
        }
    }
    trySend(activeEntryDownloadNetworkState())
    connectivityManager.registerDefaultNetworkCallback(callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}
