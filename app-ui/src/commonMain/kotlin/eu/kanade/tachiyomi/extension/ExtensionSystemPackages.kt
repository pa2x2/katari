package eu.kanade.tachiyomi.extension

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/**
 * What the system itself does for extensions it installs as its own apps, as Android does with apks: their page in the
 * system settings, its permission to install them and its own uninstall prompt. Where the platform installs none,
 * nothing here applies and what would use it hides.
 */
@PlatformFacility
interface ExtensionSystemPackages {
    /** Whether the system settings have a page of [extension]'s, which its details then link to. */
    fun hasAppInfo(extension: Extension.Installed): Boolean

    /** Only called while [hasAppInfo]. */
    fun openAppInfo(context: PlatformContext, extension: Extension.Installed)

    /** Whether uninstalling [extension] is up to the system, which asks the user itself; otherwise the app asks first. */
    fun asksBeforeUninstall(extension: Extension.Installed): Boolean

    /**
     * True while the chosen way of installing extensions needs the system's permission to install apps and the app
     * doesn't have it, checked again each time the screen resumes.
     */
    @Composable
    fun isInstallPermissionMissing(): Boolean

    /** Only called while [isInstallPermissionMissing]. */
    fun requestInstallPermission(context: PlatformContext)
}
