package app.katari.appupdate

import app.katari.appupdate.check.AvailableUpdate

data class AppUpdateState(
    val status: AppUpdateStatus = AppUpdateStatus.Idle,
    /** The update offered, kept through download, install and failures so the prompt can still show it. */
    val update: AvailableUpdate? = null,
    val sheetOpen: Boolean = false,
)

sealed interface AppUpdateStatus {
    /** Nothing checked yet in this process. */
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data object UpToDate : AppUpdateStatus
    data object Available : AppUpdateStatus

    /** [progress] is 0–1, or null while the download size is unknown. */
    data class Downloading(val progress: Float?) : AppUpdateStatus

    /** Downloaded, waiting for the user to allow installs from Katari, which Android asks for. */
    data object NeedsPermission : AppUpdateStatus
    data object Installing : AppUpdateStatus

    /** Installed; the new version runs once the app starts again, as it does on desktop. */
    data object ReadyToRestart : AppUpdateStatus
    data class Failed(val error: AppUpdateError) : AppUpdateStatus
}
