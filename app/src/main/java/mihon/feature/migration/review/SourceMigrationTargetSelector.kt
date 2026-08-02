package mihon.feature.migration.review

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshIntent
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshResult
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage

internal class SourceMigrationTargetSelector(
    private val sessionStore: SourceMigrationSessionStore,
    private val candidateEntryResolver: SourceMigrationCandidateEntryResolver,
    private val migration: EntryMigrationFeature,
) {
    suspend fun select(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        candidate: SourceMigrationCandidate,
    ): SourceMigrationTargetSelectionResult {
        try {
            val session = sessionStore.get(sessionId)
                ?: return SourceMigrationTargetSelectionResult.Unavailable
            if (session.stage !in EDITABLE_PREPARATION_STAGES) {
                return SourceMigrationTargetSelectionResult.Unavailable
            }
            val item = session.items.firstOrNull { it.sourceEntryId == sourceEntryId }
                ?: return SourceMigrationTargetSelectionResult.Unavailable
            val resolved = candidateEntryResolver.resolve(session, sourceEntryId, candidate)
                ?: return SourceMigrationTargetSelectionResult.Unavailable
            val source = resolved.source
            val target = resolved.target

            when (
                migration.refreshTarget(
                    EntryMigrationTargetRefreshIntent(
                        source = source,
                        target = target,
                        fetchDetails = target.thumbnailUrl == null,
                        fetchChildren = true,
                    ),
                )
            ) {
                EntryMigrationTargetRefreshResult.Refreshed -> Unit
                EntryMigrationTargetRefreshResult.NoChildren,
                EntryMigrationTargetRefreshResult.SourceUnavailable,
                is EntryMigrationTargetRefreshResult.Rejected,
                is EntryMigrationTargetRefreshResult.OperationalFailure,
                -> return SourceMigrationTargetSelectionResult.Rejected
            }

            val refreshedTarget = candidateEntryResolver.reloadTarget(target, session.profileId)
            return when (
                val preparation = migration.prepare(
                    EntryMigrationPrepareIntent(
                        source = source,
                        target = refreshedTarget,
                        operationKey = item.operationKey,
                    ),
                )
            ) {
                is EntryMigrationPreparationResult.Ready -> {
                    val selected = sessionStore.selectTarget(
                        sessionId = sessionId,
                        sourceEntryId = sourceEntryId,
                        target = candidate.copy(matchKind = SourceMigrationMatchKind.MANUAL),
                        targetEntryId = refreshedTarget.id,
                        state = SourceMigrationItemState.READY,
                        availableOptions = preparation.availableOptions,
                    )
                    if (selected) {
                        SourceMigrationTargetSelectionResult.Selected
                    } else {
                        SourceMigrationTargetSelectionResult.Unavailable
                    }
                }
                is EntryMigrationPreparationResult.Rejected,
                is EntryMigrationPreparationResult.OperationalFailure,
                -> SourceMigrationTargetSelectionResult.Rejected
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return SourceMigrationTargetSelectionResult.Rejected
        }
    }

    suspend fun clear(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
    ): SourceMigrationTargetSelectionResult {
        return if (sessionStore.clearTarget(sessionId, sourceEntryId)) {
            SourceMigrationTargetSelectionResult.Cleared
        } else {
            SourceMigrationTargetSelectionResult.Unavailable
        }
    }
}

internal enum class SourceMigrationTargetSelectionResult {
    Selected,
    Cleared,
    Rejected,
    Unavailable,
}

private val EDITABLE_PREPARATION_STAGES = setOf(
    SourceMigrationSessionStage.DISCOVERY_QUEUED,
    SourceMigrationSessionStage.DISCOVERING,
    SourceMigrationSessionStage.DISCOVERY_PAUSED,
    SourceMigrationSessionStage.REVIEW_REQUIRED,
)
