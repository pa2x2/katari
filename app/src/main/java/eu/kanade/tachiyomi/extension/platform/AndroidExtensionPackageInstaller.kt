package eu.kanade.tachiyomi.extension.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.installer.Installer
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstallActivity
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstallService
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.storage.copyAndSetReadOnlyTo
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException

/**
 * Installs an apk the way the user chose: Android's installer through an activity (legacy), PackageInstaller or Shizuku
 * through [ExtensionInstallService], or as a private copy, which an update of a private copy always stays.
 */
@Inject
class AndroidExtensionPackageInstaller(
    private val context: Context,
    basePreferences: BasePreferences,
    private val loader: ExtensionLoader,
    private val kinds: ExtensionKinds,
    private val files: ExtensionFiles,
) : ExtensionPackageInstaller {

    private val extensionInstaller = basePreferences.extensionInstaller

    override fun install(
        downloadId: Long,
        apk: File,
        extensionPackage: ExtensionPackage,
        userActionBehavior: UserActionBehavior,
        route: InstallRoute,
        onStep: (InstallStep) -> Unit,
    ) {
        if (route == InstallRoute.Private) {
            installApkPrivately(apk, extensionPackage, onStep)
            return
        }

        when (val installer = extensionInstaller.get()) {
            BasePreferences.ExtensionInstaller.LEGACY -> {
                if (userActionBehavior == UserActionBehavior.MarkAsRequiresUserAction) {
                    onStep(InstallStep.RequiresUserAction)
                    apk.delete()
                    return
                }
                val intent = Intent(context, ExtensionInstallActivity::class.java)
                    .setDataAndType(apk.getUriCompat(context), APK_MIME)
                    .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                context.startActivity(intent)
            }

            BasePreferences.ExtensionInstaller.PRIVATE -> {
                installApkPrivately(apk, extensionPackage, onStep)
            }

            else -> {
                val intent = ExtensionInstallService.getIntent(
                    context,
                    downloadId,
                    apk.getUriCompat(context),
                    installer,
                    userActionBehavior,
                )
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    private fun installApkPrivately(tempFile: File, extensionPackage: ExtensionPackage, onStep: (InstallStep) -> Unit) {
        try {
            installPrivateExtensionFile(tempFile, extensionPackage)
            onStep(InstallStep.Installed)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to install extension privately" }
            onStep(InstallStep.Error.from(e))
        }

        tempFile.delete()
    }

    /**
     * Throws with what stopped it when the extension can't be installed.
     */
    private fun installPrivateExtensionFile(file: File, extension: ExtensionPackage) {
        check(kinds.isExtension(extension)) { "${extension.packageName} isn't an extension" }
        val currentExtension = loader.getExtensionPackageFromPkgName(extension.packageName)

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

        val target = AndroidExtensionPackages.privateApk(files, extension.packageName)
        val staged = File(target.parentFile, "${target.name}.part")
        try {
            file.copyAndSetReadOnlyTo(staged, overwrite = true)
            if (!staged.renameTo(target)) throw IOException("Unable to replace ${target.name}")
        } finally {
            staged.delete()
        }
        if (currentExtension != null) {
            ExtensionInstallReceiver.notifyReplaced(context, extension.packageName)
        } else {
            ExtensionInstallReceiver.notifyAdded(context, extension.packageName)
        }
    }

    override fun cancel(downloadId: Long) {
        Installer.cancelInstallQueue(downloadId)
    }

    override fun isInstalledShared(packageName: String): Boolean = context.isPackageInstalled(packageName)

    override fun uninstallShared(packageName: String) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$packageName".toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun deletePrivate(packageName: String) {
        AndroidExtensionPackages.privateApk(files, packageName).delete()
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val EXTRA_DOWNLOAD_ID = "ExtensionInstaller.extra.DOWNLOAD_ID"
    }
}
