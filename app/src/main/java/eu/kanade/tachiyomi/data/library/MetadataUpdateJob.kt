package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.katari.android.di.appGraph
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import tachiyomi.domain.work.WorkOutcome

/** Runs [LibraryMetadataUpdater] in the foreground for the profile [AndroidMetadataUpdateRunner] started it for. */
class MetadataUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var runner: AndroidMetadataUpdateRunner

    @Inject
    private lateinit var updater: LibraryMetadataUpdater

    @Inject
    private lateinit var notifier: LibraryUpdateNotifier

    init {
        context.appGraph.inject(this)
    }

    override suspend fun doWork(): Result {
        setForegroundSafely()
        return when (updater.update(runner.profileIdOf(inputData))) {
            WorkOutcome.Done -> Result.success()
            WorkOutcome.RetryLater -> Result.retry()
            WorkOutcome.Failed -> Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.metadataProgressNotification(runner.profileIdOf(inputData)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }
}
