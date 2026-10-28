package mihon.desktop.entry

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import tachiyomi.domain.entry.model.Entry

/**
 * Stands in for deleting entries, which needs the download and merge features desktop does not host yet. It reports
 * itself not offered, so clearing the database and deleting a profile are hidden; given entries anyway, such as from
 * a restored backup, it fails before changing anything, so they are kept instead of being lost without their
 * downloads and merges being cleaned up. The shared feature replaces this once desktop hosts those features.
 */
@Inject
class NoEntryDestructiveRemovalFeature : EntryDestructiveRemovalFeature {
    override val isOffered: Boolean = false

    override suspend fun remove(entries: List<Entry>): EntryDestructiveRemovalResult =
        if (entries.isEmpty()) {
            EntryDestructiveRemovalResult.NoChange
        } else {
            EntryDestructiveRemovalResult.Failed(
                entries,
                UnsupportedOperationException("Desktop cannot delete entries"),
            )
        }
}
