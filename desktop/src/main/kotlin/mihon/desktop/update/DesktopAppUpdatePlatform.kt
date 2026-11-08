package mihon.desktop.update

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.desktop.application.AppRestart
import mihon.desktop.application.DesktopBuildInfo
import mihon.desktop.application.installation.AppInstallation
import mihon.desktop.application.installation.InstallationUpdate
import mihon.desktop.release.DesktopRelease
import mihon.desktop.storage.DesktopDirectories
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import mihon.feature.appupdate.AppUpdatePlatform
import mihon.feature.appupdate.InstallResult
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.GithubAsset
import mihon.feature.appupdate.check.ReleaseVersion
import mihon.feature.appupdate.download.UpdateDownloader
import mihon.feature.appupdate.download.sha256
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File

/**
 * The desktop app's updates: the release's package for the app's [AppInstallation], put in the installed app's place,
 * which runs the new version once the app restarts.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopAppUpdatePlatform(
    network: NetworkHelper,
    directories: DesktopDirectories,
    private val restart: AppRestart,
    private val installation: AppInstallation,
) : AppUpdatePlatform {

    private val downloader = UpdateDownloader(directories.cache.resolve("app-update").toFile(), network)

    override val repository: String = DesktopRelease.REPOSITORY

    override val tagPrefix: String = DesktopRelease.TAG_PREFIX

    override val installedVersion: String = DesktopBuildInfo.VERSION_NAME

    /** The downloaded package of the update being installed. */
    @Volatile
    private var downloaded: File? = null

    override fun selectAsset(assets: List<GithubAsset>): GithubAsset? {
        val update = installation.update ?: return null
        return assets.find { update.isPackageName(it.name) }
    }

    override fun elapsedRealtime(): Long = System.nanoTime() / NANOS_PER_MILLI

    override suspend fun removeInstalledDownloads() {
        ReleaseVersion.parse(installedVersion)?.let(downloader::removeInstalled)
    }

    override fun clearDownloads() = downloader.clear()

    override suspend fun prepare(update: AvailableUpdate) {}

    override suspend fun download(update: AvailableUpdate, onProgress: (Float?) -> Unit) {
        // Before the download, which is large, rather than after it
        installationUpdate().checkReplaceable()
        downloaded = downloader.download(update.version, update.asset, onProgress)
    }

    override suspend fun verify(update: AvailableUpdate) = withIOContext {
        val file = downloaded?.takeIf(File::isFile)
        val expected = update.asset.sha256
        if (file == null ||
            (expected != null && !file.sha256().equals(expected, ignoreCase = true)) ||
            !installationUpdate().isPackage(file)
        ) {
            downloader.clear()
            throw AppUpdateException(AppUpdateError.Damaged)
        }
    }

    override fun canInstall(): Boolean = true

    override suspend fun install(update: AvailableUpdate): InstallResult = withIOContext {
        val file = downloaded ?: throw AppUpdateException(AppUpdateError.Damaged)
        installationUpdate().replace(file)
        InstallResult.RestartToFinish
    }

    override fun restart() = restart.request()

    private fun installationUpdate(): InstallationUpdate = installation.update
        ?: throw AppUpdateException(AppUpdateError.InstallFailed("The installation doesn't update itself"))

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
