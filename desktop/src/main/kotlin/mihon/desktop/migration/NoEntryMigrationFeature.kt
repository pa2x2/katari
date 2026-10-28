package mihon.desktop.migration

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationExecuteIntent
import mihon.entry.interactions.migration.EntryMigrationExecutionResult
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationOperationIntent
import mihon.entry.interactions.migration.EntryMigrationOperationReconciliationResult
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.migration.EntryMigrationRejection
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshIntent
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshResult
import tachiyomi.domain.entry.model.Entry

/**
 * Migration as a feature no entry type contributes to, answering as the shared feature does for a source type that
 * can't migrate, so screens don't offer it. Stands in for the shared feature, which desktop can host once it has
 * sources to migrate between and the merge, download and tracking features migration carries state through.
 */
@Inject
class NoEntryMigrationFeature : EntryMigrationFeature {
    private val unsupported = EntryMigrationRejection.UNSUPPORTED_SOURCE_TYPE

    override fun availability(entry: Entry): EntryMigrationAvailability =
        EntryMigrationAvailability.Unavailable(unsupported)

    override fun prepareSelection(entries: List<Entry>): EntryMigrationSelectionResult =
        EntryMigrationSelectionResult.Rejected(
            if (entries.isEmpty()) EntryMigrationRejection.EMPTY_SELECTION else unsupported,
        )

    override suspend fun refreshTarget(intent: EntryMigrationTargetRefreshIntent): EntryMigrationTargetRefreshResult =
        EntryMigrationTargetRefreshResult.Rejected(unsupported)

    override suspend fun reconcileOperation(
        intent: EntryMigrationOperationIntent,
    ): EntryMigrationOperationReconciliationResult = EntryMigrationOperationReconciliationResult.NotApplied

    override suspend fun prepare(intent: EntryMigrationPrepareIntent): EntryMigrationPreparationResult =
        EntryMigrationPreparationResult.Rejected(unsupported)

    override suspend fun execute(intent: EntryMigrationExecuteIntent): EntryMigrationExecutionResult =
        EntryMigrationExecutionResult.Rejected(unsupported)
}
