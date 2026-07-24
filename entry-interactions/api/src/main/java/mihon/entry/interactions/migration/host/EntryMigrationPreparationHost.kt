package mihon.entry.interactions.host

import tachiyomi.domain.entry.model.Entry

/** Segregated application adapter used only to prepare an explicit-profile Migration pair. */
interface EntryMigrationPreparationHost {
    fun profile(profileId: Long): EntryMigrationPreparationProfileHost
}

interface EntryMigrationPreparationProfileHost {
    suspend fun inspectPair(
        sourceEntryId: Long,
        targetEntryId: Long,
    ): EntryMigrationHostInspectionResult
}

sealed interface EntryMigrationHostInspectionResult {
    data class Ready(
        val source: Entry,
        val target: Entry,
        val sourceCategoryIds: List<Long>,
        val sourceHasCustomCover: Boolean,
    ) : EntryMigrationHostInspectionResult

    data object SourceMissing : EntryMigrationHostInspectionResult
    data object TargetMissing : EntryMigrationHostInspectionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationHostInspectionResult
}
