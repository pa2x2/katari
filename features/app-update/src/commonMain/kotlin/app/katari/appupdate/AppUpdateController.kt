package app.katari.appupdate

import app.katari.appupdate.check.AvailableUpdate
import app.katari.appupdate.check.GithubReleaseSource
import app.katari.appupdate.check.selectUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.hours

/**
 * The app's one update flow: finds a newer GitHub release of the running app, then downloads, checks and installs it
 * the way [platform] does. The update prompt and the About screen both show [state]; when the user was already told
 * about a version is kept in [AppUpdatePreferences], the rest lasts for the process.
 */
class AppUpdateController(
    private val preferences: AppUpdatePreferences,
    private val source: GithubReleaseSource,
    private val platform: AppUpdatePlatform,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = mutableState.asStateFlow()

    private val launchHandled = AtomicBoolean(false)

    /** [AppUpdatePlatform.elapsedRealtime] of the last automatic check. */
    @Volatile
    private var lastAutomaticCheckAt = 0L
    private var checkJob: Job? = null
    private var updateJob: Job? = null

    /** Whether a checked download waits for [AppUpdatePlatform.canInstall]. */
    @Volatile
    private var waitingToInstall = false

    init {
        platform.foregrounds.onEach { onForeground() }.launchIn(scope)
    }

    /** Runs once per process when the app is first ready for a prompt. */
    fun onLaunch() {
        if (!launchHandled.compareAndSet(false, true)) return
        lastAutomaticCheckAt = platform.elapsedRealtime()
        scope.launch {
            platform.removeInstalledDownloads()
            if (preferences.checkOnLaunch.get()) check(CheckMode.Automatic)
        }
    }

    /**
     * Repeats the automatic check every [AUTOMATIC_CHECK_INTERVAL] until cancelled, because Katari can stay open for
     * days and the launch check alone would rarely run. Callers keep it running only while the app is visible.
     */
    suspend fun recheckPeriodically() {
        while (true) {
            delay(lastAutomaticCheckAt + AUTOMATIC_CHECK_INTERVAL.inWholeMilliseconds - platform.elapsedRealtime())
            lastAutomaticCheckAt = platform.elapsedRealtime()
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
                platform.download(update) { progress ->
                    mutableState.update {
                        if (it.status is AppUpdateStatus.Downloading) {
                            it.copy(status = AppUpdateStatus.Downloading(progress))
                        } else {
                            it
                        }
                    }
                }
                install(update)
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

    fun restart() {
        if (state.value.status == AppUpdateStatus.ReadyToRestart) platform.restart()
    }

    /** The app came back to the front, which is how an install that waited for the user goes on. */
    private fun onForeground() {
        platform.onForeground()
        if (!waitingToInstall || state.value.status != AppUpdateStatus.NeedsPermission || !platform.canInstall()) {
            return
        }
        val update = state.value.update ?: return
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
                    releases = source.releases(platform.repository),
                    tagPrefix = platform.tagPrefix,
                    channel = preferences.channel.get(),
                    installedVersion = platform.installedVersion,
                    selectAsset = platform::selectAsset,
                )
                update?.let { platform.prepare(it) }
                preferences.lastCheckedAt.set(System.currentTimeMillis())
                if (update == null) platform.clearDownloads()
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

    private suspend fun install(update: AvailableUpdate) {
        platform.verify(update)
        if (!platform.canInstall()) {
            waitingToInstall = true
            mutableState.update { it.copy(status = AppUpdateStatus.NeedsPermission, sheetOpen = true) }
            return
        }
        waitingToInstall = false
        mutableState.update { it.copy(status = AppUpdateStatus.Installing) }
        when (platform.install(update)) {
            InstallResult.Installed -> mutableState.update { it.copy(sheetOpen = false) }
            InstallResult.Cancelled -> mutableState.update { it.copy(status = AppUpdateStatus.Available) }
            InstallResult.RestartToFinish -> mutableState.update { it.copy(status = AppUpdateStatus.ReadyToRestart) }
        }
    }

    private fun fail(error: AppUpdateError) {
        logcat(LogPriority.WARN) { "App update failed: $error" }
        mutableState.update { it.copy(status = AppUpdateStatus.Failed(error)) }
    }

    private enum class CheckMode { Automatic, User, Refresh }
}

private val AUTOMATIC_CHECK_INTERVAL = 6.hours

/** A download or install is under way, or done and waiting for a restart, which a check must not replace. */
private val AppUpdateStatus.isBusy: Boolean
    get() = this is AppUpdateStatus.Downloading ||
        this == AppUpdateStatus.NeedsPermission ||
        this == AppUpdateStatus.Installing ||
        this == AppUpdateStatus.ReadyToRestart
