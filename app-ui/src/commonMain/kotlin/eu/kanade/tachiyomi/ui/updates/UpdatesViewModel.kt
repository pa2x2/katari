package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.download.EntryDownloadActionAvailability
import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.download.EntryDownloadActionRequest
import app.katari.entry.interactions.download.EntryDownloadCancellationResult
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.download.EntryDownloadState
import app.katari.entry.interactions.download.EntryDownloadStatus
import app.katari.entry.interactions.download.sampleProgress
import app.katari.entry.interactions.merge.EntryMergeNavigationFeature
import app.katari.entry.interactions.merge.EntryMergeSubject
import app.katari.entry.interactions.navigation.EntryOpenFeature
import app.katari.entry.interactions.navigation.EntryOpenResult
import app.katari.entry.interactions.state.EntryBookmarkAvailability
import app.katari.entry.interactions.state.EntryBookmarkFeature
import app.katari.entry.interactions.state.EntryBookmarkStatus
import app.katari.entry.interactions.state.EntryBookmarkTarget
import app.katari.entry.interactions.state.EntryConsumptionFeature
import app.katari.entry.interactions.state.EntryConsumptionStatus
import app.katari.entry.interactions.translate.EntryTranslateFeature
import app.katari.entry.interactions.translate.EntryTranslateStatus
import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.library.update.pause.LibrarySourcePauses
import app.katari.library.update.pause.PausedLibrarySources
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.profiles.core.ProfileScopedStateEvent
import app.katari.profiles.core.observeProfileScopedState
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.core.util.addOrRemove
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.TranslatableChapter
import eu.kanade.presentation.updates.UpdatesLibraryUpdateStatus
import eu.kanade.presentation.updates.UpdatesSelectionState
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.presentation.updates.toUpdatesUiModels
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.visualName
import eu.kanade.tachiyomi.ui.base.viewmodel.GatedStateViewModel
import eu.kanade.tachiyomi.ui.collapseByVisibleEntry
import eu.kanade.tachiyomi.ui.entry.translation.ChapterTranslationModel
import eu.kanade.tachiyomi.ui.entry.translation.EntryTranslateSetupHost
import eu.kanade.tachiyomi.ui.library.update.activeProfileState
import eu.kanade.tachiyomi.ui.library.update.report.NewerOtherDeviceUpdate
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import logcat.LogPriority
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.pausedWhileUnsubscribed
import tachiyomi.core.common.util.lang.resubscribeOnFailure
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.domain.library.update.model.subscribeLatestSummary
import tachiyomi.domain.library.update.model.subscribeResultsSince
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.domain.updates.model.UpdatesFeed
import tachiyomi.domain.updates.model.UpdatesFeedFilter
import tachiyomi.domain.updates.model.UpdatesFeedRow
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.model.toUpdateItem
import tachiyomi.domain.updates.service.UpdatesPreferences
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class, binding = binding<ViewModel>())
class UpdatesViewModel(
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val entryDownloadActionFeature: EntryDownloadActionFeature,
    private val entryConsumptionFeature: EntryConsumptionFeature,
    private val entryBookmarkFeature: EntryBookmarkFeature,
    private val getUpdates: GetUpdates,
    private val getEntry: GetEntry,
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryRepository: EntryRepository,
    private val entryOpenFeature: EntryOpenFeature,
    private val libraryPreferences: LibraryPreferences,
    private val updatesPreferences: UpdatesPreferences,
    private val sourceManager: SourceManager,
    private val activeProfileProvider: ActiveProfileProvider,
    context: PlatformContext,
    private val libraryUpdateQueue: LibraryUpdateQueue,
    reportRepository: LibraryUpdateReportRepository,
    newerOtherDeviceUpdate: NewerOtherDeviceUpdate,
    sourceRepository: SourceRepository,
    translateFeature: EntryTranslateFeature,
    translationLanguages: EntryTranslationLanguagesFeature,
    translationSetupHost: EntryTranslateSetupHost,
) : GatedStateViewModel<UpdatesViewModel.State>(State()) {

    val snackbarHostState = SnackbarHostState()

    /** Translation of listed chapters; chapters to translate that are not downloaded yet are downloaded. */
    val translation = ChapterTranslationModel(
        scope = viewModelScope,
        context = context,
        snackbarHostState = snackbarHostState,
        download = { chapters ->
            viewModelScope.launch {
                chapters.groupBy { it.entry }.forEach { (entry, group) ->
                    entryDownloadActionFeature.download(entry, group.map { it.chapter }, startNow = false)
                }
            }
        },
        feature = translateFeature,
        languages = translationLanguages,
        setupHost = translationSetupHost,
    )

    /**
     * Statuses of listed chapters that are translated, queued or failed. Only downloaded chapters can be translated,
     * so only their entries are looked at for stored translations.
     */
    val translationStatuses: StateFlow<Map<Long, EntryTranslateStatus>> = mutableState
        .map { state ->
            state.items
                .filter {
                    it.update is UpdateItem.EntryUpdate &&
                        it.downloadStateProvider() == EntryDownloadState.DOWNLOADED
                }
                .map { it.update.entryId }
                .distinct()
        }
        .distinctUntilChanged()
        .mapLatest { entryIds -> entryIds.mapNotNull { getEntry.await(it) } }
        .flatMapLatest { entries ->
            combine(translation.statuses(entries), translation.queueStatuses()) { translated, queued ->
                translated + queued
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val libraryUpdate: Flow<LibraryUpdateState?> = libraryUpdateQueue.activeProfileState(activeProfileProvider)

    private val pausedSourceNames: Flow<List<String>> = PausedLibrarySources(
        sourcePauses = LibrarySourcePauses(libraryPreferences),
        sourceRepository = sourceRepository,
        sourceManager = sourceManager,
    )
        .subscribe()

    val libraryUpdateStatus: StateFlow<UpdatesLibraryUpdateStatus?> = combine(
        libraryUpdate,
        libraryUpdate
            .map { (it as? LibraryUpdateState.Running)?.progress?.startedAt }
            .distinctUntilChanged()
            .flatMapLatest { startedAt -> startedAt?.let(reportRepository::subscribeResultsSince) ?: flowOf(null) },
        combine(
            libraryPreferences.lastUpdateFinishedAt.changes(),
            libraryPreferences.lastUpdatedTimestamp.changes(),
        ) { finishedAt, startedAt -> finishedAt.takeIf { it > 0 } ?: startedAt },
        combine(reportRepository.subscribeLatestSummary(), newerOtherDeviceUpdate.subscribe(), ::Pair),
        pausedSourceNames,
    ) { update, resultsSoFar, lastUpdated, (latest, otherDevice), pausedSources ->
        UpdatesLibraryUpdateStatus(
            update = update,
            resultsSoFar = resultsSoFar,
            lastUpdated = lastUpdated,
            failed = latest?.failed ?: 0,
            otherDevice = otherDevice,
            pausedSources = pausedSources,
            hasReport = latest != null || otherDevice != null,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cancelLibraryUpdate() {
        viewModelScope.launchIO { libraryUpdateQueue.stop() }
    }

    private val selectionState = UpdatesSelectionState()
    private val selectedKeys: HashSet<LibraryItemKey> = HashSet()

    // Progress ticks often, so only the download indicators that read it recompose instead of every tick rebuilding
    // the list
    private val downloadProgress = mutableStateMapOf<Long, Int>()

    init {
        gatedScope.launchIO {
            // Set date limit for recent chapters/episodes
            val limit = Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())

            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                val filters = updatesPreferences.feedFilterChanges()
                val durableDownloadChanges = filters
                    .distinctUntilChanged { old, new -> old.downloaded == new.downloaded }
                    .flatMapLatest { filter ->
                        if (filter.downloaded == TriState.DISABLED) {
                            emptyFlow<Unit>()
                        } else {
                            downloadRuntime.statusUpdates()
                                .filter { it.persistedContentChanged }
                                .map { Unit }
                        }
                    }
                combine(
                    getUpdates.subscribeFeed(profileId, limit).distinctUntilChanged(),
                    // Only signals a recheck, so holding it back while hidden loses nothing
                    merge(downloadRuntime.changes, durableDownloadChanges).pausedWhileUnsubscribed(),
                    filters,
                ) { feed, _, filter ->
                    UpdatesData(feed, filter)
                }
            }.collectLatest { event ->
                when (event) {
                    is ProfileScopedStateEvent.Reset -> {
                        selectedKeys.clear()
                        selectionState.reset()
                        mutableState.update {
                            it.copy(
                                isLoading = true,
                                items = emptyList(),
                                feed = FeedSummary(),
                                dialog = null,
                            )
                        }
                    }
                    is ProfileScopedStateEvent.Value -> {
                        val (feed, filter) = event.value
                        val filterIgnoringDownloads = filter.copy(downloaded = TriState.DISABLED)
                        val resolvedUpdates = feed.rows
                            .filter { row -> filterIgnoringDownloads.matches(row) { true } }
                            .resolveUpdates()
                        mutableState.update {
                            // Download states are read here rather than while the rows resolve: a state that changed
                            // during those reads would overwrite the status update reporting it, and a download's last
                            // update has none after it to fix the row. A status update landing meanwhile reruns this.
                            val shownUpdates = resolvedUpdates.mapNotNull { resolved ->
                                val status = downloadStatus(resolved.row.update)
                                (resolved to status).takeIf {
                                    filter.matches(resolved.row) { status.state == EntryDownloadState.DOWNLOADED }
                                }
                            }
                            val updateItems = shownUpdates
                                .collapseByVisibleEntry(
                                    actualEntryId = { (resolved) -> resolved.update.entryId },
                                    visibleEntryId = { (resolved) -> resolved.visibleEntryId },
                                )
                                .map { (resolved, status) -> resolved.toUpdatesItem(status) }
                                .toPersistentList()
                            it.copy(
                                isLoading = false,
                                items = updateItems,
                                feed = FeedSummary(
                                    filter = filter,
                                    hiddenByFilters = feed.rows.size - shownUpdates.size,
                                    fromHiddenSources = feed.fromHiddenSources,
                                    entryTypes = (feed.rows.map { row -> row.update.entryType } + filter.types.all)
                                        .distinct()
                                        .sorted(),
                                    sources = (feed.rows.map { row -> row.update.sourceId } + filter.sources.all)
                                        .distinct()
                                        .associateWith { sourceManager.getDisplayInfo(it).visualName() },
                                ),
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launchIO {
            downloadRuntime.statusUpdates()
                // Each tick scans the list for its row, and the progress shown needn't move more often than this
                .sampleProgress(200.milliseconds)
                .resubscribeOnFailure(this@UpdatesViewModel)
                .collect(this@UpdatesViewModel::updateDownloadState)
        }
    }

    private suspend fun List<UpdatesFeedRow>.resolveUpdates(): List<ResolvedUpdate> {
        val visibleTargetCache = mutableMapOf<Long, Long>()
        val entryCache = mutableMapOf<Long, Entry?>()

        return map { row ->
            val updateWithRelations = row.update
            val update = updateWithRelations.toUpdateItem(updateWithRelations.entryType)
            val ownerEntry = entryCache.getOrPut(update.entryId) { getEntry.await(update.entryId) }
            val visibleEntryId = visibleTargetCache.getOrPut(update.entryId) {
                ownerEntry?.let { entry ->
                    entryMergeNavigationFeature.resolveNavigation(
                        EntryMergeSubject(entry.profileId, update.entryId),
                    ).visibleEntryId
                } ?: update.entryId
            }
            val entry = entryCache.getOrPut(visibleEntryId) {
                getEntry.await(visibleEntryId)
            }

            when (update) {
                is UpdateItem.EntryUpdate -> {
                    val visibleEntryTitle = entry?.displayTitle ?: updateWithRelations.entryTitle
                    ResolvedUpdate(
                        row = row,
                        update = update,
                        visibleEntryId = visibleEntryId,
                        visibleEntryTitle = visibleEntryTitle,
                        memberTitle = ownerEntry?.displayTitle?.takeIf {
                            visibleEntryId != update.entryId && it != visibleEntryTitle
                        },
                        visibleCoverData = entry?.asEntryCover() ?: updateWithRelations.coverData,
                        downloadAvailable = entryDownloadActionFeature.individualAvailability(
                            downloadActionRequest(update.entryType, update.sourceId),
                        ) == EntryDownloadActionAvailability.Available,
                    )
                }
            }
        }
    }

    private fun ResolvedUpdate.toUpdatesItem(downloadStatus: EntryDownloadStatus): UpdatesItem {
        updateDownloadProgress(downloadStatus)
        val chapterId = update.update.chapterId
        return UpdatesItem(
            update = update,
            visibleEntryId = visibleEntryId,
            visibleEntryTitle = visibleEntryTitle,
            memberTitle = memberTitle,
            visibleCoverData = visibleCoverData,
            downloadStateProvider = { downloadStatus.state },
            downloadProgressProvider = { downloadProgress[chapterId] ?: 0 },
            downloadAvailable = downloadAvailable,
            selected = update.key in selectedKeys,
        )
    }

    /** A feed row with its entry resolved, waiting for the download state it is shown with. */
    private class ResolvedUpdate(
        val row: UpdatesFeedRow,
        val update: UpdateItem.EntryUpdate,
        val visibleEntryId: Long,
        val visibleEntryTitle: String,
        val memberTitle: String?,
        val visibleCoverData: EntryCover,
        val downloadAvailable: Boolean,
    )

    private fun downloadStatus(update: UpdatesWithRelations): EntryDownloadStatus {
        return downloadRuntime.status(
            type = update.entryType,
            childId = update.chapterId,
            childName = update.chapterName,
            childScanlator = update.scanlator,
            childUrl = update.chapterUrl,
            entryTitle = update.entryDownloadTitle,
            sourceId = update.sourceId,
        ) ?: EntryDownloadStatus(update.entryType, update.chapterId, EntryDownloadState.NOT_DOWNLOADED)
    }

    suspend fun updateLibrary(): LibraryUpdateStart = libraryUpdateQueue.startNow()

    /**
     * Update status of downloads.
     *
     * @param download download object containing progress.
     */
    private fun updateDownloadState(download: EntryDownloadStatus) {
        updateDownloadProgress(download)
        mutableState.update { state ->
            val modifiedIndex = state.items.indexOfFirst {
                it.update is UpdateItem.EntryUpdate &&
                    it.update.entryType == download.entryType &&
                    it.update.update.chapterId == download.chapterId
            }
            if (modifiedIndex < 0) return@update state
            val item = state.items[modifiedIndex]
            if (item.downloadStateProvider() == download.state) return@update state

            val newItems = state.items.toMutableList()
            newItems[modifiedIndex] = item.copy(downloadStateProvider = { download.state })
            state.copy(items = newItems)
        }
    }

    private fun updateDownloadProgress(download: EntryDownloadStatus) {
        if (download.state == EntryDownloadState.DOWNLOADING) {
            downloadProgress[download.chapterId] = download.progress
        } else {
            downloadProgress.remove(download.chapterId)
        }
    }

    fun downloadChapters(items: List<UpdatesItem>, action: ChapterDownloadAction) {
        if (!canUseDownloadActions(items)) return
        val chapterItems = items.filter { it.update is UpdateItem.EntryUpdate }
        if (chapterItems.isEmpty()) return
        viewModelScope.launch {
            when (action) {
                ChapterDownloadAction.START -> {
                    downloadChapters(chapterItems)
                    if (chapterItems.any { it.downloadStateProvider() == EntryDownloadState.ERROR }) {
                        entryDownloadActionFeature.retry(
                            chapterItems.map { item ->
                                val update = item.update as UpdateItem.EntryUpdate
                                downloadActionRequest(update.entryType, update.sourceId)
                            },
                        )
                    }
                }
                ChapterDownloadAction.START_NOW -> {
                    val chapterUpdate = chapterItems.singleOrNull()
                        ?.let { it.update as UpdateItem.EntryUpdate }
                        ?: return@launch
                    val entry = getEntry.await(chapterUpdate.update.entryId) ?: return@launch
                    val chapter = entryChapterRepository.getChapterById(chapterUpdate.update.chapterId)
                        ?: return@launch
                    entryDownloadActionFeature.download(
                        entry = entry,
                        chapters = listOf(chapter),
                        startNow = true,
                    )
                }
                ChapterDownloadAction.CANCEL -> {
                    chapterItems.forEach { cancelDownload(it.update as UpdateItem.EntryUpdate) }
                }
                ChapterDownloadAction.DELETE -> {
                    deleteChapters(chapterItems)
                }
            }
            toggleAllSelection(false)
        }
    }

    fun isOpenApplicable(item: UpdatesItem): Boolean = entryOpenFeature.isApplicable(item.update.entryType)

    /** Opens the update's chapter; null when its entry or chapter is gone. */
    suspend fun openUpdate(context: PlatformContext, item: UpdatesItem): EntryOpenResult? {
        val entry = entryRepository.getEntryById(item.visibleEntryId) ?: return null
        val chapterId = when (val update = item.update) {
            is UpdateItem.EntryUpdate -> update.update.chapterId
        }
        val chapter = entryChapterRepository.getChapterById(chapterId) ?: return null
        return entryOpenFeature.open(context, entry, chapter)
    }

    fun translateChapter(item: UpdatesItem, action: ChapterTranslateAction) {
        val update = item.update as? UpdateItem.EntryUpdate ?: return
        viewModelScope.launch {
            val entry = getEntry.await(update.update.entryId) ?: return@launch
            val chapter = entryChapterRepository.getChapterById(update.update.chapterId) ?: return@launch
            translation.run(
                listOf(TranslatableChapter(entry, chapter, item.downloadStateProvider())),
                action,
                translationStatuses.value,
            )
        }
    }

    fun canUseDownloadActions(items: List<UpdatesItem>): Boolean {
        val requests = items.mapNotNull { item ->
            val update = item.update as? UpdateItem.EntryUpdate ?: return@mapNotNull null
            downloadActionRequest(update.entryType, update.sourceId)
        }
        if (requests.size != items.size) return false
        return entryDownloadActionFeature.individualSelectionAvailability(requests) ==
            EntryDownloadActionAvailability.Available
    }

    private fun cancelDownload(update: UpdateItem.EntryUpdate) {
        val result = entryDownloadActionFeature.cancel(
            request = downloadActionRequest(update.entryType, update.sourceId),
            chapterId = update.update.chapterId,
        )
        if (result is EntryDownloadCancellationResult.Cancelled) {
            updateDownloadState(result.status)
        }
    }

    fun markUpdatesConsumed(updates: List<UpdatesItem>, consumed: Boolean) {
        viewModelScope.launchNonCancellable {
            updates.entryChapterSelections()
                .forEach { (entry, chapters) ->
                    entryConsumptionFeature.setConsumed(entry, chapters, consumed)
                }
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param updates the list of chapters to bookmark.
     */
    fun bookmarkUpdates(updates: List<UpdatesItem>, bookmark: Boolean) {
        viewModelScope.launchNonCancellable {
            updates.entryChapterSelections()
                .forEach { (entry, chapters) ->
                    entryBookmarkFeature.setBookmarked(entry, chapters, bookmark)
                }
        }
        toggleAllSelection(false)
    }

    fun hasBookmarkAction(updates: List<UpdatesItem>, bookmark: Boolean): Boolean {
        return updates.hasBookmarkAction(
            bookmark = bookmark,
            feature = entryBookmarkFeature,
        )
    }

    fun hasConsumedAction(updates: List<UpdatesItem>, consumed: Boolean): Boolean {
        return updates.hasConsumedAction(
            consumed = consumed,
            canSetConsumed = entryConsumptionFeature::canSetConsumed,
        )
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param updatesItem the list of chapters to download.
     */
    private fun downloadChapters(updatesItem: List<UpdatesItem>) {
        viewModelScope.launchNonCancellable {
            val chapterUpdates = updatesItem.mapNotNull { it.update as? UpdateItem.EntryUpdate }
            val groupedUpdates = chapterUpdates.groupBy { it.update.entryId }.values
            for (updates in groupedUpdates) {
                val entryId = updates.first().update.entryId
                val entry = getEntry.await(entryId) ?: continue
                val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                entryDownloadActionFeature.download(
                    entry = entry,
                    chapters = chapters,
                )
            }
        }
    }

    /**
     * Delete selected chapters
     *
     * @param updatesItem list of chapters
     */
    fun deleteChapters(updatesItem: List<UpdatesItem>) {
        viewModelScope.launchNonCancellable {
            val chapterUpdates = updatesItem.mapNotNull { it.update as? UpdateItem.EntryUpdate }
            chapterUpdates
                .groupBy { it.update.entryId }
                .entries
                .forEach { (entryId, updates) ->
                    val entry = getEntry.await(entryId) ?: return@forEach
                    val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                    entryDownloadActionFeature.delete(
                        entry = entry,
                        chapters = chapters,
                    )
                }
        }
        toggleAllSelection(false)
    }

    fun showConfirmDeleteChapters(updatesItem: List<UpdatesItem>) {
        setDialog(Dialog.DeleteConfirmation(updatesItem))
    }

    fun toggleSelection(
        item: UpdatesItem,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.update.key == item.update.key }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedKeys.addOrRemove(item.update.key, selected)

                if (selected && fromLongPress) {
                    selectionState.updateRangeSelection(selectedIndex, firstSelection).forEach {
                        val inbetweenItem = get(it)
                        if (!inbetweenItem.selected) {
                            selectedKeys.add(inbetweenItem.update.key)
                            set(it, inbetweenItem.copy(selected = true))
                        }
                    }
                } else if (!fromLongPress) {
                    selectionState.updateSelectionBounds(
                        selectedIndex = selectedIndex,
                        selected = selected,
                        firstSelectedIndex = indexOfFirst { it.selected },
                        lastSelectedIndex = indexOfLast { it.selected },
                    )
                }
            }
            state.copy(items = newItems)
        }
    }

    /** Selects or deselects the updates of a group together, as its single row stands for them all. */
    fun setGroupSelection(items: List<UpdatesItem>, selected: Boolean) {
        val keys = items.mapTo(mutableSetOf()) { it.update.key }
        mutableState.update { state ->
            val newItems = state.items.map {
                if (it.update.key !in keys) return@map it
                selectedKeys.addOrRemove(it.update.key, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }
        selectionState.reset()
    }

    fun toggleGroupExpanded(key: String) {
        mutableState.update {
            val expanded = if (key in it.expandedGroups) it.expandedGroups - key else it.expandedGroups + key
            it.copy(expandedGroups = expanded)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedKeys.addOrRemove(it.update.key, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }

        selectionState.reset()
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedKeys.addOrRemove(it.update.key, !it.selected)
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
        }
        selectionState.reset()
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun resetNewUpdatesCount() {
        libraryPreferences.newUpdatesCount.set(0)
    }

    fun showFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FilterSheet) }
    }

    private data class UpdatesData(
        val feed: UpdatesFeed,
        val filter: UpdatesFeedFilter,
    )

    /**
     * What the feed's filters left out, and what there is to filter by.
     *
     * @param entryTypes the types of the window's updates, so the sheet offers only types that can match, and of
     * the ones already filtered by, so they can be cleared.
     * @param sources names by id of the sources of the window's updates and of the ones already filtered by.
     */
    @Immutable
    data class FeedSummary(
        val filter: UpdatesFeedFilter = UpdatesFeedFilter(),
        val hiddenByFilters: Int = 0,
        val fromHiddenSources: Int = 0,
        val entryTypes: List<EntryType> = emptyList(),
        val sources: Map<Long, String> = emptyMap(),
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<UpdatesItem> = listOf(),
        val feed: FeedSummary = FeedSummary(),
        val expandedGroups: Set<String> = emptySet(),
        val dialog: Dialog? = null,
    ) {
        val selected = items.filter { it.selected }
        val selectionMode = selected.isNotEmpty()

        fun getUiModel(): List<UpdatesUiModel<UpdatesItem>> {
            return items.toUpdatesUiModels(
                dateProvider = { it.update.dateFetch.toLocalDate() },
                entryIdProvider = { it.visibleEntryId },
                expandedGroups = expandedGroups,
            )
        }
    }

    sealed interface Dialog {
        data class DeleteConfirmation(val toDelete: List<UpdatesItem>) : Dialog
        data object FilterSheet : Dialog
    }

    private suspend fun List<UpdatesItem>.entryChapterSelections(): List<EntryChapterSelection> {
        return mapNotNull { it.update as? UpdateItem.EntryUpdate }
            .groupBy { it.update.entryId }
            .mapNotNull { (entryId, updates) ->
                val entry = getEntry.await(entryId) ?: return@mapNotNull null
                val chapters = updates.mapNotNull { entryChapterRepository.getChapterById(it.update.chapterId) }
                EntryChapterSelection(entry, chapters)
            }
            .filter { it.chapters.isNotEmpty() }
    }

    private fun downloadActionRequest(type: EntryType, sourceId: Long): EntryDownloadActionRequest {
        return EntryDownloadActionRequest(type, setOf(sourceId))
    }

    private data class EntryChapterSelection(
        val entry: Entry,
        val chapters: List<EntryChapter>,
    )
}

@Immutable
data class UpdatesItem(
    val update: UpdateItem,
    val visibleEntryId: Long,
    val visibleEntryTitle: String,
    /** The merged member's own title; null unless it differs from [visibleEntryTitle]. */
    val memberTitle: String?,
    val visibleCoverData: EntryCover,
    val downloadStateProvider: () -> EntryDownloadState,
    val downloadProgressProvider: () -> Int,
    val downloadAvailable: Boolean = true,
    val selected: Boolean = false,
)

internal fun List<UpdatesItem>.hasBookmarkAction(
    bookmark: Boolean,
    feature: EntryBookmarkFeature,
): Boolean {
    val entryUpdates = mapNotNull { it.update as? UpdateItem.EntryUpdate }
    if (entryUpdates.size != size) return false
    val targets = entryUpdates.map { EntryBookmarkTarget(it.entryType, it.bookmarkStatus()) }
    return feature.selectionAvailability(targets, bookmark) == EntryBookmarkAvailability.Available
}

internal fun List<UpdatesItem>.hasConsumedAction(
    consumed: Boolean,
    canSetConsumed: (entryType: EntryType, status: EntryConsumptionStatus, consumed: Boolean) -> Boolean,
): Boolean {
    return mapNotNull { it.update as? UpdateItem.EntryUpdate }
        .any { canSetConsumed(it.entryType, it.consumptionStatus(), consumed) }
}

private fun UpdateItem.EntryUpdate.consumptionStatus(): EntryConsumptionStatus {
    return EntryConsumptionStatus(
        consumed = update.read,
        hasPartialProgress = !update.read && update.started,
    )
}

private fun UpdateItem.EntryUpdate.bookmarkStatus(): EntryBookmarkStatus {
    return EntryBookmarkStatus(bookmarked = update.bookmark)
}
