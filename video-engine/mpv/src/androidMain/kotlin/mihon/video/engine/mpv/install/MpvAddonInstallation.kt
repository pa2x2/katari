package mihon.video.engine.mpv.install

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Downloads and installs the mpv add-on this Katari can use, for the player and the add-on's settings. The app's
 * update feature implements it, since it owns downloading, checking and installing APKs.
 */
interface MpvAddonInstallation {
    val state: StateFlow<MpvAddonInstallState>

    /** Finds the add-on to offer, so its size is known before the user decides. */
    fun lookUp()

    /** Installs or updates to the add-on on offer, looking it up first when there is none yet. */
    fun install()

    /** Stops a download; an install Android has started can't be stopped. */
    fun cancel()

    /** Android's "Install unknown apps" switch for Katari, for [MpvAddonInstallStatus.NeedsPermission]. */
    fun installPermissionIntent(): Intent
}

data class MpvAddonInstallState(
    /** The newest add-on release this Katari can use, once looked up. */
    val offer: MpvAddonOffer? = null,
    val status: MpvAddonInstallStatus = MpvAddonInstallStatus.Idle,
)

data class MpvAddonOffer(
    /** The libmpv release, such as `2026-11-02-1`. */
    val version: String,
    val versionCode: Long,
    val downloadBytes: Long,
)

sealed interface MpvAddonInstallStatus {
    data object Idle : MpvAddonInstallStatus
    data object LookingUp : MpvAddonInstallStatus

    /** [progress] is 0–1, or null while the size is unknown. */
    data class Downloading(val progress: Float?) : MpvAddonInstallStatus

    /** Downloaded, waiting for the user to allow installs from Katari. */
    data object NeedsPermission : MpvAddonInstallStatus
    data object Installing : MpvAddonInstallStatus

    /** Android installed it; [MpvAddon][mihon.video.engine.mpv.addon.MpvAddon] has the new state. */
    data object Installed : MpvAddonInstallStatus
    data class Failed(val error: MpvAddonInstallError) : MpvAddonInstallStatus
}

val MpvAddonInstallStatus.isBusy: Boolean
    get() = this is MpvAddonInstallStatus.Downloading ||
        this == MpvAddonInstallStatus.LookingUp ||
        this == MpvAddonInstallStatus.NeedsPermission ||
        this == MpvAddonInstallStatus.Installing
