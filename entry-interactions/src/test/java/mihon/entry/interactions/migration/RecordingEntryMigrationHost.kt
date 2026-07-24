package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMigrationExecutionHost
import mihon.entry.interactions.host.EntryMigrationExecutionInspectionResult
import mihon.entry.interactions.host.EntryMigrationExecutionProfileHost
import mihon.entry.interactions.host.EntryMigrationHostInspectionResult
import mihon.entry.interactions.host.EntryMigrationHostOperation
import mihon.entry.interactions.host.EntryMigrationHostReplayResult
import mihon.entry.interactions.host.EntryMigrationHostTransition
import mihon.entry.interactions.host.EntryMigrationHostTransitionResult
import mihon.entry.interactions.host.EntryMigrationPreparationHost
import mihon.entry.interactions.host.EntryMigrationPreparationProfileHost
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class RecordingEntryMigrationHost(
    private val source: Entry,
    private val target: Entry,
    val sourceCategories: List<Long> = listOf(3L),
    val sourceHasCustomCover: Boolean = true,
    val sourceChildren: List<EntryChapter> = emptyList(),
    val targetChildren: List<EntryChapter> = emptyList(),
) : EntryMigrationPreparationHost,
    EntryMigrationExecutionHost {
    val transitions = mutableListOf<EntryMigrationHostTransition>()

    override fun profile(profileId: Long): Profile = Profile()

    inner class Profile : EntryMigrationPreparationProfileHost, EntryMigrationExecutionProfileHost {
        override suspend fun inspectPair(sourceEntryId: Long, targetEntryId: Long) =
            EntryMigrationHostInspectionResult.Ready(source, target, sourceCategories, sourceHasCustomCover)

        override suspend fun replay(operation: EntryMigrationHostOperation) = EntryMigrationHostReplayResult.NotApplied

        override suspend fun inspectExecution(sourceEntryId: Long, targetEntryId: Long) =
            EntryMigrationExecutionInspectionResult.Ready(
                source,
                target,
                sourceChildren,
                targetChildren,
                sourceCategories,
                emptyList(),
            )

        override suspend fun applyTransition(
            transition: EntryMigrationHostTransition,
            participateMergeReplacement: (suspend () -> EntryMergeMigrationReplacementResult)?,
        ): EntryMigrationHostTransitionResult {
            transitions += transition
            participateMergeReplacement?.invoke()
            return EntryMigrationHostTransitionResult.Applied(replayed = false, hasPendingConsequences = false)
        }
    }
}
