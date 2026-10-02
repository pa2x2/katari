package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshFeature
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshRequest
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshResult
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import mihon.feature.library.update.planning.LibraryUpdateDecision
import mihon.feature.library.update.planning.LibraryUpdatePlanner
import mihon.feature.library.update.planning.LibraryUpdatePlanningContext
import mihon.feature.library.update.planning.LibraryUpdateRequest
import mihon.feature.library.update.planning.LibraryUpdateSettings
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class)
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: SourceManager = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val settingsReader = LibraryUpdateSettings.Reader(Injekt.get(), Injekt.get())
    private val planner = LibraryUpdatePlanner(Injekt.get())
    private val reportRepository: LibraryUpdateReportRepository = Injekt.get()
    private val getLibraryEntries: GetLibraryEntries = Injekt.get()
    private val entryRepository: EntryRepository = Injekt.get()
    private val fetchInterval: FetchInterval = Injekt.get()
    private val entryLibraryUpdateRefreshFeature: EntryLibraryUpdateRefreshFeature = Injekt.get()
    private val mergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature = Injekt.get()

    private val notifier = LibraryUpdateNotifier(context)

    private val selectionFile = LibraryUpdateSelectionFile(context)

    private var entriesToUpdate: List<Entry> = mutableListOf()
    private var updateScope = LibraryUpdateScope.Library
    private var skippedCount = 0
    private var currentFetchWindow: Pair<Long, Long> = Pair(0L, 0L)

    /** Start of this update, which also identifies it in the update report. */
    private var startedAt = 0L

    override suspend fun doWork(): Result {
        val automatic = tags.contains(WORK_NAME_AUTO)
        logcat(LogPriority.INFO) { "Starting library update (auto=$automatic, input=${inputData.keyValueMap})" }

        if (automatic) {
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

        val selectionName = inputData.getString(KEY_SELECTION)
        val request = readRequest(automatic, selectionName)
        updateScope = when (request) {
            is LibraryUpdateRequest.Selection -> LibraryUpdateScope.Selection
            is LibraryUpdateRequest.FollowRules -> LibraryUpdateScope.of(
                categoryId = request.categoryId,
                sourceId = request.sourceId,
                entryType = request.entryType,
            )
        }
        publishProgress(completed = 0)

        setForegroundSafely()

        startedAt = Clock.System.now().toEpochMilliseconds()
        libraryPreferences.lastUpdatedTimestamp.set(startedAt)

        return withIOContext {
            try {
                planQueue(request)
                publishProgress(completed = 0)
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
                withNonCancellableContext {
                    reportRepository.finishRun(startedAt, Clock.System.now().toEpochMilliseconds())
                }
                selectionName?.let(selectionFile::delete)
                notifier.cancelProgressNotification()
            }
        }
    }

    private suspend fun readRequest(automatic: Boolean, selectionName: String?): LibraryUpdateRequest {
        if (selectionName != null) {
            return LibraryUpdateRequest.Selection(selectionFile.read(selectionName))
        }
        if (inputData.getBoolean(KEY_SKIPPED_OF_LATEST, false)) {
            val latestRun = reportRepository.getLatestRun()
            val skipped = reportRepository.getStatuses().values
                .filter { it.decidedAt == latestRun?.startedAt && it.outcome == EntryUpdateOutcome.SKIPPED }
                .mapTo(mutableSetOf(), EntryUpdateStatus::entryId)
            return LibraryUpdateRequest.Selection(skipped)
        }
        return LibraryUpdateRequest.FollowRules(
            automatic = automatic,
            categoryId = inputData.getLong(KEY_CATEGORY, -1L).takeIf { it != -1L },
            sourceId = inputData.getLong(KEY_SOURCE, -1L).takeIf { it != -1L },
            entryType = inputData.getString(KEY_ENTRY_TYPE)
                ?.let { serialized -> EntryType.entries.find { it.name == serialized } },
        )
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

    private suspend fun planQueue(request: LibraryUpdateRequest) {
        val settings = settingsReader.read()
        val timeZone = TimeZone.currentSystemDefault()
        currentFetchWindow = fetchInterval.getWindow(
            Clock.System.now().toLocalDateTime(timeZone).date,
            timeZone,
        )
        val lastCheckedAt = reportRepository.getStatuses().values
            .mapNotNull { status -> status.lastCheckedAt?.let { status.entryId to it } }
            .toMap()
        val decisions = planner.plan(
            request = request,
            items = getLibraryEntries.await(),
            settings = settings,
            context = LibraryUpdatePlanningContext(
                now = startedAt,
                fetchWindowUpperBound = currentFetchWindow.second,
                lastCheckedAt = lastCheckedAt,
            ),
        )
        val checks = decisions.filterIsInstance<LibraryUpdateDecision.Check>()
        val left = decisions.filterIsInstance<LibraryUpdateDecision.Leave>()
        skippedCount = left.count { it.reason.outcome == EntryUpdateOutcome.SKIPPED }

        reportRepository.startRun(
            startedAt = startedAt,
            trigger = when {
                request is LibraryUpdateRequest.Selection -> LibraryUpdateTrigger.SELECTION
                request is LibraryUpdateRequest.FollowRules && request.automatic -> LibraryUpdateTrigger.AUTOMATIC
                else -> LibraryUpdateTrigger.MANUAL
            },
            librarySize = decisions.size,
        )
        reportRepository.recordDecisions(
            decidedAt = startedAt,
            decisions = left.map {
                LibraryUpdateReportRepository.Decision(
                    entryId = it.item.entry.id,
                    reason = it.reason,
                    reasonCategoryId = it.categoryId,
                )
            },
        )

        notifier.showQueueSizeWarningNotificationIfNeeded(checks.map { it.item })

        entriesToUpdate = checks.expandToMemberEntries()
            .sortedBy { it.title }

        logcat(LogPriority.INFO) {
            "Queued ${entriesToUpdate.size} library entries for update (${left.size} left out)"
        }
        if (left.isNotEmpty()) {
            logcat(LogPriority.INFO) {
                left
                    .groupBy { it.reason }
                    .map { (reason, entries) -> "$reason: [${entries.map { it.item.title }.sorted().joinToString()}]" }
                    .joinToString()
            }
        }
    }

    private suspend fun List<LibraryUpdateDecision.Check>.expandToMemberEntries(): List<Entry> {
        return flatMap { check ->
            mergeMetadataRefreshFeature.resolveOwners(check.item.entry).orderedOwners
                .filter { it.source !in check.skippedSourceIds }
        }
            .distinctBy(Entry::id)
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
                                                reportRepository.recordChecked(
                                                    entryId = queuedEntry.id,
                                                    decidedAt = startedAt,
                                                    checkedAt = Clock.System.now().toEpochMilliseconds(),
                                                    newChapters = newChapters.size,
                                                )
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

    private suspend fun recordFailedUpdate(
        failedUpdates: CopyOnWriteArrayList<Pair<Entry, String?>>,
        entry: Entry,
        errorMessage: String?,
        error: Throwable? = null,
    ) {
        failedUpdates.add(entry to errorMessage)
        reportRepository.recordFailed(entry.id, decidedAt = startedAt, error = errorMessage)
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
        val completedCount = completed.incrementAndFetch()
        notifier.showProgressNotification(
            updatingEntry,
            completedCount,
            entriesToUpdate.size,
        )
        publishProgress(completedCount)
    }

    /** Makes the progress readable outside the notification; see [progressFlow]. */
    private suspend fun publishProgress(completed: Int) {
        setProgress(
            workDataOf(
                KEY_PROGRESS_SCOPE to updateScope.name,
                KEY_PROGRESS_COMPLETED to completed,
                KEY_PROGRESS_TOTAL to entriesToUpdate.size,
                KEY_PROGRESS_SKIPPED to skippedCount,
            ),
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

        private const val KEY_PROGRESS_SCOPE = "progress_scope"
        private const val KEY_PROGRESS_COMPLETED = "progress_completed"
        private const val KEY_PROGRESS_TOTAL = "progress_total"
        private const val KEY_PROGRESS_SKIPPED = "progress_skipped"

        /** Name of the [LibraryUpdateSelectionFile] listing the entries to check. */
        private const val KEY_SELECTION = "selection"

        /** Checks the entries the latest update skipped by rules. */
        private const val KEY_SKIPPED_OF_LATEST = "skipped_of_latest"

        /**
         * Progress of the running library update, or null when none runs. An update shows up once it has decided
         * to go ahead, so an automatic run that is only going to be retried later never appears.
         */
        fun progressFlow(context: Context): Flow<LibraryUpdateProgress?> {
            return context.workManager.getWorkInfosByTagFlow(TAG)
                .map { workInfos ->
                    workInfos
                        .filter { it.state == WorkInfo.State.RUNNING }
                        .firstNotNullOfOrNull { it.progress.toLibraryUpdateProgress() }
                }
                .distinctUntilChanged()
        }

        /** When the next automatic update is due to start, or null when none is scheduled. */
        fun nextAutomaticRunFlow(context: Context): Flow<Long?> {
            return context.workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_AUTO)
                .map { workInfos ->
                    workInfos
                        .firstOrNull { !it.state.isFinished }
                        ?.nextScheduleTimeMillis
                        ?.takeIf { it != Long.MAX_VALUE }
                }
                .distinctUntilChanged()
        }

        private fun Data.toLibraryUpdateProgress(): LibraryUpdateProgress? {
            val scope = getString(KEY_PROGRESS_SCOPE)
                ?.let { name -> LibraryUpdateScope.entries.find { it.name == name } }
                ?: return null
            return LibraryUpdateProgress(
                scope = scope,
                completed = getInt(KEY_PROGRESS_COMPLETED, 0),
                total = getInt(KEY_PROGRESS_TOTAL, 0),
                skipped = getInt(KEY_PROGRESS_SKIPPED, 0),
            )
        }

        /**
         * Schedules automatic updates at the shortest interval the library or any category asks for.
         *
         * @param prefInterval the library interval when its preference is about to change and doesn't hold it yet.
         */
        suspend fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = LibraryUpdateSettings.schedulePeriodHours(
                intervalHours = prefInterval ?: preferences.autoUpdateInterval.get(),
                categoryRules = Injekt.get<LibraryUpdateRulesRepository>().getCategoryRules().values,
            )
            if (interval != null) {
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

        /** Checks what the update rules allow, limited to one page of the library when a scope is given. */
        suspend fun startNow(
            context: Context,
            category: Category? = null,
            sourceId: Long? = null,
            entryType: EntryType? = null,
        ): Boolean {
            return enqueueManual(
                context,
                workDataOf(
                    KEY_CATEGORY to category?.id,
                    KEY_SOURCE to sourceId,
                    KEY_ENTRY_TYPE to entryType?.name,
                ),
            )
        }

        /** Checks the picked entries whatever the rules say, leaving out only entries set to Never. */
        suspend fun startSelection(context: Context, entryIds: Collection<Long>): Boolean {
            if (context.workManager.isRunning(TAG)) return false
            val selectionName = LibraryUpdateSelectionFile(context).write(entryIds)
            return enqueueManual(context, workDataOf(KEY_SELECTION to selectionName))
        }

        suspend fun startSkippedOfLatest(context: Context): Boolean {
            return enqueueManual(context, workDataOf(KEY_SKIPPED_OF_LATEST to true))
        }

        private suspend fun enqueueManual(context: Context, inputData: Data): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }

            val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setInputData(inputData)
                .build()
            wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        suspend fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).await()
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
