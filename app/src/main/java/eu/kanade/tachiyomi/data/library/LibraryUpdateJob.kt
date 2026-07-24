package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.visualName
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
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
import mihon.entry.interactions.EntryLibraryUpdateRefreshFeature
import mihon.entry.interactions.EntryLibraryUpdateRefreshRequest
import mihon.entry.interactions.EntryLibraryUpdateRefreshResult
import mihon.entry.interactions.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.EntryUpdateEligibility
import mihon.entry.interactions.EntryUpdateEligibilityFeature
import mihon.entry.interactions.EntryUpdateEligibilityRequest
import mihon.entry.interactions.EntryUpdateSkipReason
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: SourceManager = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val entryUpdateEligibility: EntryUpdateEligibilityFeature = Injekt.get()
    private val getLibraryEntries: GetLibraryEntries = Injekt.get()
    private val entryRepository: EntryRepository = Injekt.get()
    private val fetchInterval: FetchInterval = Injekt.get()
    private val entryLibraryUpdateRefreshFeature: EntryLibraryUpdateRefreshFeature = Injekt.get()
    private val mergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature = Injekt.get()

    private val notifier = LibraryUpdateNotifier(context)

    private var entriesToUpdate: List<Entry> = mutableListOf()
    private var currentFetchWindow: Pair<Long, Long> = Pair(0L, 0L)

    override suspend fun doWork(): Result {
        logcat(LogPriority.INFO) {
            "Starting library update (auto=${
                tags.contains(
                    WORK_NAME_AUTO,
                )
            }, category=${inputData.getLong(KEY_CATEGORY, -1L)}, source=${inputData.getLong(KEY_SOURCE, -1L)}, " +
                "type=${inputData.getString(KEY_ENTRY_TYPE)})"
        }

        if (tags.contains(WORK_NAME_AUTO)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val preferences = Injekt.get<LibraryPreferences>()
                val restrictions = preferences.autoUpdateDeviceRestrictions.get()
                if ((DEVICE_ONLY_ON_WIFI in restrictions) && !context.isConnectedToWifi()) {
                    logcat(LogPriority.INFO) { "Skipping automatic library update because device is not on Wi-Fi" }
                    return Result.retry()
                }
            }

            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                logcat(LogPriority.INFO) {
                    "Retrying automatic library update because a manual update is already running"
                }
                return Result.retry()
            }
        }

        setForegroundSafely()

        libraryPreferences.lastUpdatedTimestamp.set(Instant.now().toEpochMilli())

        val categoryId = inputData.getLong(KEY_CATEGORY, -1L)
        val sourceId = inputData.getLong(KEY_SOURCE, -1L)
        val entryType = inputData.getString(KEY_ENTRY_TYPE)
            ?.let { serialized -> EntryType.entries.find { it.name == serialized } }
        addEntryToQueue(categoryId, sourceId, entryType)

        return withIOContext {
            try {
                updateEntryChapterList()
                logcat(LogPriority.INFO) { "Library update completed" }
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    logcat(LogPriority.INFO) { "Library update cancelled" }
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e) { "Library update failed" }
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
     * Adds entries to be updated.
     *
     * @param categoryId the ID of the category to update, or -1 if no category specified.
     * @param sourceId the ID of the source to update, or -1 if no source specified.
     */
    private suspend fun addEntryToQueue(categoryId: Long, sourceId: Long, entryType: EntryType?) {
        val libraryEntries = getLibraryEntries.await()

        val listToUpdate = if (categoryId != -1L || sourceId != -1L || entryType != null) {
            libraryEntries.filter { it.matchesUpdateScope(categoryId, sourceId, entryType) }
        } else {
            val includedCategories = libraryPreferences.updateCategories.get().map { it.toLong() }
            val excludedCategories = libraryPreferences.updateCategoriesExclude.get().map { it.toLong() }

            libraryEntries.filter {
                val included =
                    includedCategories.isEmpty() || it.categories.intersect(includedCategories.toSet()).isNotEmpty()
                val excluded = it.categories.intersect(excludedCategories.toSet()).isNotEmpty()
                included && !excluded
            }
        }

        val skippedUpdates = mutableListOf<Pair<Entry, String?>>()
        currentFetchWindow = fetchInterval.getWindow(ZonedDateTime.now())
        val fetchWindowUpperBound = currentFetchWindow.second

        val eligibleLibraryEntries = listToUpdate
            .filter {
                when (
                    val eligibility = entryUpdateEligibility.evaluate(
                        EntryUpdateEligibilityRequest(
                            entry = it.entry,
                            totalCount = it.totalCount,
                            unconsumedCount = it.unconsumedCount,
                            hasStarted = it.hasStarted,
                            fetchWindowUpperBound = fetchWindowUpperBound,
                        ),
                    )
                ) {
                    EntryUpdateEligibility.Eligible -> true
                    is EntryUpdateEligibility.Skipped -> {
                        skippedUpdates.add(it.entry to eligibility.reason.toSkippedReasonString(context))
                        false
                    }
                }
            }
            .sortedBy { it.entry.title }

        notifier.showQueueSizeWarningNotificationIfNeeded(eligibleLibraryEntries)

        entriesToUpdate = eligibleLibraryEntries.expandToMemberEntries()
            .sortedBy { it.title }

        logcat(LogPriority.INFO) {
            "Queued ${entriesToUpdate.size} library entries for update (${skippedUpdates.size} skipped)"
        }

        if (skippedUpdates.isNotEmpty()) {
            logcat(LogPriority.INFO) {
                skippedUpdates
                    .groupBy { it.second }
                    .map { (reason, entries) -> "$reason: [${entries.map { it.first.title }.sorted().joinToString()}]" }
                    .joinToString()
            }
        }
    }

    private suspend fun List<LibraryItem>.expandToMemberEntries(): List<Entry> {
        return flatMap { libraryItem ->
            mergeMetadataRefreshFeature.resolveOwners(libraryItem.entry).orderedOwners
        }
            .distinctBy(Entry::id)
    }

    private fun EntryUpdateSkipReason.toSkippedReasonString(context: Context): String {
        return when (this) {
            EntryUpdateSkipReason.NOT_ALWAYS_UPDATE ->
                context.stringResource(MR.strings.skipped_reason_not_always_update)
            EntryUpdateSkipReason.COMPLETED -> context.stringResource(MR.strings.skipped_reason_completed)
            EntryUpdateSkipReason.NOT_CAUGHT_UP -> context.stringResource(MR.strings.skipped_reason_not_caught_up)
            EntryUpdateSkipReason.NOT_STARTED -> context.stringResource(MR.strings.skipped_reason_not_started)
            EntryUpdateSkipReason.OUTSIDE_RELEASE_PERIOD ->
                context.stringResource(MR.strings.skipped_reason_not_in_release_period)
        }
    }

    /**
     * Method that updates entries in [entriesToUpdate]. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each entry it calls [entryLibraryUpdateRefreshFeature] and updates the notification showing the current
     * progress.
     *
     * @return an observable delivering the progress of each update.
     */
    private suspend fun updateEntryChapterList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingEntries = CopyOnWriteArrayList<Entry>()
        val newUpdates = CopyOnWriteArrayList<Pair<Entry, Array<EntryChapter>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Entry, String?>>()
        val refreshSession = entryLibraryUpdateRefreshFeature.newSession()

        logcat(LogPriority.INFO) { "Processing ${entriesToUpdate.size} queued library entries" }

        coroutineScope {
            entriesToUpdate.groupBy { it.source }.values
                .map { entriesInSource ->
                    async {
                        semaphore.withPermit {
                            entriesInSource.forEach { queuedEntry ->
                                ensureActive()

                                val entry = entryRepository.getEntryById(queuedEntry.id)
                                // Don't continue to update if entry is not in library
                                if (entry?.favorite != true) {
                                    return@forEach
                                }

                                withUpdateNotification(
                                    currentlyUpdatingEntries,
                                    progressCount,
                                    entry,
                                ) {
                                    try {
                                        when (
                                            val result = refreshSession.refresh(
                                                EntryLibraryUpdateRefreshRequest(
                                                    entry = entry,
                                                    fetchMetadata = libraryPreferences.autoUpdateMetadata.get(),
                                                    fetchWindowLowerBound = currentFetchWindow.first,
                                                    fetchWindowUpperBound = currentFetchWindow.second,
                                                ),
                                            )
                                        ) {
                                            is EntryLibraryUpdateRefreshResult.Updated -> {
                                                val newChapters = result.newChildren
                                                if (newChapters.isNotEmpty()) {
                                                    libraryPreferences.newUpdatesCount.getAndSet {
                                                        it + newChapters.size
                                                    }

                                                    // Keep the queued entry that contains the new EntryChapters
                                                    newUpdates.add(queuedEntry to newChapters.toTypedArray())
                                                    logcat(LogPriority.INFO) {
                                                        "Library update found ${newChapters.size} new chapter(s) " +
                                                            "for ${queuedEntry.title}"
                                                    }
                                                }
                                            }
                                            EntryLibraryUpdateRefreshResult.SourceUnavailable -> {
                                                recordFailedUpdate(
                                                    failedUpdates = failedUpdates,
                                                    entry = queuedEntry,
                                                    errorMessage = context.stringResource(
                                                        MR.strings.loader_not_implemented_error,
                                                    ),
                                                )
                                            }
                                            EntryLibraryUpdateRefreshResult.NoChildren -> {
                                                recordFailedUpdate(failedUpdates, queuedEntry, errorMessage = null)
                                            }
                                            is EntryLibraryUpdateRefreshResult.OperationalFailure -> {
                                                recordFailedUpdate(
                                                    failedUpdates,
                                                    queuedEntry,
                                                    result.error.message,
                                                    result.error,
                                                )
                                            }
                                        }
                                    } catch (e: Throwable) {
                                        recordFailedUpdate(failedUpdates, queuedEntry, e.message, e)
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()

        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
        }
        refreshSession.complete()

        logcat(LogPriority.INFO) {
            "Library update finished with ${newUpdates.size} updated entr${if (newUpdates.size == 1) "y" else "ies"} and ${failedUpdates.size} failure${if (failedUpdates.size == 1) "" else "s"}"
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.size,
                errorFile.getUriCompat(context),
            )
        }
    }

    private fun recordFailedUpdate(
        failedUpdates: CopyOnWriteArrayList<Pair<Entry, String?>>,
        entry: Entry,
        errorMessage: String?,
        error: Throwable? = null,
    ) {
        failedUpdates.add(entry to errorMessage)
        val sourceName = sourceManager.getDisplayInfo(entry.source).visualName()
        val message = buildString {
            append("Library update failed for ${entry.title} ($sourceName)")
            errorMessage?.let { append(": $it") }
        }
        if (error == null) {
            logcat(LogPriority.ERROR) { message }
        } else {
            logcat(LogPriority.ERROR, error) { message }
        }
    }

    private suspend fun withUpdateNotification(
        updatingEntry: CopyOnWriteArrayList<Entry>,
        completed: AtomicInt,
        entry: Entry,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingEntry.add(entry)
        notifier.showProgressNotification(
            updatingEntry,
            completed.load(),
            entriesToUpdate.size,
        )

        block()

        ensureActive()

        updatingEntry.remove(entry)
        completed.incrementAndFetch()
        notifier.showProgressNotification(
            updatingEntry,
            completed.load(),
            entriesToUpdate.size,
        )
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: List<Pair<Entry, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("katari_update_errors.txt")
                file.bufferedWriter().use { out ->
                    out.write(context.stringResource(MR.strings.library_errors_help, ERROR_LOG_HELP_URL) + "\n\n")
                    // Error file format:
                    // ! Error
                    //   # Source
                    //     - Entry
                    errors.groupBy({ it.second }, { it.first }).forEach { (error, entries) ->
                        out.write("\n! ${error}\n")
                        entries.groupBy { it.source }.forEach { (srcId, entries) ->
                            val sourceName = sourceManager.getDisplayInfo(srcId).visualName()
                            out.write("  # $sourceName\n")
                            entries.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {
        }
        return File("")
    }

    companion object {
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        private const val ERROR_LOG_HELP_URL = "https://mihon.app/docs/guides/troubleshooting/"

        /**
         * Key for category to update.
         */
        private const val KEY_CATEGORY = "category"

        /**
         * Key for source to update.
         */
        private const val KEY_SOURCE = "source"

        /**
         * Key for entry type to update.
         */
        private const val KEY_ENTRY_TYPE = "entry_type"

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.autoUpdateInterval.get()
            if (interval > 0) {
                val restrictions = preferences.autoUpdateDeviceRestrictions.get()
                val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
                val networkRequest = NetworkRequest.Builder().apply {
                    removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    if (DEVICE_ONLY_ON_WIFI in restrictions) {
                        addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    }
                    if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                        addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    }
                }
                    .build()
                val constraints = Constraints.Builder()
                    // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
                    .setRequiredNetworkRequest(networkRequest, networkType)
                    .setRequiresCharging(DEVICE_CHARGING in restrictions)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_AUTO,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }

        fun startNow(
            context: Context,
            category: Category? = null,
            sourceId: Long? = null,
            entryType: EntryType? = null,
        ): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }

            val inputData = workDataOf(
                KEY_CATEGORY to category?.id,
                KEY_SOURCE to sourceId,
                KEY_ENTRY_TYPE to entryType?.name,
            )
            val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setInputData(inputData)
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

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}

internal fun LibraryItem.matchesUpdateScope(
    categoryId: Long,
    sourceId: Long,
    entryType: EntryType?,
): Boolean {
    val matchesCategory = categoryId == -1L || categoryId in categories
    val matchesSource = sourceId == -1L || sourceId in sourceIds
    val matchesType = entryType == null || entry.type == entryType
    return matchesCategory && matchesSource && matchesType
}
