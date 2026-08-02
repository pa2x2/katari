package mihon.feature.migration.session

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationDiscoveryFailure
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionDraft
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.feature.migration.session.model.SourceMigrationSessionSummary
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
            draft.groups.forEachIndexed { groupIndex, group ->
                val groupId = group.visibleEntry.id
                source_migration_sessionsQueries.insertGroup(
                    sessionId = sessionId.value,
                    groupId = groupId,
                    position = groupIndex.toLong(),
                    visibleEntryId = group.visibleEntry.id,
                    visibleTitle = group.visibleEntry.displayTitle,
                )
                group.members.forEachIndexed { memberIndex, member ->
                    source_migration_sessionsQueries.insertGroupMember(
                        sessionId = sessionId.value,
                        groupId = groupId,
                        entryId = member.id,
                        position = memberIndex.toLong(),
                        sourceId = member.source,
                        title = member.displayTitle,
                        url = member.url,
                        thumbnailUrl = member.thumbnailUrl,
                        selected = member.id in group.selectedEntryIds,
                    )
                }
            }
            draft.entries.forEachIndexed { index, entry ->
                source_migration_sessionsQueries.insertItem(
                    sessionId = sessionId.value,
                    sourceEntryId = entry.id,
                    position = index.toLong(),
                    sourceId = entry.source,
                    sourceTitle = entry.title,
                    sourceUrl = entry.url,
                    sourceThumbnailUrl = entry.thumbnailUrl,
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
                source_migration_sessionsQueries.groupsBySession(sessionId.value)
            },
            handler.subscribeToList {
                source_migration_sessionsQueries.groupMembersBySession(sessionId.value)
            },
            handler.subscribeToList {
                source_migration_sessionsQueries.itemsBySession(sessionId.value)
            },
        ) { session, targetSourceIds, groups, groupMembers, items ->
            val membersByGroupId = groupMembers.groupBy(
                keySelector = { row -> row.group_id },
                valueTransform = { row -> row.toDomain() },
            )
            session?.toDomain(
                targetSourceIds = targetSourceIds,
                groups = groups.map { group -> group.toDomain(membersByGroupId[group.group_id].orEmpty()) },
                items = items.map { it.toDomain() },
            )
        }
    }

    fun observeActive(profileId: Long): Flow<List<SourceMigrationSessionSummary>> {
        return handler.subscribeToList {
            source_migration_sessionsQueries.activeSessionsByProfile(profileId)
        }.map { sessions -> sessions.map { it.toSummary() } }
    }

    suspend fun get(sessionId: SourceMigrationSessionId): SourceMigrationSession? {
        return handler.await(inTransaction = true) {
            val session = source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?: return@await null
            val targetSourceIds = source_migration_sessionsQueries.targetSourcesBySession(sessionId.value).awaitAsList()
            val groupMembers = source_migration_sessionsQueries.groupMembersBySession(sessionId.value).awaitAsList()
            val membersByGroupId = groupMembers.groupBy { it.group_id }
            val groups = source_migration_sessionsQueries.groupsBySession(sessionId.value)
                .awaitAsList()
                .map { group -> group.toDomain(membersByGroupId[group.group_id].orEmpty().map { it.toDomain() }) }
            val items = source_migration_sessionsQueries.itemsBySession(sessionId.value)
                .awaitAsList()
                .map { it.toDomain() }
            session.toDomain(targetSourceIds, groups, items)
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

    suspend fun queueItemDiscovery(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean {
        val now = clockMillis()
        return handler.await(inTransaction = true) {
            val session = source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?: return@await false
            if (session.stage != SourceMigrationSessionStage.REVIEW_REQUIRED.name) return@await false

            source_migration_sessionsQueries.queueItemDiscovery(
                searchDepth = depth.name,
                updatedAt = now,
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
            val queued = source_migration_sessionsQueries.itemsBySession(sessionId.value)
                .awaitAsList()
                .any { item ->
                    item.source_entry_id == sourceEntryId &&
                        item.state == SourceMigrationItemState.DISCOVERY_QUEUED.name
                }
            if (!queued) return@await false

            source_migration_sessionsQueries.transitionStage(
                newStage = SourceMigrationSessionStage.DISCOVERY_QUEUED.name,
                updatedAt = now,
                completedAt = null,
                sessionId = sessionId.value,
                expectedStage = SourceMigrationSessionStage.REVIEW_REQUIRED.name,
            )
            source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?.stage == SourceMigrationSessionStage.DISCOVERY_QUEUED.name
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
                    targetThumbnailUrl = candidate.targetThumbnailUrl,
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
                targetThumbnailUrl = target?.targetThumbnailUrl,
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
        availableOptions: Set<EntryMigrationOption>,
    ): Boolean {
        require(state == SourceMigrationItemState.READY || state == SourceMigrationItemState.NEEDS_REVIEW)
        require(target.sessionId == sessionId && target.sourceEntryId == sourceEntryId)
        return handler.await(inTransaction = true) {
            val session = source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?: return@await false
            if (session.stage != SourceMigrationSessionStage.REVIEW_REQUIRED.name) return@await false
            source_migration_sessionsQueries.selectItemTarget(
                state = state.name,
                included = state == SourceMigrationItemState.READY,
                targetEntryId = targetEntryId,
                targetSourceId = target.targetSourceId,
                targetTitle = target.targetTitle,
                targetUrl = target.targetUrl,
                targetThumbnailUrl = target.targetThumbnailUrl,
                matchKind = target.matchKind.name,
                availableOptions = availableOptions.encodeOptions(),
                updatedAt = clockMillis(),
                sessionId = sessionId.value,
                sourceEntryId = sourceEntryId,
            )
            source_migration_sessionsQueries.itemsBySession(sessionId.value)
                .awaitAsList()
                .firstOrNull { item -> item.source_entry_id == sourceEntryId }
                ?.let { item -> item.selected_target_entry_id == targetEntryId && item.state == state.name }
                ?: false
        }
    }

    suspend fun recordPlanningConflict(
        sessionId: SourceMigrationSessionId,
        sourceEntryIds: Set<Long>,
        errorCode: String,
    ) {
        if (sourceEntryIds.isEmpty()) return
        val now = clockMillis()
        handler.await(inTransaction = true) {
            sourceEntryIds.forEach { sourceEntryId ->
                source_migration_sessionsQueries.recordItemPlanningConflict(
                    errorCode = errorCode,
                    updatedAt = now,
                    sessionId = sessionId.value,
                    sourceEntryId = sourceEntryId,
                )
            }
        }
    }

    suspend fun queueExecution(
        sessionId: SourceMigrationSessionId,
        plannedSourceEntryIds: Set<Long>,
    ): Boolean {
        if (plannedSourceEntryIds.isEmpty()) return false
        val now = clockMillis()
        return handler.await(inTransaction = true) {
            val session = source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?: return@await false
            if (session.stage != SourceMigrationSessionStage.REVIEW_REQUIRED.name) return@await false
            val currentIds = source_migration_sessionsQueries.itemsBySession(sessionId.value)
                .awaitAsList()
                .filter { item -> item.included && item.state == SourceMigrationItemState.READY.name }
                .mapTo(mutableSetOf()) { item -> item.source_entry_id }
            if (currentIds != plannedSourceEntryIds) return@await false

            plannedSourceEntryIds.forEach { sourceEntryId ->
                source_migration_sessionsQueries.queueItemExecution(now, sessionId.value, sourceEntryId)
            }
            source_migration_sessionsQueries.transitionStage(
                newStage = SourceMigrationSessionStage.EXECUTION_QUEUED.name,
                updatedAt = now,
                completedAt = null,
                sessionId = sessionId.value,
                expectedStage = SourceMigrationSessionStage.REVIEW_REQUIRED.name,
            )
            source_migration_sessionsQueries.sessionById(sessionId.value).awaitAsOneOrNull()
                ?.stage == SourceMigrationSessionStage.EXECUTION_QUEUED.name
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
