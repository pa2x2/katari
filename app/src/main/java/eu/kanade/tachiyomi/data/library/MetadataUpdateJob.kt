package eu.kanade.tachiyomi.data.library
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.model.LibraryItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class MetadataUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val getLibraryEntries: GetLibraryEntries = Injekt.get()
    private val entryRepository: EntryRepository = Injekt.get()
    private val syncEntryWithSource: SyncEntryWithSource = Injekt.get()

    private val notifier = LibraryUpdateNotifier(context)

    private var entriesToUpdate: List<Entry> = mutableListOf()

    override suspend fun doWork(): Result {
        setForegroundSafely()

        addEntriesToQueue()

        return withIOContext {
            try {
                updateMetadata()
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notifier = LibraryUpdateNotifier(context)
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    /**
     * Adds list of entries to be updated.
     */
    private suspend fun addEntriesToQueue() {
        val libraryItems = getLibraryEntries.await()
        notifier.showQueueSizeWarningNotificationIfNeeded(libraryItems)
        entriesToUpdate = libraryItems.expandToMemberEntries()
    }

    private suspend fun List<LibraryItem>.expandToMemberEntries(): List<Entry> {
        return flatMap { libraryItem ->
            libraryItem.memberEntryIds.mapNotNull { memberKey ->
                if (memberKey.id == libraryItem.entry.id) {
                    libraryItem.entry
                } else {
                    entryRepository.getEntryById(memberKey.id)
                }
            }
        }
            .distinctBy(Entry::id)
    }

    private suspend fun updateMetadata() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingEntries = CopyOnWriteArrayList<Entry>()

        coroutineScope {
            entriesToUpdate.groupBy { it.source }
                .values
                .map { entriesInSource ->
                    async {
                        semaphore.withPermit {
                            entriesInSource.forEach { entry ->
                                ensureActive()

                                withUpdateNotification(
                                    currentlyUpdatingEntries,
                                    progressCount,
                                    entry,
                                ) {
                                    val freshEntry = entryRepository.getEntryById(entry.id)
                                    if (freshEntry?.favorite != true) {
                                        return@withUpdateNotification
                                    }
                                    try {
                                        syncEntryWithSource(
                                            freshEntry,
                                            fetchDetails = true,
                                            fetchChapters = false,
                                        )
                                    } catch (e: Throwable) {
                                        // Ignore errors and continue
                                        logcat(LogPriority.ERROR, e)
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
    }

    private suspend fun withUpdateNotification(
        updatingEntries: CopyOnWriteArrayList<Entry>,
        completed: AtomicInt,
        entry: Entry,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingEntries.add(entry)
        notifier.showProgressNotification(
            updatingEntries,
            completed.load(),
            entriesToUpdate.size,
        )

        block()

        ensureActive()

        updatingEntries.remove(entry)
        completed.fetchAndIncrement()
        notifier.showProgressNotification(
            updatingEntries,
            completed.load(),
            entriesToUpdate.size,
        )
    }

    companion object {
        private const val TAG = "MetadataUpdate"
        private const val WORK_NAME_MANUAL = "MetadataUpdate"

        fun startNow(context: Context): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }
            val request = OneTimeWorkRequestBuilder<MetadataUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .build()
            wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)
                }
        }
    }
}
