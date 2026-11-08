package app.katari.sourcemigration.work

import app.katari.sourcemigration.execution.SourceMigrationExecutionPlanner
import app.katari.sourcemigration.execution.model.SourceMigrationExecutionPlanResult
import app.katari.sourcemigration.session.SourceMigrationSessionStore
import app.katari.sourcemigration.session.model.SourceMigrationDiscoveryDepth
import app.katari.sourcemigration.session.model.SourceMigrationSessionId
import app.katari.sourcemigration.session.model.SourceMigrationSessionStage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The stages of a Source Migration session: each start moves the session into its queued stage, or leaves it where
 * it is when its stage doesn't allow the start, and only then hands it to the platform's [SourceMigrationWork].
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SourceMigrationStages(
    private val store: SourceMigrationSessionStore,
    private val executionPlanner: SourceMigrationExecutionPlanner,
    private val work: SourceMigrationWork,
) : SourceMigrationWorkScheduler {
    override suspend fun startDiscovery(sessionId: SourceMigrationSessionId): Boolean {
        val session = store.get(sessionId) ?: return false
        when (session.stage) {
            SourceMigrationSessionStage.DRAFT -> {
                if (!store.transitionStage(sessionId, session.stage, SourceMigrationSessionStage.DISCOVERY_QUEUED)) {
                    return false
                }
            }
            SourceMigrationSessionStage.DISCOVERY_PAUSED -> {
                store.clearCancellationRequest(sessionId)
                if (!store.transitionStage(sessionId, session.stage, SourceMigrationSessionStage.DISCOVERY_QUEUED)) {
                    return false
                }
            }
            SourceMigrationSessionStage.DISCOVERY_QUEUED,
            SourceMigrationSessionStage.DISCOVERING,
            -> Unit
            else -> return false
        }

        work.startDiscovery(sessionId)
        return true
    }

    override suspend fun pauseDiscovery(sessionId: SourceMigrationSessionId) {
        store.requestCancellation(sessionId)
        val stage = store.get(sessionId)?.stage
        if (stage == SourceMigrationSessionStage.DISCOVERY_QUEUED ||
            stage == SourceMigrationSessionStage.DISCOVERING
        ) {
            store.transitionStage(sessionId, stage, SourceMigrationSessionStage.DISCOVERY_PAUSED)
        }
        work.stopDiscovery(sessionId)
    }

    override suspend fun discardPreparation(sessionId: SourceMigrationSessionId): Boolean {
        val session = store.get(sessionId) ?: return true
        if (session.stage !in DISCARDABLE_PREPARATION_STAGES) return false

        store.requestCancellation(sessionId)
        work.stopSession(sessionId)
        store.delete(sessionId)
        return true
    }

    override suspend fun restartItemDiscovery(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean {
        if (!store.queueItemDiscovery(sessionId, sourceEntryId, depth)) return false
        return startDiscovery(sessionId)
    }

    override suspend fun restartUnresolvedDiscovery(
        sessionId: SourceMigrationSessionId,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean {
        if (!store.queueUnresolvedDiscovery(sessionId, depth)) return false
        return startDiscovery(sessionId)
    }

    override suspend fun startExecution(sessionId: SourceMigrationSessionId): SourceMigrationExecutionStartResult {
        val session = store.get(sessionId) ?: return SourceMigrationExecutionStartResult.Unavailable
        when (session.stage) {
            SourceMigrationSessionStage.REVIEW_REQUIRED -> {
                when (val plan = executionPlanner.plan(session)) {
                    is SourceMigrationExecutionPlanResult.Ready -> {
                        val plannedIds = plan.items.mapTo(mutableSetOf()) { it.sourceEntryId }
                        if (!store.queueExecution(sessionId, plannedIds)) {
                            return SourceMigrationExecutionStartResult.Unavailable
                        }
                    }
                    is SourceMigrationExecutionPlanResult.Conflicted -> {
                        plan.conflicts.forEach { conflict ->
                            store.recordPlanningConflict(
                                sessionId = sessionId,
                                sourceEntryIds = conflict.sourceEntryIds,
                                errorCode = conflict.reason.name,
                            )
                        }
                        return SourceMigrationExecutionStartResult.Conflicted(plan.conflicts)
                    }
                    SourceMigrationExecutionPlanResult.NoItems -> {
                        return SourceMigrationExecutionStartResult.NoItems
                    }
                }
            }
            SourceMigrationSessionStage.EXECUTION_PAUSED -> {
                store.clearCancellationRequest(sessionId)
                if (!store.transitionStage(sessionId, session.stage, SourceMigrationSessionStage.EXECUTION_QUEUED)) {
                    return SourceMigrationExecutionStartResult.Unavailable
                }
            }
            SourceMigrationSessionStage.EXECUTION_QUEUED,
            SourceMigrationSessionStage.EXECUTING,
            -> Unit
            else -> return SourceMigrationExecutionStartResult.Unavailable
        }

        work.startExecution(sessionId)
        return SourceMigrationExecutionStartResult.Started
    }

    override suspend fun pauseExecution(sessionId: SourceMigrationSessionId) {
        store.requestCancellation(sessionId)
        val stage = store.get(sessionId)?.stage
        if (stage == SourceMigrationSessionStage.EXECUTION_QUEUED ||
            stage == SourceMigrationSessionStage.EXECUTING
        ) {
            store.transitionStage(sessionId, stage, SourceMigrationSessionStage.EXECUTION_PAUSED)
        }
        work.stopExecution(sessionId)
    }

    private companion object {
        val DISCARDABLE_PREPARATION_STAGES = setOf(
            SourceMigrationSessionStage.DRAFT,
            SourceMigrationSessionStage.DISCOVERY_QUEUED,
            SourceMigrationSessionStage.DISCOVERING,
            SourceMigrationSessionStage.DISCOVERY_PAUSED,
            SourceMigrationSessionStage.REVIEW_REQUIRED,
        )
    }
}
