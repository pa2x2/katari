package eu.kanade.tachiyomi.extension.platform

import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.InstallRoute
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import java.io.File

/** Installs and uninstalls apk extensions on the platform, as the shared installer's queue hands them over. */
interface ExtensionPackageInstaller {

    /**
     * Installs [apk], read as [extensionPackage] and already checked to be signed with its store's key, by [route], and
     * deletes it once it's no longer needed. [onStep] takes the steps the install reaches here; an install handed to an
     * installer that reports later reports to the extension manager under [downloadId].
     */
    fun install(
        downloadId: Long,
        apk: File,
        extensionPackage: ExtensionPackage,
        userActionBehavior: UserActionBehavior,
        route: InstallRoute,
        onStep: (InstallStep) -> Unit,
    )

    /** Takes the install of [downloadId] out of the platform installer's queue, if it's waiting there. */
    fun cancel(downloadId: Long)

    /** Whether [packageName] is installed for every app, so uninstalling it goes through [uninstallShared]. */
    fun isInstalledShared(packageName: String): Boolean

    /** Asks the platform to uninstall [packageName], which is installed for every app; it reports the removal. */
    fun uninstallShared(packageName: String)

    /** Deletes the app's private copy of [packageName], if it has one. */
    fun deletePrivate(packageName: String)
}
