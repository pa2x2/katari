package mihon.desktop.library

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.update.model.LibraryUpdateProgress

/**
 * Desktop posts no notifications yet, so library updates and metadata refreshes tell nothing outside the app. The
 * library still shows an update's progress through the queue's state, and the update report its results.
 */
@Inject
class NoLibraryUpdateNotifications : LibraryUpdateNotifications {
    override suspend fun showQueueSizeWarningIfNeeded(
        profileId: Long,
        items: List<LibraryItem>,
        rulesProfileId: Long?,
    ) =
        Unit

    override suspend fun showProgress(profileId: Long, entries: List<Entry>, progress: LibraryUpdateProgress) = Unit

    override suspend fun showNextInQueue(profileId: Long, nextProfileId: Long?) = Unit

    override suspend fun showMetadataProgress(profileId: Long, entries: List<Entry>, completed: Int, total: Int) = Unit

    override suspend fun showUpdates(profileId: Long, updates: List<Pair<Entry, List<EntryChapter>>>) = Unit

    override suspend fun showUpdateErrors(profileId: Long, failed: Int) = Unit

    override fun cancelProgress() = Unit
}
