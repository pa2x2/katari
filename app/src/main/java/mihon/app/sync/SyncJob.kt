package mihon.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.katari.sync.client.trigger.SyncBackgroundRun
import mihon.app.di.appGraph
import tachiyomi.domain.work.WorkOutcome

/** Runs [SyncBackgroundRun] for WorkManager, as the periodic sync or the hand-off its input names. */
class SyncJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val run = context.appGraph.syncBackgroundRun

    override suspend fun doWork(): Result {
        val outcome = when (inputData.getString(KIND)) {
            HAND_OFF -> run.handOff()
            else -> run.periodic()
        }
        return when (outcome) {
            WorkOutcome.Done -> Result.success()
            WorkOutcome.RetryLater -> Result.retry()
            WorkOutcome.Failed -> Result.failure()
        }
    }

    companion object {
        const val KIND = "kind"
        const val PERIODIC = "periodic"
        const val HAND_OFF = "handOff"
    }
}
