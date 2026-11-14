package eu.kanade.tachiyomi.data.library

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.library.EntryLibraryUpdateRefreshFeature
import app.katari.entry.interactions.library.EntryLibraryUpdateRefreshRequest
import app.katari.entry.interactions.library.EntryLibraryUpdateRefreshResult
import app.katari.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import app.katari.entry.interactions.state.EntryUpdateEligibilityFeature
import app.katari.library.update.pause.LibrarySourcePauses
import app.katari.library.update.planning.LibraryUpdateDecision
import app.katari.library.update.planning.LibraryUpdatePlanner
import app.katari.library.update.planning.LibraryUpdatePlanningContext
import app.katari.library.update.planning.LibraryUpdateRequest
import app.katari.library.update.planning.LibraryUpdateSettings
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.domain.library.update.model.LibraryUpdateProgress
import tachiyomi.domain.library.update.model.LibraryUpdateScope
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock

/**
 * One library update of [input]: it plans which entries the profile's rules let it check, checks them source by
 * source, and keeps the update report. Everything it reads and writes goes to the input's profile explicitly, so
 * switching profiles while it runs leaves it working on the library it started with. One serves a single [run].
 */
@OptIn(ExperimentalAtomicApi::class)
@AssistedInject
class LibraryUpdateRun(
    @Assisted private val input: LibraryUpdateInput,
    @Assisted private val onProgress: suspend (LibraryUpdateProgress) -> Unit,
    private val context: PlatformContext,
    private val sourceManager: SourceManager,
    private val rulesRepository: LibraryUpdateRulesRepository,
    private val eligibility: EntryUpdateEligibilityFeature,
    private val reportRepository: LibraryUpdateReportRepository,
    private val getLibraryEntries: GetLibraryEntries,
    private val categoryRepository: CategoryRepository,
    private val entryRepository: EntryRepository,
    private val fetchInterval: FetchInterval,
    private val entryLibraryUpdateRefreshFeature: EntryLibraryUpdateRefreshFeature,
    private val mergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature,
    private val notifications: LibraryUpdateNotifications,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
    private val selectionFile: LibraryUpdateSelectionFile,
    profileStore: ProfileStore,
) {
    @AssistedFactory
    fun interface Factory {
        /** [onProgress] gets how far the update got, for the queue's state. */
        fun create(input: LibraryUpdateInput, onProgress: suspend (LibraryUpdateProgress) -> Unit): LibraryUpdateRun
    }

    private val profileId = input.profileId
    private val libraryPreferences = LibraryPreferences(profileStore.profileStore(profileId))

    // Sources update concurrently, and getAndSet is a separate read and write
    private val newUpdatesCountLock = Any()
    private val settingsReader = LibraryUpdateSettings.Reader(libraryPreferences, rulesRepository)
    private val sourcePauses = LibrarySourcePauses(libraryPreferences)
    private val planner = LibraryUpdatePlanner(eligibility)

    private var entriesToUpdate = LibraryUpdateRunEntries(emptyList())
    private var updateScope = LibraryUpdateScope.Library

    /** The category, source or type a page refresh covers, as the user knows it. */
    private var updateScopeName: String? = null
    private var skippedCount = 0
    private var currentFetchWindow: Pair<Long, Long> = Pair(0L, 0L)

    /** Start of this update, which also identifies it in the update report. */
    private var startedAt = 0L

    /**
     * Runs the update. A failure along the way ends it as finished, since failing would fail every update queued behind
     * it; it is stopped by cancelling the coroutine that runs it.
     *
     * @param nextInQueue the profile whose update starts after this one, or null while none waits.
     * @param onScoped called once the update knows what it covers, before it reads the library; Android brings the
     * update to the foreground there.
     */
    suspend fun run(nextInQueue: Flow<Long?>, onScoped: suspend () -> Unit) {
        logcat(LogPriority.INFO) { "Starting library update ($input)" }

        val request = readRequest()
        updateScope = when (request) {
            is LibraryUpdateRequest.Selection -> LibraryUpdateScope.Selection
            is LibraryUpdateRequest.FollowRules -> LibraryUpdateScope.of(
                categoryId = request.categoryId,
                sourceId = request.sourceId,
                entryType = request.entryType,
            )
        }
        updateScopeName = (request as? LibraryUpdateRequest.FollowRules)?.let { scopeName(it) }
        startedAt = Clock.System.now().toEpochMilliseconds()
        publishProgress(completed = 0)

        onScoped()

        withIOContext {
            val nextInQueueNotification = launch {
                nextInQueue.collect { next -> notifications.showNextInQueue(profileId, next) }
            }
            try {
                planQueue(request)
                publishProgress(completed = 0)
                updateEntryChapterList()
                logcat(LogPriority.INFO) { "Library update completed" }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e) { "Library update failed" }
            } finally {
                nextInQueueNotification.cancel()
                withNonCancellableContext {
                    val finishedAt = Clock.System.now().toEpochMilliseconds()
                    reportRepository.finishRun(profileId, startedAt, finishedAt)
                }
                input.selectionName?.let(selectionFile::delete)
                notifications.cancelProgress()
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
            request.entryType?.let { type ->
                context.stringResource(entryTypePresentationFeature.presentation(type).presentation.displayNameLabel)
            },
        )
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")
    }

    private suspend fun readRequest(): LibraryUpdateRequest {
        if (input.selectionName != null) {
            return LibraryUpdateRequest.Selection(selectionFile.read(input.selectionName))
        }
        if (input.skippedOfLatest) {
            val reportStart = reportRepository.getLatestRun(profileId)?.startedAt ?: Long.MAX_VALUE
            val skipped = reportRepository.getStatuses(profileId).values
                .filter { it.decidedAt >= reportStart && it.outcome == EntryUpdateOutcome.SKIPPED }
                .mapTo(mutableSetOf(), EntryUpdateStatus::entryId)
            return LibraryUpdateRequest.Selection(skipped)
        }
        return LibraryUpdateRequest.FollowRules(
            automatic = input.automatic,
            categoryId = input.categoryId,
            sourceId = input.sourceId,
            entryType = input.entryType,
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

        notifications.showQueueSizeWarningIfNeeded(profileId, checks.map { it.item }, rulesProfileId = profileId)

        entriesToUpdate = LibraryUpdateRunEntries(
            checks.expandToMemberEntries()
                .sortedBy { it.title },
        )

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
     * Checks the entries in [entriesToUpdate], sources side by side and each source one entry at a time, showing the
     * progress as it goes.
     */
    private suspend fun updateEntryChapterList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingEntries = CopyOnWriteArrayList<Entry>()
        val newUpdates = CopyOnWriteArrayList<Pair<Entry, List<EntryChapter>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Entry, String?>>()
        val refreshSession = entryLibraryUpdateRefreshFeature.newSession()

        logcat(LogPriority.INFO) { "Processing ${entriesToUpdate.size} queued library entries" }

        coroutineScope {
            entriesToUpdate.planned.groupBy { it.source }.values
                // The biggest sources take longest, so starting them first finishes the whole update sooner
                .sortedByDescending { it.size }
                .map { entriesInSource ->
                    async {
                        semaphore.withPermit {
                            val queue = ArrayDeque(entriesInSource)
                            while (queue.isNotEmpty()) {
                                ensureActive()

                                val queuedEntry = queue.removeFirst()
                                val entry = entryRepository.getEntryById(queuedEntry.id, profileId)
                                // Don't continue to update if entry is not in library
                                if (entry?.favorite != true) {
                                    continue
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
                                                    firstFetchIsBackCatalogue = !entriesToUpdate.joined(entry.id),
                                                ),
                                            )
                                        ) {
                                            is EntryLibraryUpdateRefreshResult.Updated -> {
                                                queue.addAll(joinMembers(result.joinedEntryIds))
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
                                                    newUpdates.add(queuedEntry to newChapters)
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

        notifications.cancelProgress()

        if (newUpdates.isNotEmpty()) {
            notifications.showUpdates(profileId, newUpdates)
        }
        refreshSession.complete()

        logcat(LogPriority.INFO) {
            "Library update finished with ${newUpdates.size} updated entr${if (newUpdates.size == 1) "y" else "ies"} and ${failedUpdates.size} failure${if (failedUpdates.size == 1) "" else "s"}"
        }

        if (failedUpdates.isNotEmpty()) {
            notifications.showUpdateErrors(profileId, failedUpdates.size)
        }
    }

    /**
     * Adds members that joined a merged entry while it was refreshed. The source listed them under that entry, so the
     * profile, rules and filters that let the entry into this update let them in too.
     */
    private suspend fun joinMembers(entryIds: Set<Long>): List<Entry> {
        if (entryIds.isEmpty()) return emptyList()
        val members = entryIds
            .mapNotNull { entryRepository.getEntryById(it, profileId) }
            .filter(Entry::favorite)
        val joined = entriesToUpdate.join(members)
        if (joined.isNotEmpty()) {
            reportRepository.growRun(profileId, startedAt, joined.size)
            logcat(LogPriority.INFO) {
                "Queued ${joined.size} merged entry member(s) that joined during the library update"
            }
        }
        return joined
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

    /** Shows [entry] among the ones being checked while [block] checks it, and counts it as done afterwards. */
    private suspend fun withUpdateNotification(
        updatingEntry: CopyOnWriteArrayList<Entry>,
        completed: AtomicInt,
        entry: Entry,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingEntry.add(entry)
        showProgress(progress(completed.load(), updatingEntry))

        block()

        ensureActive()

        updatingEntry.remove(entry)
        showProgress(progress(completed.incrementAndFetch(), updatingEntry))
    }

    private suspend fun showProgress(progress: LibraryUpdateProgress) {
        notifications.showProgress(profileId, progress)
        onProgress(progress)
    }

    private suspend fun publishProgress(completed: Int) {
        onProgress(progress(completed, checking = emptyList()))
    }

    private fun progress(completed: Int, checking: List<Entry>) = LibraryUpdateProgress(
        scope = updateScope,
        scopeName = updateScopeName,
        completed = completed,
        total = entriesToUpdate.size,
        skipped = skippedCount,
        startedAt = startedAt,
        checking = checking.map { it.title },
    )
}
