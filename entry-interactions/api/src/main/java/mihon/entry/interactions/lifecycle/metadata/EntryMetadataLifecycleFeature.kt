package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryMetadataLifecycleFeature {
    suspend fun changed(
        previous: Entry,
        current: Entry,
    ): EntryMetadataChangeResult
}

data class EntryMetadataChangedEvent(
    val previous: Entry,
    val current: Entry,
)

sealed interface EntryMetadataChangeResult {
    data object NoChange : EntryMetadataChangeResult

    data class Applied(
        val failures: List<EntryLifecycleConsequenceFailure>,
    ) : EntryMetadataChangeResult
}

data class EntryLifecycleConsequenceFailure(
    val participantId: String,
    val ownerId: String,
    val cause: Throwable,
)
