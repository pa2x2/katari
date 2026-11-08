package eu.kanade.tachiyomi.extension

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import tachiyomi.presentation.core.util.collectAsState

/** Extension apks installed for every app, which Android's settings, installer permission and uninstaller handle. */
@Inject
class AndroidExtensionSystemPackages(
    private val application: Application,
    private val basePreferences: BasePreferences,
) : ExtensionSystemPackages {
    override fun hasAppInfo(extension: Extension.Installed): Boolean =
        (extension.form as? InstalledForm.Package)?.isShared == true

    override fun openAppInfo(context: PlatformContext, extension: Extension.Installed) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", extension.pkgName, null)
            context.startActivity(this)
        }
    }

    override fun asksBeforeUninstall(extension: Extension.Installed): Boolean =
        application.isPackageInstalled(extension.pkgName)

    @Composable
    override fun isInstallPermissionMissing(): Boolean {
        val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)
        val installer by basePreferences.extensionInstaller.collectAsState()
        return !installGranted && installer.requiresSystemPermission
    }

    override fun requestInstallPermission(context: PlatformContext) {
        context.launchRequestPackageInstallsPermission()
    }
}
