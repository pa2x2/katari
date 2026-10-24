package mihon.feature.appupdate.mpvaddon

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.eventFlow
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.ExpectedApk
import mihon.feature.appupdate.install.InstallOutcome
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonPackage
import mihon.video.engine.mpv.addon.MpvAddonProblem
import mihon.video.engine.mpv.addon.MpvAddonState
import mihon.video.engine.mpv.install.MpvAddonInstallError
import mihon.video.engine.mpv.install.MpvAddonInstallState
import mihon.video.engine.mpv.install.MpvAddonInstallStatus
import mihon.video.engine.mpv.install.MpvAddonInstallation
import mihon.video.engine.mpv.install.MpvAddonOffer
import mihon.video.engine.mpv.install.isBusy
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Installs and updates the mpv add-on from the libmpv releases: on request from the player or Settings, and through
 * the add-on update sheet the launch check opens. Katari's own update uses its download, check and install steps when
 * the new Katari needs another add-on.
 *
 * A new add-on only takes effect after Katari restarts when this process has already loaded the libraries, so an
 * update installed then is followed by the restart prompt.
 */
internal class MpvAddonController(
    private val addon: MpvAddon,
    private val source: MpvAddonReleaseSource,
    private val downloader: UpdateApkDownloader,
    private val verifier: UpdateApkVerifier,
    private val installer: AppUpdateInstaller,
    private val preferences: AppUpdatePreferences,
    private val scope: CoroutineScope,
) : MpvAddonInstallation {

    private val mutableState = MutableStateFlow(MpvAddonInstallState())
    override val state: StateFlow<MpvAddonInstallState> = mutableState.asStateFlow()

    private val mutablePrompt = MutableStateFlow(MpvAddonPrompt())
    val prompt: StateFlow<MpvAddonPrompt> = mutablePrompt.asStateFlow()

    /** The release behind [MpvAddonInstallState.offer]. */
    @Volatile
    private var release: MpvAddonRelease? = null
    private var job: Job? = null

    /** The APK waiting for the install permission. */
    private var waitingApk: File? = null

    private val launchHandled = AtomicBoolean(false)

    init {
        ProcessLifecycleOwner.get().lifecycle.eventFlow
            .filter { it == Lifecycle.Event.ON_START }
            .onEach { onForeground() }
            .launchIn(scope + Dispatchers.Main)
    }

    /**
     * With the launch check for Katari's own update: opens the add-on update sheet when the add-on is behind. Runs once
     * per process, so a recreated MainActivity doesn't reopen a sheet closed with "Not now".
     */
    fun onLaunch() {
        if (!launchHandled.compareAndSet(false, true)) return
        if (!CHECKS_AUTOMATICALLY || !preferences.checkOnLaunch.get()) return
        scope.launch {
            try {
                checkForUpdate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nobody asked for this check, so its failure stays quiet.
                logcat(LogPriority.WARN, e) { "mpv add-on update check failed" }
            }
        }
    }

    override fun lookUp() {
        if (state.value.status.isBusy) return
        job = scope.launch { reportingFailure { find() } }
    }

    override fun install() {
        if (state.value.status.isBusy) return
        job = scope.launch {
            reportingFailure {
                val release = release ?: find() ?: return@reportingFailure
                mutableState.update { it.copy(status = MpvAddonInstallStatus.Downloading(0f)) }
                val apk = download(release) { progress ->
                    mutableState.update {
                        if (it.status is MpvAddonInstallStatus.Downloading) {
                            it.copy(status = MpvAddonInstallStatus.Downloading(progress))
                        } else {
                            it
                        }
                    }
                }
                verify(apk, release)
                installOrWait(apk)
            }
        }
    }

    override fun cancel() {
        if (state.value.status is MpvAddonInstallStatus.Downloading) job?.cancel()
    }

    override fun installPermissionIntent(): Intent = installer.installPermissionIntent()

    fun openSheet() {
        if (prompt.value.update != null) mutablePrompt.update { it.copy(sheetOpen = true) }
    }

    /** "Not now": a download under way keeps going. */
    fun closeSheet() {
        mutablePrompt.update { it.copy(sheetOpen = false) }
        mutableState.update {
            if (it.status == MpvAddonInstallStatus.NeedsPermission) it.copy(status = MpvAddonInstallStatus.Idle) else it
        }
    }

    /** Only an optional update can be skipped: a required one is what keeps mpv working. */
    fun skipVersion() {
        val update = prompt.value.update ?: return
        if (!update.required) preferences.skippedMpvAddonVersion.set(update.release.version)
        closeSheet()
    }

    fun closeRestartPrompt() {
        mutablePrompt.update { it.copy(restartOpen = false) }
    }

    /** Downloads [release]'s APK for Katari's own update, which installs it before Katari. */
    suspend fun download(release: MpvAddonRelease, onProgress: (Float?) -> Unit): File =
        downloader.download(release.version, release.apk, onProgress)

    suspend fun verify(apk: File, release: MpvAddonRelease) {
        val installed = (addon.state.value as? MpvAddonState.Installed)?.versionCode ?: 0L
        try {
            verifier.verify(
                apk,
                release.apk.sha256,
                ExpectedApk(
                    packageName = MpvAddonPackage.NAME,
                    // Android refuses to install over a higher versionCode.
                    minVersionCode = maxOf(release.versionCode, installed),
                    intMetaData = MpvAddonPackage.GLUE_META_DATA to release.glue,
                ),
            )
        } catch (e: AppUpdateException) {
            if (e.error == AppUpdateError.Damaged || e.error == AppUpdateError.NotNewer) downloader.clear()
            throw e
        }
    }

    fun canInstall(): Boolean = installer.canInstall()

    /** Installs a verified add-on APK; Katari's own update calls it with the permission already granted. */
    suspend fun installVerified(apk: File): InstallOutcome {
        mutableState.update { it.copy(status = MpvAddonInstallStatus.Installing) }
        val outcome = installer.install(apk)
        when (outcome) {
            InstallOutcome.Installed -> onInstalled()
            InstallOutcome.Cancelled -> mutableState.update { it.copy(status = MpvAddonInstallStatus.Idle) }
        }
        return outcome
    }

    private suspend fun checkForUpdate() {
        val installed = addon.read() as? MpvAddonState.Installed ?: return
        // Android won't install over an add-on signed with another key.
        if (installed.problem == MpvAddonProblem.ForeignSigner) return
        val requirement = addon.requirement
        val found = source.newest(requirement.glue, requirement.minVersionCode, requirement.abi) ?: return
        val required = installed.problem != null
        if (!required && found.versionCode <= installed.versionCode) return
        offer(found)
        val skipped = !required && found.version == preferences.skippedMpvAddonVersion.get()
        mutablePrompt.update { it.copy(update = MpvAddonUpdate(found, required), sheetOpen = !skipped) }
    }

    /** Looks up the add-on to offer, reporting a failure or a missing release in [state]. */
    private suspend fun find(): MpvAddonRelease? {
        mutableState.update { it.copy(status = MpvAddonInstallStatus.LookingUp) }
        val requirement = addon.requirement
        val found = source.newest(requirement.glue, requirement.minVersionCode, requirement.abi)
        if (found == null) {
            mutableState.update { it.copy(status = MpvAddonInstallStatus.Failed(MpvAddonInstallError.NoRelease)) }
            return null
        }
        offer(found)
        mutableState.update { it.copy(status = MpvAddonInstallStatus.Idle) }
        return found
    }

    private fun offer(found: MpvAddonRelease) {
        release = found
        mutableState.update {
            it.copy(offer = MpvAddonOffer(found.version, found.versionCode, found.apk.size))
        }
    }

    private suspend fun installOrWait(apk: File) {
        if (!installer.canInstall()) {
            waitingApk = apk
            mutableState.update { it.copy(status = MpvAddonInstallStatus.NeedsPermission) }
            return
        }
        waitingApk = null
        installVerified(apk)
    }

    private suspend fun onInstalled() {
        downloader.clear()
        addon.read()
        mutableState.update { it.copy(status = MpvAddonInstallStatus.Installed) }
        mutablePrompt.update {
            it.copy(update = null, sheetOpen = false, restartOpen = addon.isLoaded)
        }
    }

    private fun onForeground() {
        installer.onForeground()
        val apk = waitingApk ?: return
        if (state.value.status != MpvAddonInstallStatus.NeedsPermission || !installer.canInstall()) return
        job = scope.launch { reportingFailure { installOrWait(apk) } }
    }

    /** Runs [block], turning its failure into [MpvAddonInstallStatus.Failed]. */
    private suspend fun reportingFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            mutableState.update { it.copy(status = MpvAddonInstallStatus.Idle) }
            throw e
        } catch (e: AppUpdateException) {
            fail(e.error.toInstallError())
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "mpv add-on install failed" }
            fail(MpvAddonInstallError.InstallFailed(e.message))
        }
    }

    private fun fail(error: MpvAddonInstallError) {
        logcat(LogPriority.WARN) { "mpv add-on install failed: $error" }
        mutableState.update { it.copy(status = MpvAddonInstallStatus.Failed(error)) }
    }

    private companion object {
        /**
         * GitHub's add-on is signed with the release key, which a locally built Katari rejects, so debug builds only
         * check by themselves against a local release list.
         */
        val CHECKS_AUTOMATICALLY = !BuildConfig.DEBUG || BuildConfig.MPV_ADDON_RELEASES_URL.isNotEmpty()
    }
}

