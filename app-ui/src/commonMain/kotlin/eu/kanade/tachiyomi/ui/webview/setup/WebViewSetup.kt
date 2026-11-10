package eu.kanade.tachiyomi.ui.webview.setup

import app.katari.core.common.platform.PlatformFacility
import kotlinx.coroutines.flow.StateFlow

/**
 * What the platform's web view needs from the user before it shows pages. Android's is part of the system and always
 * ready; desktop's runs on a Chromium the app downloads once the user agrees to it.
 */
@PlatformFacility
interface WebViewSetup {
    /** Whether what the web view runs on is the app's to download and remove; false where the system brings it. */
    val isManaged: Boolean

    val state: StateFlow<WebViewSetupState>

    /** Whether anything needed the web view this run while it wasn't set up, so what failed since may be for that. */
    val wasNeeded: StateFlow<Boolean>

    /**
     * Whether the Library tells that a web view was needed: something without a screen of its own, such as a source's
     * request, needed one this run while none was set up, and the user hasn't answered since.
     */
    val isNoticed: StateFlow<Boolean>

    /** Whether the set-up dialog is asked for from outside a screen that shows it itself, such as from the notice. */
    val isRequested: StateFlow<Boolean>

    /** The size of what [start] downloads, and of what that takes on disk once set up. */
    val downloadBytes: Long
    val installedBytes: Long

    /** Asks for the set-up dialog over whatever the window shows, and takes the Library's notice away. */
    fun request()

    fun dismissRequest()

    /** Puts the Library's notice away for this run. */
    fun dismissNotice()

    /** Sets the web view up, which the user agreed to just now. [state] tells how it goes, until [cancel]. */
    fun start()

    fun cancel()

    /** The disk space the web view's own files take now, set up or downloaded in part. */
    suspend fun storedBytes(): Long

    /** Removes what [start] set up, closing the pages the web view shows; the next need asks to set it up again. */
    suspend fun remove()
}

sealed interface WebViewSetupState {
    data object Ready : WebViewSetupState

    data object NotSetUp : WebViewSetupState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : WebViewSetupState

    /** The download is complete; its files are being put in place. */
    data object Unpacking : WebViewSetupState

    data class Failed(val failure: WebViewSetupFailure) : WebViewSetupState
}

sealed interface WebViewSetupFailure {
    /** A download nobody asked for just now wasn't made on a metered network. */
    data object MeteredNetwork : WebViewSetupFailure

    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) : WebViewSetupFailure

    data object DamagedDownload : WebViewSetupFailure

    data class Network(val message: String?) : WebViewSetupFailure

    data class Storage(val message: String?) : WebViewSetupFailure

    /** The app's data folder is on a disk that runs no programs. */
    data object FolderRunsNoPrograms : WebViewSetupFailure
}
