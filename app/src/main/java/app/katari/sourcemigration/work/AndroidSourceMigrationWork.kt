package app.katari.sourcemigration.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import app.katari.sourcemigration.session.model.SourceMigrationSessionId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.system.setBackoffCriteria
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.guava.await

/**
 * A session's discovery and execution as WorkManager work, one unique work of each per session, tagged with the
 * session so all of it can be stopped at once. Stopping work also takes its notification away.
 */
@Inject
@SingleIn(AppScope::class)
class AndroidSourceMigrationWork(private val context: Context) : SourceMigrationWork {
    private val notifier = SourceMigrationNotifier(context)

    override fun startDiscovery(sessionId: SourceMigrationSessionId) {
        val request = OneTimeWorkRequestBuilder<SourceMigrationDiscoveryWorker>()
            .setInputData(workDataOf(SourceMigrationDiscoveryWorker.KEY_SESSION_ID to sessionId.value))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(SourceMigrationWork.BACKOFF)
            .addTag(TAG_DISCOVERY)
            .addTag(sessionTag(sessionId))
            .build()
        context.workManager.enqueueUniqueWork(
            discoveryWorkName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun stopDiscovery(sessionId: SourceMigrationSessionId) {
        context.workManager.cancelUniqueWork(discoveryWorkName(sessionId))
        notifier.cancel(sessionId)
    }

    override fun startExecution(sessionId: SourceMigrationSessionId) {
        val request = OneTimeWorkRequestBuilder<SourceMigrationExecutionWorker>()
            .setInputData(workDataOf(SourceMigrationExecutionWorker.KEY_SESSION_ID to sessionId.value))
            .setBackoffCriteria(SourceMigrationWork.BACKOFF)
            .addTag(TAG_EXECUTION)
            .addTag(sessionTag(sessionId))
            .build()
        context.workManager.enqueueUniqueWork(
            executionWorkName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun stopExecution(sessionId: SourceMigrationSessionId) {
        context.workManager.cancelUniqueWork(executionWorkName(sessionId))
        notifier.cancel(sessionId)
    }

    override suspend fun stopSession(sessionId: SourceMigrationSessionId) {
        context.workManager.cancelAllWorkByTag(sessionTag(sessionId)).result.await()
        notifier.cancel(sessionId)
    }

    private companion object {
        const val TAG_DISCOVERY = "source-migration-discovery"
        const val TAG_EXECUTION = "source-migration-execution"

        fun discoveryWorkName(sessionId: SourceMigrationSessionId) = "$TAG_DISCOVERY-${sessionId.value}"

        fun executionWorkName(sessionId: SourceMigrationSessionId) = "$TAG_EXECUTION-${sessionId.value}"

        fun sessionTag(sessionId: SourceMigrationSessionId) = "source-migration-${sessionId.value}"
    }
}
