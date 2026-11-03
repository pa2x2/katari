package eu.kanade.tachiyomi.network.localnetwork

import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android 17's local network permission, kept for extensions that ask for it. Desktop has no such permission, so local
 * connections are always allowed and nothing asks the UI to request it.
 */
object LocalNetworkAccess {

    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val permissionRequests: SharedFlow<Unit> = requests.asSharedFlow()

    fun isGranted(context: Context): Boolean = true

    internal fun requestPermission() {
        requests.tryEmit(Unit)
    }
}
