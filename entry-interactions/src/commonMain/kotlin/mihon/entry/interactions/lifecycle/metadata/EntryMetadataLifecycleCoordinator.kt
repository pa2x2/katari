package mihon.entry.interactions.lifecycle.metadata

import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import mihon.entry.interactions.lifecycle.attempt
import tachiyomi.domain.entry.model.Entry

internal class EntryMetadataLifecycleCoordinator(
    private val downloads: () -> EntryDownloadMaintenanceFeature,
) : EntryMetadataLifecycleFeature {
    override suspend fun changed(previous: Entry, current: Entry): EntryMetadataChangeResult {
        require(previous.profileId == current.profileId && previous.id == current.id) {
            "Metadata lifecycle cannot change Entry identity"
        }
        require(previous.type == current.type) { "Metadata lifecycle cannot change Entry type" }
        if (previous == current) return EntryMetadataChangeResult.NoChange

        val failures = mutableListOf<EntryLifecycleConsequenceFailure>()
        if (previous.downloadFolderTitle != current.downloadFolderTitle) {
            failures.attempt("entry.download.maintenance.metadata-change") {
                downloads().renameEntry(previous, current.downloadFolderTitle)
            }
        }
        return EntryMetadataChangeResult.Applied(failures)
    }
}
