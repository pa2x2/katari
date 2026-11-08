package app.katari.appupdate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.eventFlow
import app.katari.appupdate.check.AvailableUpdate
import app.katari.appupdate.check.GithubAsset
import app.katari.appupdate.check.ReleaseVersion
import app.katari.appupdate.check.selectReleaseApk
import app.katari.appupdate.download.UpdateDownloader
import app.katari.appupdate.install.AppUpdateInstaller
import app.katari.appupdate.install.InstallOutcome
import app.katari.appupdate.install.UpdateApkVerifier
import app.katari.appupdate.install.removeLegacyUpdateDownload
import app.katari.appupdate.mpvaddon.MpvAddonController
import app.katari.appupdate.mpvaddon.MpvAddonRelease
import app.katari.appupdate.mpvaddon.MpvAddonReleaseSource
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.addon.MpvAddonProblem
import app.katari.video.engine.mpv.addon.MpvAddonState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * The Android app's updates: the release APK for this build's distribution and the device's ABI, installed through a
 * PackageInstaller session once the user allows installs from Katari.
 *
 * When the mpv add-on is installed and the new version needs another one, the update brings that add-on along and
 * installs it first, so mpv keeps working once the new version starts. Public so the public app graph can bind it,
 * while what it is made of stays internal to the app.
 */
class AndroidAppUpdatePlatform internal constructor(
    private val context: Context,
    private val downloader: UpdateDownloader,
    private val verifier: UpdateApkVerifier,
    private val installer: AppUpdateInstaller,
    private val mpvAddon: MpvAddon,
    private val mpvAddonSource: MpvAddonReleaseSource,
    private val mpvAddonController: MpvAddonController,
) : AppUpdatePlatform {

    override val repository: String = AppUpdateBuild.REPOSITORY

    override val tagPrefix: String = "v"

    override val installedVersion: String get() = AppUpdateBuild.versionName

    /** The add-on the last found update brings along, by that update's version. */
    @Volatile
    private var addon: Pair<String, MpvAddonRelease>? = null

    /** The APKs of the update being installed. */
    @Volatile
    private var downloaded: DownloadedUpdate? = null

    /** The mpv add-on [update] installs first, or null when it brings none. */
    internal fun addonOf(
        update: AvailableUpdate,
    ): MpvAddonRelease? = addon?.takeIf { it.first == update.version }?.second

    /** The system page that allows installs from Katari ("Install unknown apps"). */
    internal fun installPermissionIntent(): Intent = installer.installPermissionIntent()

    override fun selectAsset(assets: List<GithubAsset>): GithubAsset? =
        selectReleaseApk(assets, AppUpdateBuild.isFoss, Build.SUPPORTED_ABIS.first())

    /** Keeps counting while the device sleeps. */
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override suspend fun removeInstalledDownloads() {
        ReleaseVersion.parse(installedVersion)?.let(downloader::removeInstalled)
        removeLegacyUpdateDownload(context)
    }

    override fun clearDownloads() = downloader.clear()

    override suspend fun prepare(update: AvailableUpdate) {
        addon = neededAddon(update)?.let { update.version to it }
    }

    override suspend fun download(update: AvailableUpdate, onProgress: (Float?) -> Unit) {
        val addon = addonOf(update)
        val totalBytes = update.asset.size + (addon?.apk?.size ?: 0L)
        val addonDownload = addon?.let {
            val apk = mpvAddonController.download(it) { progress ->
                onProgress(overall(progress, 0L, it.apk.size, totalBytes))
            }
            DownloadedAddon(apk, it)
        }
        val apk = downloader.download(update.version, update.asset) { progress ->
            onProgress(overall(progress, addon?.apk?.size ?: 0L, update.asset.size, totalBytes))
        }
        downloaded = DownloadedUpdate(apk, addonDownload)
    }

    override suspend fun verify(update: AvailableUpdate) {
        val downloaded = downloaded ?: throw AppUpdateException(AppUpdateError.Damaged)
        try {
            verifier.verify(downloaded.apk, update.asset.sha256, verifier.selfUpdate())
        } catch (e: AppUpdateException) {
            if (e.error == AppUpdateError.Damaged) downloader.clear()
            throw e
        }
        downloaded.addon?.let { mpvAddonController.verify(it.apk, it.release) }
    }

    override fun canInstall(): Boolean = installer.canInstall()

    override suspend fun install(update: AvailableUpdate): InstallResult {
        val downloaded = downloaded ?: throw AppUpdateException(AppUpdateError.Damaged)
        if (downloaded.addon != null &&
            mpvAddonController.installVerified(downloaded.addon.apk) == InstallOutcome.Cancelled
        ) {
            return InstallResult.Cancelled
        }
        return when (installer.install(downloaded.apk)) {
            InstallOutcome.Installed -> InstallResult.Installed
            InstallOutcome.Cancelled -> InstallResult.Cancelled
        }
    }

    // Coming back to Katari is how an install that waited for the permission or a confirmation screen continues.
    // The lifecycle takes its observers on the main thread.
    override val foregrounds: Flow<Unit> = ProcessLifecycleOwner.get().lifecycle.eventFlow
        .filter { it == Lifecycle.Event.ON_START }
        .map {}
        .flowOn(Dispatchers.Main)

    override fun onForeground() = installer.onForeground()

    /** Reports one APK's [progress] as part of all the update's downloads, which start [before] bytes in. */
    private fun overall(progress: Float?, before: Long, size: Long, total: Long): Float? =
        progress?.takeIf { total > 0 }?.let { (before + it * size) / total }

    /**
     * The add-on [update] needs instead of the installed one, or null when the installed one will do or there is none
     * to update. A failed lookup leaves the add-on out rather than holding back Katari's update.
     */
    private suspend fun neededAddon(update: AvailableUpdate): MpvAddonRelease? {
        val installed = mpvAddon.read() as? MpvAddonState.Installed ?: return null
        if (installed.problem == MpvAddonProblem.ForeignSigner) return null
        val requirementUrl = update.release.assets
            .find { it.name == mpvRequirementAssetName(update.release.tagName) }
            ?.downloadUrl
            ?: return null
        return try {
            val needed = mpvAddonSource.katariRequirement(requirementUrl) ?: return null
            if (installed.glue == needed.glue && installed.versionCode >= needed.minVersionCode) return null
            mpvAddonSource.newest(needed.glue, needed.minVersionCode, mpvAddon.requirement.abi)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Looking up the mpv add-on for ${update.version} failed" }
            null
        }
    }
}

/** The mpv add-on a release needs, as the release workflow publishes it. */
private fun mpvRequirementAssetName(tag: String) = "katari-$tag-mpv.json"

private data class DownloadedUpdate(val apk: File, val addon: DownloadedAddon?)

private data class DownloadedAddon(val apk: File, val release: MpvAddonRelease)
