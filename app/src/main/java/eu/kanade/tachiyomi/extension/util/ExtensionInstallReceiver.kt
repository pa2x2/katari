package eu.kanade.tachiyomi.extension.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Broadcast receiver that listens for the system's packages installed, updated or removed, and only
 * notifies the given [listener] when the package is an extension.
 *
 * @param listener The listener that should be notified of extension installation events.
 */
internal class ExtensionInstallReceiver(
    private val listener: Listener,
    private val scope: CoroutineScope,
) : BroadcastReceiver() {

    // Handled one at a time in arrival order, so a quick removal can't overtake the load of an earlier install
    private val events = Channel<Event>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (event in events) {
                try {
                    handle(event)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e) { "Failed to handle $event" }
                }
            }
        }
    }

    fun register(context: Context) {
        ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private val filter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(ACTION_EXTENSION_ADDED)
        addAction(ACTION_EXTENSION_REPLACED)
        addAction(ACTION_EXTENSION_REMOVED)
        addDataScheme("package")
    }

    /**
     * Called when one of the events of the [filter] is received. When the package is an extension,
     * it's loaded in background and it notifies the [listener] when finished.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val pkgName = getPackageNameFromIntent(intent)
        if (pkgName == null) {
            logcat(LogPriority.WARN) { "Package name not found" }
            return
        }

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, ACTION_EXTENSION_ADDED -> {
                if (isReplacing(intent)) return
                events.trySend(Event.Added(context, pkgName))
            }
            Intent.ACTION_PACKAGE_REPLACED, ACTION_EXTENSION_REPLACED -> {
                events.trySend(Event.Replaced(context, pkgName))
            }
            Intent.ACTION_PACKAGE_REMOVED, ACTION_EXTENSION_REMOVED -> {
                if (isReplacing(intent)) return
                events.trySend(Event.Removed(pkgName))
            }
        }
    }

    /**
     * Returns true if this package is performing an update.
     *
     * @param intent The intent that triggered the event.
     */
    private fun isReplacing(intent: Intent): Boolean {
        return intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
    }

    private suspend fun handle(event: Event) {
        when (event) {
            is Event.Added -> when (
                val extension = ExtensionLoader.loadExtensionFromPkgName(event.context, event.pkgName)
            ) {
                is Extension.Loaded -> listener.onExtensionInstalled(extension)
                is Extension.NotLoaded -> listener.onExtensionNotLoaded(extension)
                null -> {}
            }
            is Event.Replaced -> when (
                val extension = ExtensionLoader.loadExtensionFromPkgName(event.context, event.pkgName)
            ) {
                is Extension.Loaded -> listener.onExtensionUpdated(extension)
                is Extension.NotLoaded -> listener.onExtensionNotLoaded(extension)
                null -> {}
            }
            is Event.Removed -> listener.onPackageUninstalled(event.pkgName)
        }
    }

    /**
     * Returns the package name of the installed, updated or removed application.
     */
    private fun getPackageNameFromIntent(intent: Intent?): String? {
        return intent?.data?.schemeSpecificPart
    }

    /**
     * Listener that receives extension installation events.
     */
    interface Listener {
        suspend fun onExtensionInstalled(extension: Extension.Loaded)
        suspend fun onExtensionUpdated(extension: Extension.Loaded)
        suspend fun onExtensionNotLoaded(extension: Extension.NotLoaded)
        suspend fun onPackageUninstalled(pkgName: String)
    }

    private sealed interface Event {
        data class Added(val context: Context, val pkgName: String) : Event
        data class Replaced(val context: Context, val pkgName: String) : Event
        data class Removed(val pkgName: String) : Event
    }

    companion object {
        private const val ACTION_EXTENSION_ADDED = "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_ADDED"
        private const val ACTION_EXTENSION_REPLACED = "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_REPLACED"
        private const val ACTION_EXTENSION_REMOVED = "${BuildConfig.APPLICATION_ID}.ACTION_EXTENSION_REMOVED"

        fun notifyAdded(context: Context, pkgName: String) {
            notify(context, pkgName, ACTION_EXTENSION_ADDED)
        }

        fun notifyReplaced(context: Context, pkgName: String) {
            notify(context, pkgName, ACTION_EXTENSION_REPLACED)
        }

        fun notifyRemoved(context: Context, pkgName: String) {
            notify(context, pkgName, ACTION_EXTENSION_REMOVED)
        }

        private fun notify(context: Context, pkgName: String, action: String) {
            Intent(action).apply {
                // Encoded, since a file extension's ID can hold anything, such as a '#' a Uri would read as a fragment
                data = Uri.fromParts("package", pkgName, null)
                `package` = context.packageName
                context.sendBroadcast(this)
            }
        }
    }
}
