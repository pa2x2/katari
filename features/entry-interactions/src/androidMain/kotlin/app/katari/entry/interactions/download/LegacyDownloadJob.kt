package app.katari.entry.interactions.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.katari.core.metro.metroGraph

/**
 * Takes the work queued under the per-type download workers that came before [EntryDownloadJob]. It doesn't download
 * anything itself: it starts the shared download queue, which holds what those workers would have downloaded, and
 * completes.
 */
internal class LegacyDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(
    context,
    workerParams,
) {
    override suspend fun doWork(): Result {
        applicationContext.metroGraph<EntryDownloadWorkerGraph>().entryDownloadWorkController.start()
        return Result.success()
    }
}
