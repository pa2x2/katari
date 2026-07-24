package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryProfileMoveFeature {
    suspend fun preview(request: EntryProfileMoveRequest): EntryProfileMovePreview

    suspend fun execute(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ): EntryProfileMoveResult
}

data class EntryProfileMoveRequest(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val destinationCategoryId: Long?,
    val selectedVisibleEntryIds: List<Long>,
)

data class EntryProfileMoveGroup(
    val entries: List<Entry>,
)

data class EntryProfileMoveConflict(
    val sourceEntry: Entry,
    val destinationEntry: Entry,
    val destinationMergeAffected: Boolean,
)

data class EntryProfileMovePreview(
    val request: EntryProfileMoveRequest,
    val reference: EntryProfileMoveReference,
    val groups: List<EntryProfileMoveGroup>,
    val conflicts: List<EntryProfileMoveConflict>,
)

enum class EntryProfileMoveConflictResolution {
    KEEP_SOURCE,
    OVERWRITE_DESTINATION,
    KEEP_DESTINATION_REMOVE_SOURCE,
}

data class EntryProfileMoveResult(
    val movedSelectedItemCount: Int,
    val skippedSelectedItemCount: Int,
    val overwrittenDuplicateCount: Int,
    val removedSourceDuplicateCount: Int,
    val consequenceFailures: List<EntryLifecycleConsequenceFailure> = emptyList(),
)

interface EntryProfileMoveParticipantReference

interface EntryProfileMoveReference {
    fun participantReference(
        participantId: String,
        type: eu.kanade.tachiyomi.source.entry.EntryType,
    ): EntryProfileMoveParticipantReference?
}
