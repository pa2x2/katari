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
import mihon.feature.migration.execution.SourceMigrationExecutionRunResult
import mihon.feature.migration.execution.SourceMigrationExecutionRunner
import mihon.feature.migration.session.model.SourceMigrationSessionId
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceMigrationExecutionWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    private val runner: SourceMigrationExecutionRunner = Injekt.get()
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
                    notifier.showExecutionProgress(sessionId, completed, total)
                }
            ) {
                is SourceMigrationExecutionRunResult.Completed -> {
                    notifier.showExecutionComplete(sessionId, result.migratedItems, result.attentionItems)
                    Result.success()
                }
                SourceMigrationExecutionRunResult.Paused,
                SourceMigrationExecutionRunResult.NoWork,
                -> {
                    notifier.cancel(sessionId)
                    Result.success()
                }
                SourceMigrationExecutionRunResult.SessionMissing -> Result.failure()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error) { "Source Migration execution failed for ${sessionId.value}" }
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?.let(::SourceMigrationSessionId)
            ?: SourceMigrationSessionId(id.toString())
        return ForegroundInfo(
            notifier.notificationId(sessionId),
            notifier.executionProgressNotification(sessionId, 0, 0),
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
