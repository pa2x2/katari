package mihon.feature.migration.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import eu.kanade.tachiyomi.util.system.workManager
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import java.util.concurrent.TimeUnit

class SourceMigrationWorkScheduler(
    private val context: Context,
    private val store: SourceMigrationSessionStore,
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

    private companion object {
        const val TAG_DISCOVERY = "source-migration-discovery"
        const val MINIMUM_BACKOFF_SECONDS = 30L

        fun discoveryWorkName(sessionId: SourceMigrationSessionId) = "$TAG_DISCOVERY-${sessionId.value}"

        fun sessionTag(sessionId: SourceMigrationSessionId) = "source-migration-${sessionId.value}"
    }
}
