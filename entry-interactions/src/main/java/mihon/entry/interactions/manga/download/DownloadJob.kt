package mihon.entry.interactions.manga.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mihon.core.metro.metroGraph
import mihon.entry.interactions.download.EntryDownloadWorkerGraph

/**
 * Compatibility worker for Manga work persisted before download execution was unified.
 *
 * WorkManager stores a worker's fully qualified class name. Older installations can therefore
 * still restore this legacy class after an app update, so its class and package names must remain
 * available. It intentionally lives in the root `:entry-interactions` module, which owns the
 * shared WorkManager lifecycle; the actual Manga download implementation remains in
 * `:entry-interactions:manga`. New work runs through [mihon.entry.interactions.download.EntryDownloadJob],
 * while this shim starts that shared runtime and completes the legacy work.
 */
class DownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        applicationContext.metroGraph<EntryDownloadWorkerGraph>().entryDownloadWorkController.start()
        return Result.success()
    }
}
