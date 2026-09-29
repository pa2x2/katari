package eu.kanade.tachiyomi.network.localnetwork

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide state of Android 17's local network permission. Network clients report blocked local connections here,
 * and the foreground UI answers [permissionRequests] by asking the user.
 */
object LocalNetworkAccess {

    const val PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val permissionRequests: SharedFlow<Unit> = requests.asSharedFlow()

    fun isGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ||
            context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    internal fun requestPermission() {
        requests.tryEmit(Unit)
    }
}
