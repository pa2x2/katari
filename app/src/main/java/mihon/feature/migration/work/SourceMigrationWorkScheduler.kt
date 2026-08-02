package mihon.feature.migration.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import eu.kanade.tachiyomi.util.system.workManager
import mihon.feature.migration.execution.SourceMigrationExecutionPlanner
import mihon.feature.migration.execution.model.SourceMigrationExecutionPlanResult
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import java.util.concurrent.TimeUnit

class SourceMigrationWorkScheduler(
    private val context: Context,
    private val store: SourceMigrationSessionStore,
    private val executionPlanner: SourceMigrationExecutionPlanner,
) {
    suspend fun startDiscovery(sessionId: SourceMigrationSessionId): Boolean {
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

        val request = OneTimeWorkRequestBuilder<SourceMigrationDiscoveryWorker>()
            .setInputData(workDataOf(SourceMigrationDiscoveryWorker.KEY_SESSION_ID to sessionId.value))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MINIMUM_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_DISCOVERY)
            .addTag(sessionTag(sessionId))
            .build()
        context.workManager.enqueueUniqueWork(
            discoveryWorkName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return true
    }

    suspend fun pauseDiscovery(sessionId: SourceMigrationSessionId) {
        store.requestCancellation(sessionId)
        val stage = store.get(sessionId)?.stage
        if (stage == SourceMigrationSessionStage.DISCOVERY_QUEUED ||
            stage == SourceMigrationSessionStage.DISCOVERING
        ) {
            store.transitionStage(sessionId, stage, SourceMigrationSessionStage.DISCOVERY_PAUSED)
        }
        context.workManager.cancelUniqueWork(discoveryWorkName(sessionId))
    }

    suspend fun restartItemDiscovery(
        sessionId: SourceMigrationSessionId,
        sourceEntryId: Long,
        depth: SourceMigrationDiscoveryDepth,
    ): Boolean {
        if (!store.queueItemDiscovery(sessionId, sourceEntryId, depth)) return false
        return startDiscovery(sessionId)
    }

    suspend fun startExecution(sessionId: SourceMigrationSessionId): SourceMigrationExecutionStartResult {
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

        val request = OneTimeWorkRequestBuilder<SourceMigrationExecutionWorker>()
            .setInputData(workDataOf(SourceMigrationExecutionWorker.KEY_SESSION_ID to sessionId.value))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MINIMUM_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_EXECUTION)
            .addTag(sessionTag(sessionId))
            .build()
        context.workManager.enqueueUniqueWork(
            executionWorkName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return SourceMigrationExecutionStartResult.Started
    }

    suspend fun pauseExecution(sessionId: SourceMigrationSessionId) {
        store.requestCancellation(sessionId)
        val stage = store.get(sessionId)?.stage
        if (stage == SourceMigrationSessionStage.EXECUTION_QUEUED ||
            stage == SourceMigrationSessionStage.EXECUTING
        ) {
            store.transitionStage(sessionId, stage, SourceMigrationSessionStage.EXECUTION_PAUSED)
        }
        context.workManager.cancelUniqueWork(executionWorkName(sessionId))
    }

    private companion object {
        const val TAG_DISCOVERY = "source-migration-discovery"
        const val TAG_EXECUTION = "source-migration-execution"
        const val MINIMUM_BACKOFF_SECONDS = 30L

        fun discoveryWorkName(sessionId: SourceMigrationSessionId) = "$TAG_DISCOVERY-${sessionId.value}"

        fun executionWorkName(sessionId: SourceMigrationSessionId) = "$TAG_EXECUTION-${sessionId.value}"

        fun sessionTag(sessionId: SourceMigrationSessionId) = "source-migration-${sessionId.value}"
    }
}
