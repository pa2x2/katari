package app.katari.entry.interactions.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.model.Entry

/**
 * Application boundary for Entry source-migration workflows.
 *
 * Callers submit user intent and consume feature-owned preparation or execution results. Provider lookup, transfer
 * ordering, persistence, and optional feature consequences are deliberately absent from this contract.
 */
interface EntryMigrationFeature {
    /** Which chapter marks migrating an entry of [type] carries over when the person keeps chapter state. */
    fun childStateTransfer(type: EntryType): MigrationChildStateTransfer

    fun availability(entry: Entry): EntryMigrationAvailability

    fun prepareSelection(entries: List<Entry>): EntryMigrationSelectionResult

    suspend fun refreshTarget(intent: EntryMigrationTargetRefreshIntent): EntryMigrationTargetRefreshResult

    suspend fun reconcileOperation(
        intent: EntryMigrationOperationIntent,
    ): EntryMigrationOperationReconciliationResult

    suspend fun prepare(intent: EntryMigrationPrepareIntent): EntryMigrationPreparationResult

    suspend fun execute(intent: EntryMigrationExecuteIntent): EntryMigrationExecutionResult
}
