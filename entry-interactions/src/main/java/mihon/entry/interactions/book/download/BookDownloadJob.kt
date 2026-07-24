package mihon.entry.interactions.book.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mihon.entry.interactions.EntryDownloadWorkController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Compatibility worker for BOOK work persisted before download execution was unified.
 *
 * WorkManager stores a worker's fully qualified class name. Older installations can therefore
 * still restore this legacy class after an app update, so its class and package names must remain
 * available. It intentionally lives in the root `:entry-interactions` module, which owns the
 * shared WorkManager lifecycle; the actual BOOK download implementation remains in
 * `:entry-interactions:book`. New work runs through [mihon.entry.interactions.EntryDownloadJob],
 * while this shim starts that shared runtime and completes the legacy work.
 */
internal class BookDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(
    context,
    workerParams,
) {
    override suspend fun doWork(): Result {
        Injekt.get<EntryDownloadWorkController>().start()
        return Result.success()
    }
}
