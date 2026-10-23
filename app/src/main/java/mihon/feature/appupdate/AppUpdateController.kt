package mihon.feature.appupdate

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import logcat.LogPriority
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.check.ReleaseVersion
import mihon.feature.appupdate.check.selectUpdate
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.InstallOutcome
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.MpvAddonRelease
import mihon.feature.appupdate.mpvaddon.MpvAddonReleaseSource
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonProblem
import mihon.video.engine.mpv.addon.MpvAddonState
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.hours

/**
 * The app's one update flow: finds a newer GitHub release, downloads its APK and installs it. The prompt in
 * MainActivity and the About screen both show [state]; when the user was already told about a version is kept in
 * [AppUpdatePreferences], the rest lasts for the process.
 *
 * When the mpv add-on is installed and the new version needs another one, the update carries that add-on and installs
 * it first, so mpv keeps working once the new version starts.
 */
internal class AppUpdateController(
    private val preferences: AppUpdatePreferences,
    private val source: GithubReleaseSource,
    private val downloader: UpdateApkDownloader,
    private val verifier: UpdateApkVerifier,
    private val installer: AppUpdateInstaller,
    private val mpvAddon: MpvAddon,
    private val mpvAddonSource: MpvAddonReleaseSource,
    private val mpvAddonController: MpvAddonController,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    private val launchHandled = AtomicBoolean(false)

    /** [SystemClock.elapsedRealtime] of the last automatic check, which keeps counting while the device sleeps. */
    @Volatile
    private var lastAutomaticCheckAt = 0L
    private var checkJob: Job? = null
    private var updateJob: Job? = null

    /** The APKs waiting for the install permission. */
    private var downloaded: DownloadedUpdate? = null

    init {
        // Coming back to Katari is how an install that waited for the permission or a confirmation screen continues.
        ProcessLifecycleOwner.get().lifecycle.eventFlow
            .filter { it == Lifecycle.Event.ON_START }
            .onEach { onForeground() }
            .launchIn(scope + Dispatchers.Main)
    }

    /** Runs once per process when the app is first ready for a prompt. */
    fun onLaunch() {
        if (!launchHandled.compareAndSet(false, true)) return
        lastAutomaticCheckAt = SystemClock.elapsedRealtime()
        scope.launch {
            ReleaseVersion.parse(AppUpdateBuild.versionName)?.let(downloader::removeInstalled)
            if (preferences.checkOnLaunch.get()) check(CheckMode.Automatic)
        }
    }

    /**
     * Repeats the automatic check every [AUTOMATIC_CHECK_INTERVAL] until cancelled, because Katari can stay open for
     * days and the launch check alone would rarely run. Callers keep it running only while the app is visible.
     */
    suspend fun recheckPeriodically() {
        while (true) {
            delay(lastAutomaticCheckAt + AUTOMATIC_CHECK_INTERVAL.inWholeMilliseconds - SystemClock.elapsedRealtime())
            lastAutomaticCheckAt = SystemClock.elapsedRealtime()
            if (preferences.checkOnLaunch.get()) check(CheckMode.Automatic)
        }
    }

    /** Checks on the user's request: a found update opens the prompt, and a failure is reported. */
    fun checkNow() = startCheck(CheckMode.User)

    /** Checks again after the channel changed, updating what About shows without opening the prompt. */
    fun refresh() = startCheck(CheckMode.Refresh)

    fun startUpdate() {
        val current = state.value
        val update = current.update ?: return
        if (current.status.isBusy || current.status == AppUpdateStatus.Checking) return
        mutableState.update { it.copy(status = AppUpdateStatus.Downloading(0f)) }
        updateJob = scope.launch {
            try {
                val addon = update.addon
                val totalBytes = update.apk.size + (addon?.apk?.size ?: 0L)
                val addonDownload = addon?.let {
                    val apk = mpvAddonController.download(it) { progress ->
                        reportDownload(progress, 0L, it.apk.size, totalBytes)
                    }
                    DownloadedAddon(apk, it)
                }
                val apk = downloader.download(update.version, update.apk) { progress ->
                    reportDownload(progress, addon?.apk?.size ?: 0L, update.apk.size, totalBytes)
                }
                install(DownloadedUpdate(apk, addonDownload))
            } catch (e: CancellationException) {
                mutableState.update { it.copy(status = AppUpdateStatus.Available) }
                throw e
            } catch (e: AppUpdateException) {
                fail(e.error)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "App update failed" }
                fail(AppUpdateError.InstallFailed(e.message))
            }
        }
    }

    /** Reports one APK's [progress] as part of all the update's downloads, which start [before] bytes in. */
    private fun reportDownload(progress: Float?, before: Long, size: Long, total: Long) {
        val overall = progress?.takeIf { total > 0 }?.let { (before + it * size) / total }
        mutableState.update {
            if (it.status is AppUpdateStatus.Downloading) it.copy(status = AppUpdateStatus.Downloading(overall)) else it
        }
    }

    fun cancelDownload() {
        if (state.value.status is AppUpdateStatus.Downloading) updateJob?.cancel()
    }

    fun installPermissionIntent(): Intent = installer.installPermissionIntent()

    fun openSheet() {
        if (state.value.update != null) mutableState.update { it.copy(sheetOpen = true) }
    }

    /** "Not now": the next automatic check asks again. A download under way keeps going. */
    fun closeSheet() {
        mutableState.update {
            it.copy(
                sheetOpen = false,
                // Waiting for a permission the user just declined to give would install out of the blue later.
                status = if (it.status == AppUpdateStatus.NeedsPermission) AppUpdateStatus.Available else it.status,
            )
        }
    }

    fun skipVersion() {
        state.value.update?.let { preferences.skippedVersion.set(it.version) }
        closeSheet()
    }

    private fun startCheck(mode: CheckMode) {
        scope.launch { check(mode) }
    }

    private suspend fun check(mode: CheckMode) {
        // Never swap the update out from under a download or install.
        if (state.value.status.isBusy) return
        // A newer check, say after a channel switch, replaces a running one.
        checkJob?.cancel()
        val job = scope.launch {
            mutableState.update { it.copy(status = AppUpdateStatus.Checking) }
            try {
                val update = selectUpdate(
                    releases = source.releases(AppUpdateBuild.REPOSITORY),
                    channel = preferences.channel.get(),
                    installedVersion = AppUpdateBuild.versionName,
                    isFoss = AppUpdateBuild.isFoss,
                    primaryAbi = Build.SUPPORTED_ABIS.first(),
                )?.let { it.copy(addon = addonFor(it)) }
                preferences.lastCheckedAt.set(System.currentTimeMillis())
                if (update == null) downloader.clear()
                val openSheet = update != null && when (mode) {
                    CheckMode.Automatic -> update.version != preferences.skippedVersion.get()
                    CheckMode.User -> true
                    CheckMode.Refresh -> false
                }
                mutableState.update {
                    AppUpdateState(
                        status = if (update != null) AppUpdateStatus.Available else AppUpdateStatus.UpToDate,
                        update = update,
                        sheetOpen = (it.sheetOpen && update != null) || openSheet,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "App update check failed" }
                val error = (e as? AppUpdateException)?.error ?: AppUpdateError.GithubUnexpected
                mutableState.update {
                    if (mode == CheckMode.Automatic) {
                        // Nobody asked for an automatic check, so its failure stays quiet.
                        it.copy(status = if (it.update != null) AppUpdateStatus.Available else AppUpdateStatus.Idle)
                    } else {
                        it.copy(status = AppUpdateStatus.Failed(error))
                    }
                }
            }
        }
        checkJob = job
        job.join()
    }

    private suspend fun install(update: DownloadedUpdate) {
        val available = state.value.update ?: return
        try {
            verifier.verify(update.apk, available.apk.sha256, verifier.selfUpdate())
        } catch (e: AppUpdateException) {
            if (e.error == AppUpdateError.Damaged) downloader.clear()
            throw e
        }
        update.addon?.let { mpvAddonController.verify(it.apk, it.release) }
        if (!installer.canInstall()) {
            downloaded = update
            mutableState.update { it.copy(status = AppUpdateStatus.NeedsPermission, sheetOpen = true) }
            return
        }
        downloaded = null
        mutableState.update { it.copy(status = AppUpdateStatus.Installing) }
        if (update.addon != null && mpvAddonController.installVerified(update.addon.apk) == InstallOutcome.Cancelled) {
            mutableState.update { it.copy(status = AppUpdateStatus.Available) }
            return
        }
        when (installer.install(update.apk)) {
            InstallOutcome.Installed -> mutableState.update { it.copy(sheetOpen = false) }
            InstallOutcome.Cancelled -> mutableState.update { it.copy(status = AppUpdateStatus.Available) }
        }
    }

    /**
     * The add-on [update] needs instead of the installed one, or null when the installed one will do or there is none
     * to update. A failed lookup leaves the add-on out rather than holding back Katari's update.
     */
    private suspend fun addonFor(update: AvailableUpdate): MpvAddonRelease? {
        val installed = mpvAddon.read() as? MpvAddonState.Installed ?: return null
        if (installed.problem == MpvAddonProblem.ForeignSigner) return null
        val requirementUrl = update.mpvRequirementUrl ?: return null
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

    private fun onForeground() {
        installer.onForeground()
        val update = downloaded ?: return
        if (state.value.status != AppUpdateStatus.NeedsPermission || !installer.canInstall()) return
        updateJob = scope.launch {
            try {
                install(update)
            } catch (e: AppUpdateException) {
                fail(e.error)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e) { "App update install failed" }
                fail(AppUpdateError.InstallFailed(e.message))
            }
        }
    }

    private fun fail(error: AppUpdateError) {
        logcat(LogPriority.WARN) { "App update failed: $error" }
        mutableState.update { it.copy(status = AppUpdateStatus.Failed(error)) }
    }

    private enum class CheckMode { Automatic, User, Refresh }
}

private val AUTOMATIC_CHECK_INTERVAL = 6.hours

private data class DownloadedUpdate(val apk: File, val addon: DownloadedAddon?)

private data class DownloadedAddon(val apk: File, val release: MpvAddonRelease)

/** A download or install is under way, which a check must not interrupt. */
private val AppUpdateStatus.isBusy: Boolean
    get() = this is AppUpdateStatus.Downloading ||
        this == AppUpdateStatus.NeedsPermission ||
        this == AppUpdateStatus.Installing
