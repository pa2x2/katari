package eu.kanade.tachiyomi.data.chapterlist

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.FinishChapterListWalk
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryRepository
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Reads the older parts of every chapter list still arriving in parts, of every profile: sources side by side, one
 * title at a time per source, so no site gets more than one walk at once. WorkManager keeps it across restarts; a
 * walk that fails keeps its reason for the chapter list and is retried with backoff.
 */
@OptIn(ExperimentalAtomicApi::class)
class ChapterListWalkJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var walks: EntryChapterListWalkRepository

    @Inject
    private lateinit var entryRepository: EntryRepository

    @Inject
    private lateinit var finishChapterListWalk: FinishChapterListWalk

    override suspend fun doWork(): Result {
        context.appGraph.inject(this)
        val attempted = mutableSetOf<Long>()
        val failed = AtomicBoolean(false)
        // Walks started while this run goes on are picked up before it ends
        while (true) {
            val pending = walks.getAll().filter { it.entryId !in attempted }
            if (pending.isEmpty()) break
            attempted += pending.map { it.entryId }
            val entries = pending.mapNotNull { walk ->
                entryRepository.getEntryById(walk.entryId, walk.profileId)
                    ?: null.also { walks.finish(walk.entryId) }
            }
            coroutineScope {
                entries.groupBy { it.source }.values.forEach { sourceEntries ->
                    launch {
                        sourceEntries.forEach { entry ->
                            try {
                                finishChapterListWalk(entry)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                logcat(LogPriority.WARN, error) {
                                    "Older chapters of entry ${entry.id} stopped loading"
                                }
                                failed.store(true)
                            }
                        }
                    }
                }
            }
        }
        return when {
            !failed.load() -> Result.success()
            // The walks keep their failure on the chapter list, where the user can retry them
            runAttemptCount >= MAX_ATTEMPTS -> Result.success()
            else -> Result.retry()
        }
    }

    companion object {
        private const val TAG = "ChapterListWalk"
        private const val MAX_ATTEMPTS = 8

        /**
         * Starts reading. [restart] stops a run waiting to retry, as the user's Retry does; otherwise the request
         * runs after any current one, so a walk started while one runs is never missed.
         */
        fun start(context: Context, restart: Boolean) {
            val request = OneTimeWorkRequestBuilder<ChapterListWalkJob>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            val policy = if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE
            context.workManager.enqueueUniqueWork(TAG, policy, request)
        }
    }
}
