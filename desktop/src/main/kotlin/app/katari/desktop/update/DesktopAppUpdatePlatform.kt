package app.katari.desktop.update

import app.katari.appupdate.AppUpdateError
import app.katari.appupdate.AppUpdateException
import app.katari.appupdate.AppUpdatePlatform
import app.katari.appupdate.InstallResult
import app.katari.appupdate.check.AvailableUpdate
import app.katari.appupdate.check.GithubAsset
import app.katari.appupdate.check.ReleaseVersion
import app.katari.appupdate.download.UpdateDownloader
import app.katari.appupdate.download.sha256
import app.katari.desktop.application.AppRestart
import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.application.installation.AppInstallation
import app.katari.desktop.application.installation.InstallationUpdate
import app.katari.desktop.release.DesktopRelease
import app.katari.desktop.storage.DesktopDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
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
