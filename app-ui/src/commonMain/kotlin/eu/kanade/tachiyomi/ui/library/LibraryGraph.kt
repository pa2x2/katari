package eu.kanade.tachiyomi.ui.library

import app.katari.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.ui.ProfileUnlock
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import tachiyomi.domain.library.service.LibraryUpdateQueue

/** Graph entry points of the library tab and the library update report. */
interface LibraryGraph {
    val profileManager: ProfileManager
    val profileUnlock: ProfileUnlock
    val libraryUpdateQueue: LibraryUpdateQueue
    val webViewDestination: WebViewDestination

    /** Tells whether entries can be deleted from the library. */
    val entryDestructiveRemovalFeature: EntryDestructiveRemovalFeature
}
