package mihon.feature.migration.session

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailure
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionDraft
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.data.DatabaseHandler
import java.util.UUID

class SourceMigrationSessionStore(
    private val handler: DatabaseHandler,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun create(draft: SourceMigrationSessionDraft): SourceMigrationSessionId {
        val sessionId = SourceMigrationSessionId(UUID.randomUUID().toString())
        val now = clockMillis()
        handler.await(inTransaction = true) {
            source_migration_sessionsQueries.insertSession(
                sessionId = sessionId.value,
                profileId = draft.profileId,
                originSourceId = draft.originSourceId,
                stage = SourceMigrationSessionStage.DRAFT.name,
                selectedOptions = draft.selectedOptions.encodeOptions(),
                createdAt = now,
                updatedAt = now,
            )
            draft.targetSourceIds.forEachIndexed { index, sourceId ->
                source_migration_sessionsQueries.insertTargetSource(
                    sessionId = sessionId.value,
                    sourceId = sourceId,
                    position = index.toLong(),
                )
            }
            draft.entries.forEachIndexed { index, entry ->
                source_migration_sessionsQueries.insertItem(
                    sessionId = sessionId.value,
                    sourceEntryId = entry.id,
                    position = index.toLong(),
                    sourceId = entry.source,
                    sourceTitle = entry.title,
                    sourceUrl = entry.url,
                    state = SourceMigrationItemState.DISCOVERY_QUEUED.name,
                    operationKey = UUID.randomUUID().toString(),
                    updatedAt = now,
                )
            }
        }
        return sessionId
    }

    fun observe(sessionId: SourceMigrationSessionId): Flow<SourceMigrationSession?> {
        return combine(
            handler.subscribeToOneOrNull {
                source_migration_sessionsQueries.sessionById(sessionId.value)
            },
            handler.subscribeToList {
                source_migration_sessionsQueries.targetSourcesBySession(sessionId.value)
            },
            handler.subscribeToList {
                source_migration_sessionsQueries.itemsBySession(sessionId.value)
            },
        ) { session, targetSourceIds, items ->
            session?.toDomain(targetSourceIds, items.map { it.toDomain() })
        }
    }

    suspend fun get(sessionId: SourceMigrationSessionId): SourceMigrationSession? {
        return handler.await(inTransaction = true) {
            val session = source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?: return@await null
            val targetSourceIds = source_migration_sessionsQueries.targetSourcesBySession(sessionId.value).awaitAsList()
            val items = source_migration_sessionsQueries.itemsBySession(sessionId.value)
                .awaitAsList()
                .map { it.toDomain() }
            session.toDomain(targetSourceIds, items)
        }
    }

    fun observeCandidates(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
    ): Flow<List<SourceMigrationCandidate>> {
        return handler.subscribeToList {
            source_migration_sessionsQueries.candidatesByItem(sessionId.value, sourceEntryId)
        }.map { candidates -> candidates.map { it.toDomain() } }
    }

    fun observeDiscoveryFailures(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
    ): Flow<List<SourceMigrationDiscoveryFailure>> {
        return handler.subscribeToList {
            source_migration_sessionsQueries.discoveryFailuresByItem(sessionId.value, sourceEntryId)
        }.map { failures -> failures.map { it.toDomain() } }
    }

    suspend fun transitionStage(
        sessionId: SourceMigrationSessionId,
        expected: SourceMigrationSessionStage,
        new: SourceMigrationSessionStage,
    ): Boolean {
        val now = clockMillis()
        return handler.await(inTransaction = true) {
            source_migration_sessionsQueries.transitionStage(
                newStage = new.name,
                updatedAt = now,
                completedAt = now.takeIf { new.isTerminal },
                sessionId = sessionId.value,
                expectedStage = expected.name,
            )
            source_migration_sessionsQueries.sessionById(sessionId.value)
                .awaitAsOneOrNull()
                ?.stage == new.name
        }
    }

    suspend fun recordSessionFailure(
        sessionId: SourceMigrationSessionId,
        errorCode: String,
        errorMessage: String?,
    ) {
        val now = clockMillis()
        handler.await {
            source_migration_sessionsQueries.recordSessionFailure(
                newStage = SourceMigrationSessionStage.FAILED.name,
                errorCode = errorCode,
                errorMessage = errorMessage?.take(MAX_ERROR_MESSAGE_LENGTH),
                updatedAt = now,
                completedAt = now,
                sessionId = sessionId.value,
            )
        }
    }

    suspend fun requestCancellation(sessionId: SourceMigrationSessionId) {
        handler.await {
            source_migration_sessionsQueries.requestCancellation(clockMillis(), sessionId.value)
        }
    }

    suspend fun clearCancellationRequest(sessionId: SourceMigrationSessionId) {
        handler.await {
            source_migration_sessionsQueries.clearCancellationRequest(clockMillis(), sessionId.value)
        }
    }

    suspend fun markItemSearching(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
    ) {
        handler.await {
            source_migration_sessionsQueries.markItemSearching(
                state = SourceMigrationItemState.DISCOVERING.name,
                updatedAt = clockMillis(),
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
        }
    }

    suspend fun replaceDiscoveryEvidence(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        candidates: List<SourceMigrationCandidate>,
        failures: List<SourceMigrationDiscoveryFailure>,
    ) {
        require(candidates.all { it.sessionId == sessionId && it.sourceEntryId == sourceEntryId }) {
            "Migration candidates must belong to the updated session item"
        }
        require(candidates.map(SourceMigrationCandidate::rank).distinct().size == candidates.size) {
            "Migration candidate ranks must be unique"
        }
        require(failures.all { it.sessionId == sessionId && it.sourceEntryId == sourceEntryId }) {
            "Migration discovery failures must belong to the updated session item"
        }
        require(failures.map(SourceMigrationDiscoveryFailure::targetSourceId).distinct().size == failures.size) {
            "Migration discovery can retain only one failure per target source"
        }
        handler.await(inTransaction = true) {
            source_migration_sessionsQueries.clearCandidates(sessionId.value, sourceEntryId)
            source_migration_sessionsQueries.clearDiscoveryFailures(sessionId.value, sourceEntryId)
            candidates.forEach { candidate ->
                source_migration_sessionsQueries.insertCandidate(
                    sessionId = sessionId.value,
                    sourceEntryId = sourceEntryId,
                    targetSourceId = candidate.targetSourceId,
                    targetTitle = candidate.targetTitle,
                    targetUrl = candidate.targetUrl,
                    rank = candidate.rank,
                    score = candidate.score,
                    matchKind = candidate.matchKind.name,
                    discoveredAt = candidate.discoveredAt,
                )
            }
            failures.forEach { failure ->
                source_migration_sessionsQueries.insertDiscoveryFailure(
                    sessionId = sessionId.value,
                    sourceEntryId = sourceEntryId,
                    targetSourceId = failure.targetSourceId,
                    reason = failure.reason.name,
                    retryable = failure.retryable,
                    detail = failure.detail?.take(MAX_ERROR_MESSAGE_LENGTH),
                    updatedAt = failure.updatedAt,
                )
            }
        }
    }

    suspend fun recordDiscoveryResult(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        state: SourceMigrationItemState,
        target: SourceMigrationCandidate?,
        targetEntryId: Long?,
        included: Boolean,
        availableOptions: Set<EntryMigrationOption>,
        errorCode: String? = null,
        errorMessage: String? = null,
    ) {
        require(state in DISCOVERY_RESULT_STATES) { "Invalid discovery result state: $state" }
        require((target != null) == (state in DISCOVERY_TARGET_STATES)) {
            "Resolved Migration discovery states require a target and unresolved states must not retain one"
        }
        require((target != null) == (targetEntryId != null)) {
            "A resolved Migration target requires its materialized Entry identity"
        }
        handler.await {
            source_migration_sessionsQueries.recordItemDiscovery(
                state = state.name,
                included = included && state == SourceMigrationItemState.READY,
                targetEntryId = targetEntryId,
                targetSourceId = target?.targetSourceId,
                targetTitle = target?.targetTitle,
                targetUrl = target?.targetUrl,
                matchKind = target?.matchKind?.name,
                availableOptions = availableOptions.encodeOptions(),
                errorCode = errorCode,
                errorMessage = errorMessage?.take(MAX_ERROR_MESSAGE_LENGTH),
                updatedAt = clockMillis(),
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
        }
    }

    suspend fun setItemIncluded(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        included: Boolean,
    ) {
        handler.await {
            source_migration_sessionsQueries.setItemIncluded(
                included,
                clockMillis(),
                sessionId.value,
                sourceEntryId,
            )
        }
    }

    suspend fun selectTarget(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        target: SourceMigrationCandidate,
        targetEntryId: Long,
        state: SourceMigrationItemState,
    ) {
        require(state == SourceMigrationItemState.READY || state == SourceMigrationItemState.NEEDS_REVIEW)
        require(target.sessionId == sessionId && target.sourceEntryId == sourceEntryId)
        handler.await {
            source_migration_sessionsQueries.selectItemTarget(
                state = state.name,
                included = state == SourceMigrationItemState.READY,
                targetEntryId = targetEntryId,
                targetSourceId = target.targetSourceId,
                targetTitle = target.targetTitle,
                targetUrl = target.targetUrl,
                matchKind = target.matchKind.name,
                updatedAt = clockMillis(),
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
        }
    }

    suspend fun recordExecutionState(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        state: SourceMigrationItemState,
        incrementAttempt: Boolean,
        errorCode: String? = null,
        errorMessage: String? = null,
    ) {
        require(state in EXECUTION_STATES) { "Invalid execution state: $state" }
        handler.await {
            source_migration_sessionsQueries.recordItemExecution(
                state = state.name,
                attemptIncrement = if (incrementAttempt) 1 else 0,
                errorCode = errorCode,
                errorMessage = errorMessage?.take(MAX_ERROR_MESSAGE_LENGTH),
                updatedAt = clockMillis(),
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
        }
    }

    suspend fun delete(sessionId: SourceMigrationSessionId) {
        handler.await { source_migration_sessionsQueries.deleteSession(sessionId.value) }
    }

    private companion object {
        const val MAX_ERROR_MESSAGE_LENGTH = 2_000

        val DISCOVERY_RESULT_STATES = setOf(
            SourceMigrationItemState.READY,
            SourceMigrationItemState.NEEDS_REVIEW,
            SourceMigrationItemState.NO_MATCH,
            SourceMigrationItemState.DISCOVERY_FAILED,
            SourceMigrationItemState.CONFLICT,
        )

        val DISCOVERY_TARGET_STATES = setOf(
            SourceMigrationItemState.READY,
            SourceMigrationItemState.NEEDS_REVIEW,
            SourceMigrationItemState.CONFLICT,
        )

        val EXECUTION_STATES = setOf(
            SourceMigrationItemState.EXECUTION_QUEUED,
            SourceMigrationItemState.EXECUTING,
            SourceMigrationItemState.APPLIED,
            SourceMigrationItemState.APPLIED_INCOMPLETE,
            SourceMigrationItemState.EXECUTION_FAILED,
            SourceMigrationItemState.CONFLICT,
            SourceMigrationItemState.CANCELLED,
        )
    }
}
