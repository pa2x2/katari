package eu.kanade.tachiyomi.data.chapterlist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.katari.android.di.appGraph
import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.interactor.WalkPendingChapterLists
import tachiyomi.domain.work.WorkOutcome

/** Runs [WalkPendingChapterLists] for WorkManager, which keeps it across restarts and retries it with backoff. */
class ChapterListWalkJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var walkPendingChapterLists: WalkPendingChapterLists

    override suspend fun doWork(): Result {
        context.appGraph.inject(this)
        return when (walkPendingChapterLists.await(runAttemptCount)) {
            WorkOutcome.Done -> Result.success()
            WorkOutcome.RetryLater -> Result.retry()
            WorkOutcome.Failed -> Result.failure()
        }
    }
}
