package mihon.feature.appupdate

import android.content.Intent
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.check.ReleaseVersion
import mihon.feature.appupdate.check.selectUpdate
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.InstallOutcome
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The app's one update flow: finds a newer GitHub release, downloads its APK and installs it. The prompt in
 * MainActivity and the About screen both show [state]; when the user was already told about a version is kept in
 * [AppUpdatePreferences], the rest lasts for the process.
 */
internal class AppUpdateController(
    private val preferences: AppUpdatePreferences,
    private val source: GithubReleaseSource,
    private val downloader: UpdateApkDownloader,
    private val verifier: UpdateApkVerifier,
    private val installer: AppUpdateInstaller,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    private val launchHandled = AtomicBoolean(false)
    private var checkJob: Job? = null
    private var updateJob: Job? = null

    /** The APK waiting for the install permission. */
    private var downloadedApk: File? = null

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
        scope.launch {
            ReleaseVersion.parse(AppUpdateBuild.versionName)?.let(downloader::removeInstalled)
            if (preferences.checkOnLaunch.get()) check(CheckMode.Launch)
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
                val apk = downloader.download(update.version, update.apk) { progress ->
                    mutableState.update {
                        if (it.status is AppUpdateStatus.Downloading) {
                            it.copy(
                                status = AppUpdateStatus.Downloading(progress),
                            )
                        } else {
                            it
                        }
                    }
                }
                install(apk)
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

    fun cancelDownload() {
        if (state.value.status is AppUpdateStatus.Downloading) updateJob?.cancel()
    }

    fun installPermissionIntent(): Intent = installer.installPermissionIntent()

    fun openSheet() {
        if (state.value.update != null) mutableState.update { it.copy(sheetOpen = true) }
    }

    /** "Not now": the launch check asks again next time. A download under way keeps going. */
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
                )
                preferences.lastCheckedAt.set(System.currentTimeMillis())
                if (update == null) downloader.clear()
                val openSheet = update != null && when (mode) {
                    CheckMode.Launch -> update.version != preferences.skippedVersion.get()
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
                    if (mode == CheckMode.Launch) {
                        // Nobody asked for the launch check, so its failure stays quiet.
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

    private suspend fun install(apk: File) {
        val update = state.value.update ?: return
        try {
            verifier.verify(apk, update.apk.sha256)
        } catch (e: AppUpdateException) {
            if (e.error == AppUpdateError.Damaged) downloader.clear()
            throw e
        }
        if (!installer.canInstall()) {
            downloadedApk = apk
            mutableState.update { it.copy(status = AppUpdateStatus.NeedsPermission, sheetOpen = true) }
            return
        }
        downloadedApk = null
        mutableState.update { it.copy(status = AppUpdateStatus.Installing) }
        when (installer.install(apk)) {
            InstallOutcome.Installed -> mutableState.update { it.copy(sheetOpen = false) }
            InstallOutcome.Cancelled -> mutableState.update { it.copy(status = AppUpdateStatus.Available) }
        }
    }

    private fun onForeground() {
        installer.onForeground()
        val apk = downloadedApk ?: return
        if (state.value.status != AppUpdateStatus.NeedsPermission || !installer.canInstall()) return
        updateJob = scope.launch {
            try {
                install(apk)
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

    private enum class CheckMode { Launch, User, Refresh }
}

/** A download or install is under way, which a check must not interrupt. */
private val AppUpdateStatus.isBusy: Boolean
    get() = this is AppUpdateStatus.Downloading ||
        this == AppUpdateStatus.NeedsPermission ||
        this == AppUpdateStatus.Installing
