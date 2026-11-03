package mihon.feature.appupdate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mihon.core.common.platform.PlatformFacility
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.check.GithubAsset

/**
 * What [AppUpdateController] needs from the platform it updates: which releases are the running app's, which file of a
 * release fits this build, and how that file gets downloaded, checked and installed.
 */
@PlatformFacility
interface AppUpdatePlatform {
    /** The GitHub repository the app is released from, as `owner/name`. */
    val repository: String

    /** How the app's release tags start, before the version. */
    val tagPrefix: String

    val installedVersion: String

    /** The file of a release that installs over this build, or null when the release has none for it. */
    fun selectAsset(assets: List<GithubAsset>): GithubAsset?

    /** Milliseconds on a clock that only moves forward, for spacing the automatic checks. */
    fun elapsedRealtime(): Long

    /** Deletes downloads the running app has already been updated past; runs once a process. */
    suspend fun removeInstalledDownloads()

    /** Deletes every download, once a check finds nothing to update to. */
    fun clearDownloads()

    /** Learns what else [update] brings along before it is offered, such as Android's mpv add-on. */
    suspend fun prepare(update: AvailableUpdate)

    /**
     * Downloads [update] and what it brings along. [onProgress] gets the share of all of it downloaded, 0–1, or null
     * while the size is unknown. Throws [AppUpdateException] when it fails.
     */
    suspend fun download(update: AvailableUpdate, onProgress: (Float?) -> Unit)

    /** Throws [AppUpdateException] unless the downloads of [update] are what they should be. */
    suspend fun verify(update: AvailableUpdate)

    /** Whether installing may start now; false waits for the user, such as for Android's install permission. */
    fun canInstall(): Boolean

    suspend fun install(update: AvailableUpdate): InstallResult

    /** Emits each time the app comes back to the front, where an install waiting for the user may go on. */
    val foregrounds: Flow<Unit> get() = emptyFlow()

    /** The app came back to the front, before an install waiting for the user goes on. */
    fun onForeground() {}

    /** Starts the app again on the version it installed, for [InstallResult.RestartToFinish]. */
    fun restart() {}
}

enum class InstallResult {
    /** The new version replaced this one; on Android it ends this process. */
    Installed,

    /** The user turned the install down. */
    Cancelled,

    /** Installed, running once the app starts again. */
    RestartToFinish,
}
