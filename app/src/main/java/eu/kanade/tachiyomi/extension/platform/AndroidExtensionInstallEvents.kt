package eu.kanade.tachiyomi.extension.platform

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver

/** Android's package broadcasts and the app's own, which reach [ExtensionInstallReceiver] alike. */
@Inject
class AndroidExtensionInstallEvents(private val context: Context) : ExtensionInstallEvents {

    override fun register(onEvent: (ExtensionInstallEvent) -> Unit) {
        ExtensionInstallReceiver(onEvent).register(context)
    }

    override fun announceAdded(pkgName: String) {
        ExtensionInstallReceiver.notifyAdded(context, pkgName)
    }

    override fun announceReplaced(pkgName: String) {
        ExtensionInstallReceiver.notifyReplaced(context, pkgName)
    }

    override fun announceRemoved(pkgName: String) {
        ExtensionInstallReceiver.notifyRemoved(context, pkgName)
    }
}
