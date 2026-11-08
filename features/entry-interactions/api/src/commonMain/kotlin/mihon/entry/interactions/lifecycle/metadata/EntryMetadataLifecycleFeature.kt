package mihon.entry.interactions.lifecycle.metadata

import mihon.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
import tachiyomi.domain.entry.model.Entry

interface EntryMetadataLifecycleFeature {
    suspend fun changed(
        previous: Entry,
        current: Entry,
    ): EntryMetadataChangeResult
}

sealed interface EntryMetadataChangeResult {
    data object NoChange : EntryMetadataChangeResult

    data class Applied(
        val failures: List<EntryLifecycleConsequenceFailure>,
    ) : EntryMetadataChangeResult
}
