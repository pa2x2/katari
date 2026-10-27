package eu.kanade.tachiyomi.data.chapterlist

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import java.util.concurrent.TimeUnit

@Inject
@SingleIn(AppScope::class)
class ChapterListWalkWorkScheduler(private val application: Application) : ChapterListWalkScheduler {
    override fun schedule(restart: Boolean) {
        val request = OneTimeWorkRequestBuilder<ChapterListWalkJob>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(TAG)
            .build()
        val policy = if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE
        application.workManager.enqueueUniqueWork(TAG, policy, request)
    }

    private companion object {
        const val TAG = "ChapterListWalk"
    }
}
