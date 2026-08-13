package mihon.feature.migration.discovery

import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshIntent
import mihon.entry.interactions.migration.EntryMigrationTargetRefreshResult
import mihon.feature.migration.discovery.model.SourceMigrationDiscoveredCandidate
import mihon.feature.migration.discovery.model.SourceMigrationDiscoveryRequest
import mihon.feature.migration.discovery.model.SourceMigrationSearchDepth
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailure
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationMatchKind
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class SourceMigrationDiscoveryRunner(
    private val store: SourceMigrationSessionStore,
    private val discovery: SourceMigrationCandidateDiscovery,
    private val entryRepository: EntryRepository,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val migration: EntryMigrationFeature,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun run(
        sessionId: SourceMigrationSessionId,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): SourceMigrationDiscoveryRunResult {
        val session = startOrReload(sessionId) ?: return SourceMigrationDiscoveryRunResult.SessionMissing
        if (session.stage == SourceMigrationSessionStage.DISCOVERY_PAUSED || session.cancellationRequested) {
            pause(sessionId, session.stage)
            return SourceMigrationDiscoveryRunResult.Paused
        }
        if (session.stage != SourceMigrationSessionStage.DISCOVERING) {
            return SourceMigrationDiscoveryRunResult.NoWork
        }

        val items = session.items.filter { it.state in RUNNABLE_ITEM_STATES }
        var completed = session.items.size - items.size
        onProgress(completed, session.items.size)
        for (item in items) {
            val current = store.getRunControl(sessionId) ?: return SourceMigrationDiscoveryRunResult.SessionMissing
            if (current.stage != SourceMigrationSessionStage.DISCOVERING || current.cancellationRequested) {
                pause(sessionId, current.stage)
                return SourceMigrationDiscoveryRunResult.Paused
            }
            discoverItem(session, item)
            completed++
            onProgress(completed, session.items.size)
        }

        val current = store.getRunControl(sessionId) ?: return SourceMigrationDiscoveryRunResult.SessionMissing
        if (current.stage != SourceMigrationSessionStage.DISCOVERING || current.cancellationRequested) {
            pause(sessionId, current.stage)
            return SourceMigrationDiscoveryRunResult.Paused
        }
        store.transitionStage(
            sessionId = sessionId,
            expected = SourceMigrationSessionStage.DISCOVERING,
            new = SourceMigrationSessionStage.REVIEW_REQUIRED,
        )
        return SourceMigrationDiscoveryRunResult.ReviewRequired(completed, session.items.size)
    }

    private suspend fun startOrReload(sessionId: SourceMigrationSessionId): SourceMigrationSession? {
        val session = store.get(sessionId) ?: return null
        if (session.stage == SourceMigrationSessionStage.DISCOVERY_QUEUED) {
            store.transitionStage(
                sessionId = sessionId,
                expected = SourceMigrationSessionStage.DISCOVERY_QUEUED,
                new = SourceMigrationSessionStage.DISCOVERING,
            )
            return store.get(sessionId)
        }
        return session
    }

    private suspend fun pause(sessionId: SourceMigrationSessionId, currentStage: SourceMigrationSessionStage) {
        if (currentStage == SourceMigrationSessionStage.DISCOVERING) {
            store.transitionStage(
                sessionId = sessionId,
                expected = SourceMigrationSessionStage.DISCOVERING,
                new = SourceMigrationSessionStage.DISCOVERY_PAUSED,
            )
        } else if (currentStage == SourceMigrationSessionStage.DISCOVERY_QUEUED) {
            store.transitionStage(
                sessionId = sessionId,
                expected = SourceMigrationSessionStage.DISCOVERY_QUEUED,
                new = SourceMigrationSessionStage.DISCOVERY_PAUSED,
            )
        }
    }

    private suspend fun discoverItem(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
    ) {
        store.markItemSearching(session.id, item.sourceEntryId)
        val source = entryRepository.getEntryById(item.sourceEntryId, session.profileId)
        if (source == null) {
            recordUnresolved(
                session,
                item,
                SourceMigrationItemState.DISCOVERY_FAILED,
                ERROR_SOURCE_ENTRY_MISSING,
            )
            return
        }

        try {
            val result = discovery.discover(
                SourceMigrationDiscoveryRequest(
                    sourceTitle = source.title,
                    entryType = source.type,
                    targetSourceIds = session.targetSourceIds,
                    depth = when (item.searchDepth) {
                        SourceMigrationDiscoveryDepth.STANDARD -> SourceMigrationSearchDepth.STANDARD
                        SourceMigrationDiscoveryDepth.BROAD -> SourceMigrationSearchDepth.BROAD
                    },
                ),
            )
            val discoveredAt = clockMillis()
            val candidates = result.candidates.mapIndexed { index, candidate ->
                candidate.toPersistedCandidate(session.id, item.sourceEntryId, index.toLong(), discoveredAt)
            }
            val failures = result.failures.map { failure ->
                SourceMigrationDiscoveryFailure(
                    sessionId = session.id,
                    sourceEntryId = item.sourceEntryId,
                    targetSourceId = failure.sourceId,
                    reason = failure.reason,
                    retryable = failure.retryable,
                    detail = failure.detail,
                    updatedAt = discoveredAt,
                )
            }
            store.replaceDiscoveryEvidence(session.id, item.sourceEntryId, candidates, failures)

            val selected = result.candidates.firstOrNull()
            if (selected == null) {
                val hasIncompleteSearch = failures.any(SourceMigrationDiscoveryFailure::retryable) ||
                    failures.size == session.targetSourceIds.size
                recordUnresolved(
                    session,
                    item,
                    if (hasIncompleteSearch) {
                        SourceMigrationItemState.DISCOVERY_FAILED
                    } else {
                        SourceMigrationItemState.NO_MATCH
                    },
                    if (hasIncompleteSearch) ERROR_TARGET_SEARCH_INCOMPLETE else null,
                )
                return
            }
            prepareSelectedTarget(session, item, source, selected, candidates.first())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            recordUnresolved(
                session,
                item,
                SourceMigrationItemState.DISCOVERY_FAILED,
                ERROR_DISCOVERY_OPERATION,
                error.message,
            )
        }
    }

    private suspend fun prepareSelectedTarget(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
        source: Entry,
        selected: SourceMigrationDiscoveredCandidate,
        persistedCandidate: SourceMigrationCandidate,
    ) {
        val target = networkToLocalEntry(selected.entry, session.profileId)
        when (
            val refresh = migration.refreshTarget(
                EntryMigrationTargetRefreshIntent(
                    source = source,
                    target = target,
                    fetchDetails = target.thumbnailUrl == null,
                    fetchChildren = true,
                ),
            )
        ) {
            EntryMigrationTargetRefreshResult.Refreshed -> Unit
            EntryMigrationTargetRefreshResult.NoChildren -> {
                recordConflict(session, item, persistedCandidate, target, ERROR_TARGET_HAS_NO_CHILDREN)
                return
            }
            EntryMigrationTargetRefreshResult.SourceUnavailable -> {
                recordUnresolved(
                    session,
                    item,
                    SourceMigrationItemState.DISCOVERY_FAILED,
                    ERROR_TARGET_SOURCE_UNAVAILABLE,
                )
                return
            }
            is EntryMigrationTargetRefreshResult.Rejected -> {
                recordConflict(session, item, persistedCandidate, target, refresh.reason.name)
                return
            }
            is EntryMigrationTargetRefreshResult.OperationalFailure -> {
                recordUnresolved(
                    session,
                    item,
                    SourceMigrationItemState.DISCOVERY_FAILED,
                    ERROR_TARGET_REFRESH,
                    refresh.error.message,
                )
                return
            }
        }

        val refreshedTarget = entryRepository.getEntryById(target.id, session.profileId) ?: target
        when (
            val preparation = migration.prepare(
                EntryMigrationPrepareIntent(
                    source = source,
                    target = refreshedTarget,
                    operationKey = item.operationKey,
                ),
            )
        ) {
            is EntryMigrationPreparationResult.Ready -> {
                val ready = selected.matchKind == SourceMigrationMatchKind.EXACT
                store.recordDiscoveryResult(
                    sessionId = session.id,
                    sourceEntryId = item.sourceEntryId,
                    state = if (ready) SourceMigrationItemState.READY else SourceMigrationItemState.NEEDS_REVIEW,
                    target = persistedCandidate,
                    targetEntryId = refreshedTarget.id,
                    included = ready,
                    availableOptions = preparation.availableOptions,
                )
            }
            is EntryMigrationPreparationResult.Rejected -> {
                recordConflict(session, item, persistedCandidate, refreshedTarget, preparation.reason.name)
            }
            is EntryMigrationPreparationResult.OperationalFailure -> {
                if (preparation.retryable) {
                    recordUnresolved(
                        session,
                        item,
                        SourceMigrationItemState.DISCOVERY_FAILED,
                        ERROR_PAIR_PREPARATION,
                    )
                } else {
                    recordConflict(session, item, persistedCandidate, refreshedTarget, ERROR_PAIR_PREPARATION)
                }
            }
        }
    }

    private suspend fun recordConflict(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
        target: SourceMigrationCandidate,
        targetEntry: Entry,
        errorCode: String,
    ) {
        store.recordDiscoveryResult(
            sessionId = session.id,
            sourceEntryId = item.sourceEntryId,
            state = SourceMigrationItemState.CONFLICT,
            target = target,
            targetEntryId = targetEntry.id,
            included = false,
            availableOptions = emptySet(),
            errorCode = errorCode,
        )
    }

    private suspend fun recordUnresolved(
        session: SourceMigrationSession,
        item: SourceMigrationSessionItem,
        state: SourceMigrationItemState,
        errorCode: String?,
        errorMessage: String? = null,
    ) {
        store.recordDiscoveryResult(
            sessionId = session.id,
            sourceEntryId = item.sourceEntryId,
            state = state,
            target = null,
            targetEntryId = null,
            included = false,
            availableOptions = emptySet(),
            errorCode = errorCode,
            errorMessage = errorMessage,
        )
    }

    private fun SourceMigrationDiscoveredCandidate.toPersistedCandidate(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        rank: Long,
        discoveredAt: Long,
    ): SourceMigrationCandidate {
        return SourceMigrationCandidate(
            sessionId = sessionId,
            sourceEntryId = sourceEntryId,
            targetSourceId = entry.source,
            targetTitle = entry.title,
            targetUrl = entry.url,
            targetThumbnailUrl = entry.thumbnailUrl,
            rank = rank,
            score = score,
            matchKind = matchKind,
            discoveredAt = discoveredAt,
        )
    }

    private companion object {
        val RUNNABLE_ITEM_STATES = setOf(
            SourceMigrationItemState.DISCOVERY_QUEUED,
            SourceMigrationItemState.DISCOVERING,
        )

        const val ERROR_SOURCE_ENTRY_MISSING = "SOURCE_ENTRY_MISSING"
        const val ERROR_TARGET_SEARCH_INCOMPLETE = "TARGET_SEARCH_INCOMPLETE"
        const val ERROR_DISCOVERY_OPERATION = "DISCOVERY_OPERATION_FAILED"
        const val ERROR_TARGET_HAS_NO_CHILDREN = "TARGET_HAS_NO_CHILDREN"
        const val ERROR_TARGET_SOURCE_UNAVAILABLE = "TARGET_SOURCE_UNAVAILABLE"
        const val ERROR_TARGET_REFRESH = "TARGET_REFRESH_FAILED"
        const val ERROR_PAIR_PREPARATION = "PAIR_PREPARATION_FAILED"
    }
}
