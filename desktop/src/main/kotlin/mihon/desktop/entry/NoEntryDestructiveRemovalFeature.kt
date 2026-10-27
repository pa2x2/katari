package mihon.desktop.entry

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import tachiyomi.domain.entry.model.Entry

/**
 * Stands in for deleting entries, which needs the download and merge features desktop does not host yet. Desktop
 * cannot have entries until it loads sources or restores backups, so there is nothing to delete; given entries anyway,
 * it fails before changing anything, so a profile that has them is kept instead of losing them without their
 * downloads and merges being cleaned up. The shared feature replaces this once desktop hosts those features.
 */
@Inject
class NoEntryDestructiveRemovalFeature : EntryDestructiveRemovalFeature {
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
