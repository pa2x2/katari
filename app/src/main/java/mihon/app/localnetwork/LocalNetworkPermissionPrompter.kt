package mihon.app.localnetwork

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import eu.kanade.tachiyomi.network.localnetwork.LocalNetworkAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Asks for the local network permission on whichever activity is in front when a network client reports a blocked
 * local connection. A request that the user declines is not repeated for the rest of the process, so every later
 * local request fails fast with an explanation instead of reopening the dialog.
 */
class LocalNetworkPermissionPrompter(
    private val application: Application,
) : Application.ActivityLifecycleCallbacks {

    private var resumedActivity: ComponentActivity? = null
    private var requestPending = false
    private var requestInFlight = false
    private var declined = false

    fun install(scope: CoroutineScope) {
        application.registerActivityLifecycleCallbacks(this)
        LocalNetworkAccess.permissionRequests
            .onEach {
                withContext(Dispatchers.Main) {
                    requestPending = true
                    launchPendingRequest()
                }
            }
            .launchIn(scope)
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = activity as? ComponentActivity
        finishRequest()
        launchPendingRequest()
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity === activity) resumedActivity = null
    }

    private fun launchPendingRequest() {
        val activity = resumedActivity ?: return
        if (!requestPending || requestInFlight) return
        requestPending = false
        if (declined || LocalNetworkAccess.isGranted(application)) return

        requestInFlight = true
        lateinit var launcher: ActivityResultLauncher<String>
        launcher = activity.activityResultRegistry.register(
            REQUEST_KEY,
            ActivityResultContracts.RequestPermission(),
        ) {
            launcher.unregister()
            finishRequest()
        }
        launcher.launch(LocalNetworkAccess.PERMISSION)
    }

    /**
     * Settles the request from its result, or from the next resumed activity when a recreated activity dropped the
     * result callback.
     */
    private fun finishRequest() {
        if (!requestInFlight) return
        requestInFlight = false
        if (!LocalNetworkAccess.isGranted(application)) declined = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val REQUEST_KEY = "local_network_permission"
    }
}
