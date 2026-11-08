package app.katari.desktop.extension

import androidx.compose.runtime.Composable
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.model.Extension

/**
 * Desktop installs no extension as an app of the system's, so there is no app info to open, no permission to install
 * apps and no system uninstaller; the app asks before uninstalling itself.
 */
@Inject
class NoExtensionSystemPackages : ExtensionSystemPackages {
    override fun hasAppInfo(extension: Extension.Installed): Boolean = false

    override fun openAppInfo(context: PlatformContext, extension: Extension.Installed) =
        error("Desktop installs no extension as an app")

    override fun asksBeforeUninstall(extension: Extension.Installed): Boolean = false

    @Composable
    override fun isInstallPermissionMissing(): Boolean = false

    override fun requestInstallPermission(context: PlatformContext) = error("Desktop installs no extension as an app")
}
