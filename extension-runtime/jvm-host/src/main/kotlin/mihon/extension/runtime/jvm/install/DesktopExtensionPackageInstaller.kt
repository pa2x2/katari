package mihon.extension.runtime.jvm.install

import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import logcat.LogPriority
import mihon.extension.runtime.jvm.apk.DesktopExtensionPackage
import mihon.extension.runtime.jvm.apk.DesktopExtensionPackages
import mihon.extension.runtime.jvm.conversion.ConvertedJarCache
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Installs apks the only way desktop has: as the app's private copy, with the checks Android's private install makes
 * (no downgrade, and an update signed with the installed version's key). The apk's code is converted before the copy
 * replaces anything ([InstallStep.Converting]), so an apk the host can't convert leaves the installed version as it
 * was. Uninstalling deletes the copy and its converted jars.
 */
class DesktopExtensionPackageInstaller(
    private val loader: Lazy<ExtensionLoader>,
    private val kinds: ExtensionKinds,
    private val files: ExtensionFiles,
    private val installEvents: ExtensionInstallEvents,
    private val jars: ConvertedJarCache,
) : ExtensionPackageInstaller {

    override fun install(
        downloadId: Long,
        apk: File,
        extensionPackage: ExtensionPackage,
        userActionBehavior: UserActionBehavior,
        route: InstallRoute,
        onStep: (InstallStep) -> Unit,
    ) {
        try {
            installPrivately(apk, extensionPackage as DesktopExtensionPackage, onStep)
            onStep(InstallStep.Installed)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to install extension privately" }
            onStep(InstallStep.Error.from(e))
        } finally {
            apk.delete()
        }
    }

    private fun installPrivately(file: File, extension: DesktopExtensionPackage, onStep: (InstallStep) -> Unit) {
        check(kinds.isExtension(extension)) { "${extension.packageName} isn't an extension" }
        val currentExtension = loader.value.getExtensionPackageFromPkgName(extension.packageName)

        if (currentExtension != null) {
            check(extension.versionCode >= currentExtension.versionCode) {
                "The installed version is newer, and downgrading isn't allowed"
            }

            val extensionSignatures = extension.signatures
            check(!extensionSignatures.isNullOrEmpty()) { "The extension isn't signed" }
            val currentSignatures =
                checkNotNull(currentExtension.signatures) { "The installed extension isn't signed" }
            check(extensionSignatures.containsAll(currentSignatures)) {
                "It isn't signed with the same key as the installed extension"
            }
        }

        onStep(InstallStep.Converting)
        jars.jarOf(extension)

        val target = DesktopExtensionPackages.privateApk(files, extension.packageName)
        val staged = File(target.parentFile, "${target.name}.part")
        try {
            target.parentFile.mkdirs()
            file.copyTo(staged, overwrite = true)
            // A rename in one folder, which replaces the installed copy at once
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            throw IOException("Unable to replace ${target.name}", e)
        } finally {
            staged.delete()
        }
        if (currentExtension != null) {
            installEvents.announceReplaced(extension.packageName)
        } else {
            installEvents.announceAdded(extension.packageName)
        }
    }

    // Each install finishes inside install(), so none waits in a queue
    override fun cancel(downloadId: Long) = Unit

    override fun isInstalledShared(packageName: String): Boolean = false

    override fun uninstallShared(packageName: String) = error("Desktop installs no extension for every app")

    override fun deletePrivate(packageName: String) {
        DesktopExtensionPackages.privateApk(files, packageName).delete()
        jars.remove(packageName)
    }
}
