package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import java.io.File

/**
 * Desktop installs no apks yet: the stores' apk listings aren't installable there (`DesktopHostedExtensionFamilies`), so
 * none reaches this, and there is none installed to uninstall. Stands in until desktop loads apk extensions.
 */
@Inject
class NoExtensionPackageInstaller : ExtensionPackageInstaller {
    override fun install(
        downloadId: Long,
        apk: File,
        extensionPackage: ExtensionPackage,
        userActionBehavior: UserActionBehavior,
        route: InstallRoute,
        onStep: (InstallStep) -> Unit,
    ) {
        apk.delete()
        error("Desktop installs no apk extensions yet: ${extensionPackage.packageName}")
    }

    override fun cancel(downloadId: Long) = Unit

    override fun isInstalledShared(packageName: String): Boolean = false

    override fun uninstallShared(packageName: String) = error("Desktop installs no extension for every app")

    override fun deletePrivate(packageName: String) = Unit
}
