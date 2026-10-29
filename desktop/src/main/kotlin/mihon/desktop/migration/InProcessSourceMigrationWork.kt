package mihon.desktop.migration

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunResult
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunner
import mihon.feature.migration.execution.SourceMigrationExecutionRunResult
import mihon.feature.migration.execution.SourceMigrationExecutionRunner
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationWork
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.work.WorkOutcome

/**
 * A session's discovery and execution as [InProcessWork], one of each per session. The review and migration screens
 * show their progress from the session itself, and desktop posts no notifications yet.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessSourceMigrationWork(
    private val work: InProcessWork,
    private val discovery: SourceMigrationDiscoveryRunner,
    private val execution: SourceMigrationExecutionRunner,
) : SourceMigrationWork, ResumableWork {
    override val kinds: Set<String> = setOf(KIND_DISCOVERY, KIND_EXECUTION)

    override fun startDiscovery(sessionId: SourceMigrationSessionId) {
        work.enqueue(
            discoveryWorkName(sessionId),
            ExistingWorkPolicy.Replace,
            InProcessWorkRequest(
                resumeAs = KIND_DISCOVERY,
                input = mapOf(KEY_SESSION_ID to sessionId.value),
                requiresNetwork = true,
                backoff = SourceMigrationWork.BACKOFF,
            ) { discover(sessionId) },
        )
    }

    override fun stopDiscovery(sessionId: SourceMigrationSessionId) {
        work.cancel(discoveryWorkName(sessionId))
    }

    override fun startExecution(sessionId: SourceMigrationSessionId) {
        work.enqueue(
            executionWorkName(sessionId),
            ExistingWorkPolicy.Replace,
            InProcessWorkRequest(
                resumeAs = KIND_EXECUTION,
                input = mapOf(KEY_SESSION_ID to sessionId.value),
                backoff = SourceMigrationWork.BACKOFF,
            ) { execute(sessionId) },
        )
    }

    override fun stopExecution(sessionId: SourceMigrationSessionId) {
        work.cancel(executionWorkName(sessionId))
    }

    override suspend fun stopSession(sessionId: SourceMigrationSessionId) {
        work.cancelAndJoin(discoveryWorkName(sessionId))
        work.cancelAndJoin(executionWorkName(sessionId))
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        val sessionId = input[KEY_SESSION_ID]?.let(::SourceMigrationSessionId) ?: return
        when (kind) {
            KIND_DISCOVERY -> startDiscovery(sessionId)
            KIND_EXECUTION -> startExecution(sessionId)
        }
    }

    private suspend fun discover(sessionId: SourceMigrationSessionId): WorkOutcome = try {
        when (discovery.run(sessionId)) {
            is SourceMigrationDiscoveryRunResult.ReviewRequired,
            SourceMigrationDiscoveryRunResult.Paused,
            SourceMigrationDiscoveryRunResult.NoWork,
            -> WorkOutcome.Done
            SourceMigrationDiscoveryRunResult.SessionMissing -> WorkOutcome.Failed
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        logcat(LogPriority.ERROR, error) { "Source Migration discovery failed for ${sessionId.value}" }
        WorkOutcome.RetryLater
    }

    private suspend fun execute(sessionId: SourceMigrationSessionId): WorkOutcome = try {
        when (execution.run(sessionId)) {
            is SourceMigrationExecutionRunResult.Completed,
            SourceMigrationExecutionRunResult.Paused,
            SourceMigrationExecutionRunResult.NoWork,
            -> WorkOutcome.Done
            SourceMigrationExecutionRunResult.SessionMissing -> WorkOutcome.Failed
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        logcat(LogPriority.ERROR, error) { "Source Migration execution failed for ${sessionId.value}" }
        WorkOutcome.RetryLater
    }

    private companion object {
        const val KIND_DISCOVERY = "source-migration-discovery"
        const val KIND_EXECUTION = "source-migration-execution"
        const val KEY_SESSION_ID = "session_id"

        fun discoveryWorkName(sessionId: SourceMigrationSessionId) = "$KIND_DISCOVERY-${sessionId.value}"

        fun executionWorkName(sessionId: SourceMigrationSessionId) = "$KIND_EXECUTION-${sessionId.value}"
    }
}
