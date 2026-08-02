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
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

internal class SourceMigrationTargetSelector(
    private val sessionStore: SourceMigrationSessionStore,
    private val entryRepository: EntryRepository,
    private val networkToLocalEntry: NetworkToLocalEntry,
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
            if (session.stage != SourceMigrationSessionStage.REVIEW_REQUIRED) {
                return SourceMigrationTargetSelectionResult.Unavailable
            }
            val item = session.items.firstOrNull { it.sourceEntryId == sourceEntryId }
                ?: return SourceMigrationTargetSelectionResult.Unavailable
            if (candidate.sessionId != sessionId || candidate.sourceEntryId != sourceEntryId) {
                return SourceMigrationTargetSelectionResult.Unavailable
            }
            val source = entryRepository.getEntryById(sourceEntryId, session.profileId)
                ?: return SourceMigrationTargetSelectionResult.Unavailable
            val target = networkToLocalEntry(
                Entry.create().copy(
                    source = candidate.targetSourceId,
                    url = candidate.targetUrl,
                    title = candidate.targetTitle,
                    thumbnailUrl = candidate.targetThumbnailUrl,
                    type = source.type,
                    profileId = session.profileId,
                ),
                session.profileId,
            )

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

            val refreshedTarget = entryRepository.getEntryById(target.id, session.profileId) ?: target
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
}

internal enum class SourceMigrationTargetSelectionResult {
    Selected,
    Rejected,
    Unavailable,
}
