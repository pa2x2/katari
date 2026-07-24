package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

/**
 * Application boundary for Entry source-migration workflows.
 *
 * Callers submit user intent and consume feature-owned preparation or execution results. Provider lookup, transfer
 * ordering, persistence, and optional feature consequences are deliberately absent from this contract.
 */
interface EntryMigrationFeature {
    fun availability(entry: Entry): EntryMigrationAvailability

    fun prepareSelection(entries: List<Entry>): EntryMigrationSelectionResult

    suspend fun refreshTarget(intent: EntryMigrationTargetRefreshIntent): EntryMigrationTargetRefreshResult

    suspend fun prepare(intent: EntryMigrationPrepareIntent): EntryMigrationPreparationResult

    suspend fun execute(intent: EntryMigrationExecuteIntent): EntryMigrationExecutionResult
}
