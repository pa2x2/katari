package eu.kanade.tachiyomi.extension.util

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Loads each extension the platform reports added or replaced and tells [listener] how it loaded, or that a package was
 * removed. Packages that aren't extensions load as nothing and are left out.
 */
internal class ExtensionInstallEventHandling(
    private val loader: ExtensionLoader,
    private val listener: Listener,
    scope: CoroutineScope,
) {

    // Handled one at a time in arrival order, so a quick removal can't overtake the load of an earlier install
    private val events = Channel<ExtensionInstallEvent>(Channel.UNLIMITED)

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

    fun onEvent(event: ExtensionInstallEvent) {
        events.trySend(event)
    }

    private suspend fun handle(event: ExtensionInstallEvent) {
        when (event) {
            is ExtensionInstallEvent.Added -> when (
                val extension = loader.loadExtensionFromPkgName(event.pkgName)
            ) {
                is Extension.Loaded -> listener.onExtensionInstalled(extension)
                is Extension.NotLoaded -> listener.onExtensionNotLoaded(extension)
                null -> {}
            }
            is ExtensionInstallEvent.Replaced -> when (
                val extension = loader.loadExtensionFromPkgName(event.pkgName)
            ) {
                is Extension.Loaded -> listener.onExtensionUpdated(extension)
                is Extension.NotLoaded -> listener.onExtensionNotLoaded(extension)
                null -> {}
            }
            is ExtensionInstallEvent.Removed -> listener.onPackageUninstalled(event.pkgName)
        }
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
}
