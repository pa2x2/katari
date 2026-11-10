package app.katari.webview.chromium

import app.katari.model.artifacts.api.state.ModelArtifactFailure

/** Where the app's Chromium is between absent and installed. */
sealed interface ChromiumState {
    data object NotInstalled : ChromiumState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ChromiumState

    /** The download is complete and checked; its files are being put in place. */
    data object Unpacking : ChromiumState

    data object Installed : ChromiumState

    /** The last installation stopped at [failure]; nothing is installed, and what was downloaded is kept. */
    data class Failed(val failure: ChromiumFailure) : ChromiumState
}

sealed interface ChromiumFailure {
    data class Download(val failure: ModelArtifactFailure) : ChromiumFailure

    /** The disk has no room for the download and the files it unpacks to together. */
    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) : ChromiumFailure

    /** The app's data folder is on a disk that runs no programs (mounted `noexec`), and Chromium is one. */
    data object FolderRunsNoPrograms : ChromiumFailure

    data class Unpacking(val message: String?) : ChromiumFailure
}
