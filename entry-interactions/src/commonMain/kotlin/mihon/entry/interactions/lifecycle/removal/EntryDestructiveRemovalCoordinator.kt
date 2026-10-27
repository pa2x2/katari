package mihon.entry.interactions.lifecycle.removal

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.maintenance.EntryDownloadRemovalCollector
import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import mihon.entry.interactions.lifecycle.attempt
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCommit
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import tachiyomi.domain.entry.model.Entry

/**
 * Deletes entries. Inside the delete transaction their downloads are collected and they leave their merge groups;
 * custom covers and downloads are removed after the delete commits, and those failures are reported.
 */
internal class EntryDestructiveRemovalCoordinator(
    private val host: EntryDestructiveRemovalHost,
    private val mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    private val downloads: () -> EntryDownloadMaintenanceFeature,
    private val customCoverHost: EntryDestructiveRemovalCustomCoverHost,
) : EntryDestructiveRemovalFeature {
    override suspend fun remove(entries: List<Entry>): EntryDestructiveRemovalResult {
        val requested = entries.distinctBy { it.profileId to it.id }
        if (requested.isEmpty()) return EntryDestructiveRemovalResult.NoChange
        return try {
            val removedDownloads = EntryDownloadRemovalCollector(downloads)
            val commit = host.remove(requested = requested) { persisted ->
                persisted.groupBy(Entry::type).values.forEach { typedEntries ->
                    removedDownloads.prepare(typedEntries)
                    val result = mergeLibraryLifecycle().entriesRemovedFromLibrary(typedEntries)
                    check(result.unresolvedGroupCount == 0) {
                        "Merge membership changed during destructive Entry removal"
                    }
                }
            }
            when (commit) {
                is EntryDestructiveRemovalCommit.Applied -> removed(commit.entries, removedDownloads)
                EntryDestructiveRemovalCommit.NoChange -> EntryDestructiveRemovalResult.NoChange
                EntryDestructiveRemovalCommit.Conflict -> EntryDestructiveRemovalResult.Failed(
                    requested,
                    IllegalStateException("Entries changed before destructive removal"),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryDestructiveRemovalResult.Failed(requested, error)
        }
    }

    private suspend fun removed(
        entries: List<Entry>,
        removedDownloads: EntryDownloadRemovalCollector,
    ): EntryDestructiveRemovalResult.Removed {
        val failures = mutableListOf<EntryLifecycleConsequenceFailure>()
        entries.groupBy(Entry::type).forEach { (type, typedEntries) ->
            failures.attempt("entry.custom-cover.destructive-removal") {
                typedEntries.forEach { entry -> customCoverHost.removeCustomCover(entry) }
            }
            failures.attempt("entry.download.maintenance.destructive-removal") { removedDownloads.apply(type) }
        }
        return EntryDestructiveRemovalResult.Removed(entries, failures)
    }
}
