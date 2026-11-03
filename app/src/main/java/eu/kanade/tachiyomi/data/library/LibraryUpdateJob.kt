package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.library.queue.WorkChainLibraryUpdateQueue
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import mihon.app.di.appGraph

/** One update of the [AndroidLibraryUpdateWorkChain], which runs its [LibraryUpdateRun] in the foreground. */
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var chain: AndroidLibraryUpdateWorkChain

    @Inject
    private lateinit var queue: WorkChainLibraryUpdateQueue

    @Inject
    private lateinit var runFactory: LibraryUpdateRun.Factory

    @Inject
    private lateinit var notifier: LibraryUpdateNotifier

    init {
        context.appGraph.inject(this)
    }

    override suspend fun doWork(): Result {
        chain.runChained(id, inputData, tags) { input ->
            runFactory.create(input) { progress -> setProgress(chain.progressData(progress)) }
                .run(nextInQueue = queue.nextProfileFlow(id.toString()), onScoped = { setForegroundSafely() })
        }
        // A failure would fail every update queued behind this one in the chain.
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.libraryUpdateProgressNotification(chain.profileIdOf(inputData)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }
}
