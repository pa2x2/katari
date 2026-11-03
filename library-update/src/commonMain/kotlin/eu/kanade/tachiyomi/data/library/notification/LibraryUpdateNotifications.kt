package eu.kanade.tachiyomi.data.library.notification

import mihon.core.common.platform.PlatformFacility
import tachiyomi.core.common.Constants
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.update.model.LibraryUpdateProgress

/**
 * What a library update or metadata refresh tells outside the app as it runs: its progress, warnings and results, as
 * the platform shows them. One serves one update, which runs for [profileId] in every call; the library shows an
 * update's progress through the queue's state either way.
 */
@PlatformFacility
interface LibraryUpdateNotifications {
    /**
     * Warns when an update checks too much of one source.
     *
     * @param rulesProfileId the profile whose library update rules decided what is checked, so the warning can open
     * them; null for updates the rules don't shape.
     */
    suspend fun showQueueSizeWarningIfNeeded(profileId: Long, items: List<LibraryItem>, rulesProfileId: Long?)

    /** Called from several update coroutines at once. [entries] are the ones being checked. */
    suspend fun showProgress(profileId: Long, entries: List<Entry>, progress: LibraryUpdateProgress)

    /** [nextProfileId] is the profile whose update starts after this one, or null when none waits. */
    suspend fun showNextInQueue(profileId: Long, nextProfileId: Long?)

    suspend fun showMetadataProgress(profileId: Long, entries: List<Entry>, completed: Int, total: Int)

    /** The entries the update found new chapters for, with those chapters. */
    suspend fun showUpdates(profileId: Long, updates: List<Pair<Entry, List<EntryChapter>>>)

    suspend fun showUpdateErrors(profileId: Long, failed: Int)

    fun cancelProgress()

    companion object {
        /** What the warning of [showQueueSizeWarningIfNeeded] links to. */
        const val LARGE_UPDATE_HELP_URL = "${Constants.URL_DOCS}library-updates/#large-updates"
    }
}
