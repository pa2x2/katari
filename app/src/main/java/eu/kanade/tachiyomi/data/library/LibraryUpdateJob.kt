package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotificationProfile
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotificationProfiles
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.visualName
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshFeature
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshRequest
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshResult
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.state.EntryUpdateEligibilityFeature
import mihon.feature.library.update.pause.LibrarySourcePauses
import mihon.feature.library.update.planning.LibraryUpdateDecision
import mihon.feature.library.update.planning.LibraryUpdatePlanner
import mihon.feature.library.update.planning.LibraryUpdatePlanningContext
import mihon.feature.library.update.planning.LibraryUpdateRequest
import mihon.feature.library.update.planning.LibraryUpdateSettings
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class)
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var sourceManager: SourceManager

    @Inject
    private lateinit var rulesRepository: LibraryUpdateRulesRepository

    @Inject
    private lateinit var eligibility: EntryUpdateEligibilityFeature

    @Inject
    private lateinit var reportRepository: LibraryUpdateReportRepository

    @Inject
    private lateinit var getLibraryEntries: GetLibraryEntries

    @Inject
    private lateinit var categoryRepository: CategoryRepository

    @Inject
    private lateinit var entryRepository: EntryRepository

    @Inject
    private lateinit var fetchInterval: FetchInterval

    @Inject
    private lateinit var entryLibraryUpdateRefreshFeature: EntryLibraryUpdateRefreshFeature

    @Inject
    private lateinit var mergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature

    @Inject
    private lateinit var notifier: LibraryUpdateNotifier

    @Inject
    private lateinit var notificationProfiles: LibraryUpdateNotificationProfiles

    @Inject
    private lateinit var activeProfileProvider: ActiveProfileProvider

    @Inject
    private lateinit var profileStore: ProfileStore

    @Inject
    private lateinit var entryTypePresentationFeature: EntryTypePresentationFeature

    init {
        context.appGraph.inject(this)
    }

    /**
     * The profile this update runs for. Everything it reads and writes goes to this profile explicitly, so switching
     * profiles while it runs leaves it working on the library it started with.
     */
    private val profileId = inputData.getLong(KEY_PROFILE, -1L)
        .takeIf { it >= 0L }
        // Requests queued by an older version carry no profile.
        ?: activeProfileProvider.activeProfileId
    private val libraryPreferences = LibraryPreferences(profileStore.profileStore(profileId))

    // Sources update concurrently, and getAndSet is a separate read and write
    private val newUpdatesCountLock = Any()
    private val settingsReader = LibraryUpdateSettings.Reader(libraryPreferences, rulesRepository)
    private val sourcePauses = LibrarySourcePauses(libraryPreferences)
    private val planner = LibraryUpdatePlanner(eligibility)

    private val selectionFile = LibraryUpdateSelectionFile(context)

    private var entriesToUpdate: List<Entry> = mutableListOf()
    private var updateScope = LibraryUpdateScope.Library

    /** The category, source or type a page refresh covers, as the user knows it. */
    private var updateScopeName: String? = null
    private var skippedCount = 0
    private var currentFetchWindow: Pair<Long, Long> = Pair(0L, 0L)

    /** Start of this update, which also identifies it in the update report. */
    private var startedAt = 0L

    private var notificationProfile: LibraryUpdateNotificationProfile? = null

    private suspend fun notificationProfile(): LibraryUpdateNotificationProfile {
        return notificationProfile ?: notificationProfiles.resolve(profileId).also { notificationProfile = it }
    }

    override suspend fun doWork(): Result = coroutineScope {
        // Extensions read logins and API keys from this profile's source preferences, not the active profile's.
        val update = async(SourcePreferenceProfile.of(profileId), start = CoroutineStart.LAZY) { update() }
        // Registered before the skip check, so a stop either finds the update running or marks it skipped first.
        running[id] = update
        try {
            if (LibraryUpdateQueue.consumeSkip(context, id)) {
                logcat(LogPriority.INFO) { "Skipping library update that was taken out of the queue" }
                // Never started, it would keep the scope from completing.
                update.cancel()
                inputData.getString(KEY_SELECTION)?.let(selectionFile::delete)
                Result.success()
            } else {
                update.await()
            }
        } catch (_: CancellationException) {
            // Stopped by the user, or by WorkManager; either way the updates queued behind it go ahead.
            logcat(LogPriority.INFO) { "Library update cancelled" }
            Result.success()
        } finally {
            running.remove(id)
        }
    }

    private suspend fun update(): Result {
        val automatic = tags.contains(LibraryUpdateQueue.TAG_AUTOMATIC)
        logcat(LogPriority.INFO) { "Starting library update (auto=$automatic, input=${inputData.keyValueMap})" }

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
        updateScopeName = (request as? LibraryUpdateRequest.FollowRules)?.let { scopeName(it) }
        publishProgress(completed = 0)

        setForegroundSafely()

        startedAt = Clock.System.now().toEpochMilliseconds()
        libraryPreferences.lastUpdatedTimestamp.set(startedAt)

        return withIOContext {
            val profile = notificationProfile()
            val nextInQueue = launch {
                LibraryUpdateQueue.nextProfileFlow(context, id).collect { next ->
                    notifier.showNextInQueue(profile, next?.let { notificationProfiles.name(it) })
                }
            }
            try {
                planQueue(request)
                publishProgress(completed = 0)
                updateEntryChapterList()
                logcat(LogPriority.INFO) { "Library update completed" }
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e) { "Library update failed" }
                // A failure would fail every update queued behind this one in the chain.
                Result.success()
            } finally {
                nextInQueue.cancel()
                withNonCancellableContext {
                    reportRepository.finishRun(profileId, startedAt, Clock.System.now().toEpochMilliseconds())
                }
                selectionName?.let(selectionFile::delete)
                notifier.cancelProgressNotification()
            }
        }
    }

    /** "Reading", or "Reading · Manga" for a page limited by more than one of category, source and type. */
    private suspend fun scopeName(request: LibraryUpdateRequest.FollowRules): String? {
        return listOfNotNull(
            request.categoryId
                ?.let { categoryRepository.get(it, profileId) }
                ?.let { if (it.isSystemCategory) context.stringResource(MR.strings.label_default) else it.name },
            request.sourceId?.let { sourceManager.getDisplayInfo(it).visualName() },
            request.entryType?.let {
                context.stringResource(it.entryTypePresentation(entryTypePresentationFeature).displayNameLabel)
            },
        )
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")
    }

    private suspend fun readRequest(automatic: Boolean, selectionName: String?): LibraryUpdateRequest {
        if (selectionName != null) {
            return LibraryUpdateRequest.Selection(selectionFile.read(selectionName))
        }
        if (inputData.getBoolean(KEY_SKIPPED_OF_LATEST, false)) {
            val reportStart = reportRepository.getLatestRun(profileId)?.startedAt ?: Long.MAX_VALUE
            val skipped = reportRepository.getStatuses(profileId).values
                .filter { it.decidedAt >= reportStart && it.outcome == EntryUpdateOutcome.SKIPPED }
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
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.libraryUpdateProgressNotification(notificationProfile()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun planQueue(request: LibraryUpdateRequest) {
        // Ended pauses already don't count; dropping them lets the screens that list paused sources catch up.
        sourcePauses.dropEnded(startedAt)
        val settings = settingsReader.read(startedAt, profileId)
        val timeZone = TimeZone.currentSystemDefault()
        currentFetchWindow = fetchInterval.getWindow(
            Clock.System.now().toLocalDateTime(timeZone).date,
            timeZone,
        )
        val lastCheckedAt = reportRepository.getStatuses(profileId).values
            .mapNotNull { status -> status.lastCheckedAt?.let { status.entryId to it } }
            .toMap()
        val decisions = planner.plan(
            request = request,
            items = getLibraryEntries.await(profileId),
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

        notifier.showQueueSizeWarningNotificationIfNeeded(notificationProfile(), checks.map { it.item }, profileId)

        entriesToUpdate = checks.expandToMemberEntries()
            .sortedBy { it.title }

        // Keeping the report of the last full update lets a retry from it recheck one entry without losing the rest.
        if (request.coversLibrary || reportRepository.getLatestRun(profileId) == null) {
            reportRepository.startRun(
                profileId = profileId,
                startedAt = startedAt,
                trigger = when {
                    request is LibraryUpdateRequest.Selection -> LibraryUpdateTrigger.SELECTION
                    request is LibraryUpdateRequest.FollowRules && request.automatic -> LibraryUpdateTrigger.AUTOMATIC
                    else -> LibraryUpdateTrigger.MANUAL
                },
                // Merged entries are checked, and reported, member by member.
                librarySize = left.size + entriesToUpdate.size,
            )
        }
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

                                val entry = entryRepository.getEntryById(queuedEntry.id, profileId)
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
                                                    // Entries added without opening them have no details yet
                                                    fetchMetadata = !entry.initialized ||
                                                        libraryPreferences.autoUpdateMetadata.get(),
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
                                                    synchronized(newUpdatesCountLock) {
                                                        libraryPreferences.newUpdatesCount.getAndSet {
                                                            it + newChapters.size
                                                        }
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
            notifier.showUpdateNotifications(notificationProfile(), newUpdates)
        }
        refreshSession.complete()

        logcat(LogPriority.INFO) {
            "Library update finished with ${newUpdates.size} updated entr${if (newUpdates.size == 1) "y" else "ies"} and ${failedUpdates.size} failure${if (failedUpdates.size == 1) "" else "s"}"
        }

        if (failedUpdates.isNotEmpty()) {
            notifier.showUpdateErrorNotification(notificationProfile(), failedUpdates.size)
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
        notifier.showProgressNotification(notificationProfile(), updatingEntry, progress(completed.load()))

        block()

        ensureActive()

        updatingEntry.remove(entry)
        val completedCount = completed.incrementAndFetch()
        notifier.showProgressNotification(notificationProfile(), updatingEntry, progress(completedCount))
        publishProgress(completedCount)
    }

    private fun progress(completed: Int) = LibraryUpdateProgress(
        scope = updateScope,
        scopeName = updateScopeName,
        completed = completed,
        total = entriesToUpdate.size,
        skipped = skippedCount,
    )

    /** Makes the progress readable outside the notification; see [LibraryUpdateQueue.stateFlow]. */
    private suspend fun publishProgress(completed: Int) {
        setProgress(
            workDataOf(
                KEY_PROGRESS_SCOPE to updateScope.name,
                KEY_PROGRESS_SCOPE_NAME to updateScopeName,
                KEY_PROGRESS_COMPLETED to completed,
                KEY_PROGRESS_TOTAL to entriesToUpdate.size,
                KEY_PROGRESS_SKIPPED to skippedCount,
            ),
        )
    }

    companion object {
        /** Key for category to update. */
        internal const val KEY_CATEGORY = "category"

        /** Key for source to update. */
        internal const val KEY_SOURCE = "source"

        /** Key for entry type to update. */
        internal const val KEY_ENTRY_TYPE = "entry_type"

        private const val KEY_PROGRESS_SCOPE = "progress_scope"
        private const val KEY_PROGRESS_SCOPE_NAME = "progress_scope_name"
        private const val KEY_PROGRESS_COMPLETED = "progress_completed"
        private const val KEY_PROGRESS_TOTAL = "progress_total"
        private const val KEY_PROGRESS_SKIPPED = "progress_skipped"

        /** Name of the [LibraryUpdateSelectionFile] listing the entries to check. */
        internal const val KEY_SELECTION = "selection"

        /** Checks the entries the latest update skipped by rules. */
        internal const val KEY_SKIPPED_OF_LATEST = "skipped_of_latest"

        /** Id of the profile the update runs for. */
        internal const val KEY_PROFILE = "profile"

        /** Updates running in this process, by work id, so a stop can end one without cancelling the queue. */
        private val running = ConcurrentHashMap<UUID, Job>()

        /** Ends the running update [id] as finished; false when it doesn't run in this process. */
        internal fun stopRunning(id: UUID): Boolean {
            val update = running[id] ?: return false
            update.cancel()
            return true
        }

        internal fun Data.toLibraryUpdateProgress(): LibraryUpdateProgress? {
            val scope = getString(KEY_PROGRESS_SCOPE)
                ?.let { name -> LibraryUpdateScope.entries.find { it.name == name } }
                ?: return null
            return LibraryUpdateProgress(
                scope = scope,
                scopeName = getString(KEY_PROGRESS_SCOPE_NAME),
                completed = getInt(KEY_PROGRESS_COMPLETED, 0),
                total = getInt(KEY_PROGRESS_TOTAL, 0),
                skipped = getInt(KEY_PROGRESS_SKIPPED, 0),
            )
        }
    }
}
