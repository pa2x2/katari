package mihon.extension.runtime.jvm.install

import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvent
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents

/**
 * Install events on desktop, where the app installs every extension itself and no other app does: each announcement
 * is handed over in the order it was made, and ones made before [register] wait for it.
 */
class DesktopExtensionInstallEvents : ExtensionInstallEvents {

    private val lock = Any()
    private var onEvent: ((ExtensionInstallEvent) -> Unit)? = null
    private val waiting = mutableListOf<ExtensionInstallEvent>()

    override fun register(onEvent: (ExtensionInstallEvent) -> Unit) {
        synchronized(lock) {
            check(this.onEvent == null) { "Install events are already registered" }
            this.onEvent = onEvent
            waiting.forEach(onEvent)
            waiting.clear()
        }
    }

    override fun announceAdded(pkgName: String) = announce(ExtensionInstallEvent.Added(pkgName))

    override fun announceReplaced(pkgName: String) = announce(ExtensionInstallEvent.Replaced(pkgName))

    override fun announceRemoved(pkgName: String) = announce(ExtensionInstallEvent.Removed(pkgName))

    private fun announce(event: ExtensionInstallEvent) {
        synchronized(lock) {
            val onEvent = onEvent
            if (onEvent == null) waiting.add(event) else onEvent(event)
        }
    }
}
