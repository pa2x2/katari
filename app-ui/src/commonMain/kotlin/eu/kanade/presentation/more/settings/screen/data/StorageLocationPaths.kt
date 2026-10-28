package eu.kanade.presentation.more.settings.screen.data

import tachiyomi.domain.storage.DocumentUri

/**
 * How the storage location, the folder `StoragePreferences.baseStorageDirectory` keeps, relates to the folders the
 * folder picker returns and to the path the settings show for it.
 */
interface StorageLocationPaths {
    /** What the preference keeps for the picked [folder]; null where the folder can't be opened. */
    fun locationOf(folder: DocumentUri): String?

    /**
     * [location] as a path the user recognises; null where it can't be resolved. It can ask the folder's document
     * provider, so call it off the main thread.
     */
    fun displayablePath(location: String): String?
}
