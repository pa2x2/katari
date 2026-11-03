package eu.kanade.tachiyomi.extension.util

import eu.kanade.tachiyomi.extension.file.FileExtensionInstaller
import eu.kanade.tachiyomi.extension.file.FileExtensionStorage
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.ListedDownload
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.store.isInstallable
import eu.kanade.tachiyomi.extension.store.requireSignedByStore
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import logcat.LogPriority
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The installer which installs, updates and uninstalls the extensions. It queues the installs and downloads them;
 * [packageInstaller] installs an apk the way the platform does.
 */
class ExtensionInstaller internal constructor(
    private val scope: CoroutineScope,
    private val networkHelper: Lazy<NetworkHelper>,
    private val packageInstaller: ExtensionPackageInstaller,
    private val loader: ExtensionLoader,
    private val kinds: ExtensionKinds,
    private val files: ExtensionFiles,
    private val installEvents: ExtensionInstallEvents,
) {

    enum class UserActionBehavior {
        LaunchPrompt,
        MarkAsRequiresUserAction,
    }

    enum class InstallRoute {
        Configured,
        Private,
    }

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeSteps = ConcurrentHashMap<Long, MutableStateFlow<InstallStep>>()
    private val downloadPackageNames = ConcurrentHashMap<Long, String>()
    private val packageInstallSteps = MutableStateFlow<Map<String, InstallStep>>(emptyMap())

    private val httpClient: OkHttpClient by lazy { networkHelper.value.client }

    private val fileInstaller by lazy { FileExtensionInstaller(kinds, FileExtensionStorage(files)) { httpClient } }

    internal val installSteps = packageInstallSteps.asStateFlow()

    /**
     * Adds the given extension to the downloads queue and returns an observable containing its
     * step in the installation process.
     *
     * @param extension The listing to install, whose apk must be signed with its store's key.
     * @param installRoute Installer route for this operation; a file is always installed privately.
     */
    internal fun downloadAndInstall(
        extension: Extension.Available,
        userActionBehavior: UserActionBehavior = UserActionBehavior.LaunchPrompt,
        installRoute: InstallRoute = InstallRoute.Configured,
    ): Flow<InstallStep> {
        val downloadId = extension.pkgName.hashCode().toLong()
        cancelInstall(extension.pkgName)

        val step = MutableStateFlow<InstallStep>(InstallStep.Pending)
        activeSteps[downloadId] = step
        downloadPackageNames[downloadId] = extension.pkgName
        packageInstallSteps.update { it + (extension.pkgName to InstallStep.Pending) }

        val job = scope.launch {
            try {
                // Its row offers no install, but a long press on it still asks for one
                check(extension.isInstallable(kinds)) { "Katari can't run ${extension.name} ${extension.versionName}" }
                updateInstallStep(downloadId, InstallStep.Downloading)
                when (val download = extension.download) {
                    is ListedDownload.Apk -> {
                        downloadAndInstallApk(downloadId, extension, download, userActionBehavior, installRoute)
                    }
                    is ListedDownload.File -> {
                        val replaced = fileInstaller.install(extension, download) {
                            updateInstallStep(downloadId, InstallStep.Installing)
                        }
                        updateInstallStep(downloadId, InstallStep.Installed)
                        if (replaced) {
                            installEvents.announceReplaced(extension.pkgName)
                        } else {
                            installEvents.announceAdded(extension.pkgName)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Ends this install's flow, which a newer install of the same package no longer shares
                step.value = InstallStep.Idle
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                updateInstallStep(downloadId, InstallStep.Error.from(e))
            }
        }

        activeJobs[extension.pkgName] = job

        return step.asStateFlow()
            .onCompletion {
                activeJobs.remove(extension.pkgName, job)
                // A newer install of the same package reuses the download id and owns the entries from then on
                if (activeSteps.remove(downloadId, step)) {
                    downloadPackageNames.remove(downloadId)
                    // A failure or an install left to the user stays listed until it's retried, cancelled or the
                    // package changes some other way
                    val lastStep = packageInstallSteps.value[extension.pkgName]
                    if (lastStep != InstallStep.RequiresUserAction && lastStep !is InstallStep.Error) {
                        packageInstallSteps.update { it - extension.pkgName }
                    }
                }
                job.cancel()
            }
    }

    private suspend fun downloadAndInstallApk(
        downloadId: Long,
        extension: Extension.Available,
        download: ListedDownload.Apk,
        userActionBehavior: UserActionBehavior,
        installRoute: InstallRoute,
    ) {
        val tmpFile = File(files.downloadDir, "extension_${extension.pkgName}.apk")
        val request = Request.Builder().url(download.url).build()
        httpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to download extension (HTTP ${response.code})")
            }
            // Interruptible so cancelInstall stops the body read too
            runInterruptible {
                response.body.byteStream().use { input ->
                    tmpFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        val extensionPackage = runCatching { readStoreSignedApk(extension, tmpFile) }
            .onFailure { tmpFile.delete() }
            .getOrThrow()

        currentCoroutineContext().ensureActive()
        updateInstallStep(downloadId, InstallStep.Installing)
        packageInstaller.install(downloadId, tmpFile, extensionPackage, userActionBehavior, installRoute) { step ->
            updateInstallStep(downloadId, step)
        }
    }

    private fun readStoreSignedApk(extension: Extension.Available, apk: File): ExtensionPackage {
        val extensionPackage = checkNotNull(loader.getArchivePackage(apk)) {
            "The downloaded file isn't an apk"
        }
        extension.requireSignedByStore(extensionPackage.signatures.orEmpty())
        return extensionPackage
    }

    /**
     * Cancels extension install and remove from download manager and installer.
     */
    internal fun cancelInstall(pkgName: String) {
        activeJobs.remove(pkgName)?.cancel()
        packageInstaller.cancel(pkgName.hashCode().toLong())
    }

    /**
     * Uninstalls the extension by the given package name: the platform asks the user about one installed for every
     * app, and a private one is removed at once.
     *
     * @param pkgName The package name of the extension to uninstall
     */
    internal fun uninstallApk(pkgName: String) {
        if (kinds.ofFilePkgName(pkgName) == null && packageInstaller.isInstalledShared(pkgName)) {
            packageInstaller.uninstallShared(pkgName)
        } else {
            uninstallPrivateExtension(pkgName)
            installEvents.announceRemoved(pkgName)
        }
    }

    /** Removes a private apk, or a file extension, which is always private. */
    internal fun uninstallPrivateExtension(pkgName: String) {
        val file = kinds.ofFilePkgName(pkgName)
        if (file != null) {
            val (kind, id) = file
            FileExtensionStorage(files).delete(kind, id)
            return
        }
        packageInstaller.deletePrivate(pkgName)
    }

    /**
     * Sets the step of the installation of an extension.
     *
     * @param downloadId The id of the download.
     * @param step New install step.
     */
    internal fun updateInstallStep(downloadId: Long, step: InstallStep) {
        // Published before the install flow sees it, which completes on a final step and drops the download
        downloadPackageNames[downloadId]?.let { pkgName ->
            packageInstallSteps.update { it + (pkgName to step) }
        }
        activeSteps[downloadId]?.let { it.value = step }
    }

    internal fun clearInstallStep(pkgName: String) {
        packageInstallSteps.update { it - pkgName }
    }

    internal fun clearFailedInstallStep(pkgName: String) {
        packageInstallSteps.update { steps ->
            if (steps[pkgName] is InstallStep.Error) steps - pkgName else steps
        }
    }
}
