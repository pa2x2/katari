package mihon.feature.migration.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunResult
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunner
import mihon.feature.migration.session.model.SourceMigrationSessionId
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceMigrationDiscoveryWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    private val runner: SourceMigrationDiscoveryRunner = Injekt.get()
    private val notifier = SourceMigrationNotifier(context)

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?.let(::SourceMigrationSessionId)
            ?: return Result.failure()
        var runningInForeground = false
        suspend fun ensureForegroundWhenScreenIsHidden() {
            if (!runningInForeground && !SourceMigrationNotificationVisibility.isVisible(sessionId)) {
                setForegroundSafely()
                runningInForeground = true
            }
        }
        ensureForegroundWhenScreenIsHidden()
        return try {
            when (
                val result = runner.run(sessionId) { completed, total ->
                    ensureForegroundWhenScreenIsHidden()
                    setProgress(workDataOf(KEY_COMPLETED to completed, KEY_TOTAL to total))
                    notifier.showDiscoveryProgress(sessionId, completed, total)
                }
            ) {
                is SourceMigrationDiscoveryRunResult.ReviewRequired -> {
                    notifier.showReviewReady(sessionId, result.completedItems)
                    Result.success()
                }
                SourceMigrationDiscoveryRunResult.Paused,
                SourceMigrationDiscoveryRunResult.NoWork,
                -> {
                    notifier.cancel(sessionId)
                    Result.success()
                }
                SourceMigrationDiscoveryRunResult.SessionMissing -> Result.failure()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error) { "Source Migration discovery failed for ${sessionId.value}" }
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?.let(::SourceMigrationSessionId)
            ?: SourceMigrationSessionId(id.toString())
        return ForegroundInfo(
            notifier.notificationId(sessionId),
            notifier.discoveryProgressNotification(sessionId, 0, 0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"
    }
}
