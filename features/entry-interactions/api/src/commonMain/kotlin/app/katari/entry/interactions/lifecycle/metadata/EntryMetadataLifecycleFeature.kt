package app.katari.entry.interactions.lifecycle.metadata

import app.katari.entry.interactions.lifecycle.EntryLifecycleConsequenceFailure
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