internal data class MpvAddonPrompt(
    val update: MpvAddonUpdate? = null,
    val sheetOpen: Boolean = false,
    /** An update was installed while this process runs the previous add-on's libraries. */
    val restartOpen: Boolean = false,
)

/** [required]: the installed add-on can't run with this Katari, so there is no skipping it. */
internal data class MpvAddonUpdate(
    val release: MpvAddonRelease,
    val required: Boolean,
)

internal fun AppUpdateError.toInstallError(): MpvAddonInstallError = when (this) {
    AppUpdateError.GithubUnreachable -> MpvAddonInstallError.Unreachable
    AppUpdateError.GithubRateLimited -> MpvAddonInstallError.RateLimited
    is AppUpdateError.GithubStatus -> MpvAddonInstallError.GithubStatus(code)
    AppUpdateError.GithubUnexpected -> MpvAddonInstallError.GithubUnexpected
    is AppUpdateError.DownloadFailed -> MpvAddonInstallError.DownloadFailed(detail)
    // NotNewer: the download isn't the add-on release it claims to be.
    AppUpdateError.Damaged, AppUpdateError.NotNewer -> MpvAddonInstallError.Damaged
    AppUpdateError.SignatureMismatch -> MpvAddonInstallError.SignatureMismatch
    AppUpdateError.InsufficientStorage -> MpvAddonInstallError.InsufficientStorage
    AppUpdateError.Incompatible -> MpvAddonInstallError.Incompatible
    is AppUpdateError.InstallFailed -> MpvAddonInstallError.InstallFailed(detail)
}
