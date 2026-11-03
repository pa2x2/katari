package mihon.entry.interactions.download

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.SettableFuture

/** Holds the WorkManager completion boundary open after queue processing has returned. */
internal class ControlledDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : ListenableWorker(context, params) {
    val result: SettableFuture<Result> = SettableFuture.create()

    override fun startWork() = result
}
