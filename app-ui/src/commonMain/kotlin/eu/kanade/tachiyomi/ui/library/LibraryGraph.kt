package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.browse.BrowseDestination
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.feature.migration.EntryMigrationDestination
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.ProfileUnlock
import tachiyomi.domain.library.service.LibraryUpdateQueue

/** Graph entry points of the library tab and the library update report. */
interface LibraryGraph {
    val profileManager: ProfileManager
    val profileUnlock: ProfileUnlock
    val libraryUpdateQueue: LibraryUpdateQueue
    val browseDestination: BrowseDestination
    val entryMigrationDestination: EntryMigrationDestination
    val webViewDestination: WebViewDestination

    /** Tells whether entries can be deleted from the library. */
    val entryDestructiveRemovalFeature: EntryDestructiveRemovalFeature
}
