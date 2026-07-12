package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.domain.entry.model.chaptersFiltered
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.buildMergeTargetQuery
import eu.kanade.presentation.entry.components.buildMergeTargets
import eu.kanade.presentation.entry.components.rankMergeTargets
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import eu.kanade.tachiyomi.util.lang.toStoredDisplayName
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.EntryCapabilityInteraction
import mihon.entry.interactions.EntryChildGroupFilterInteraction
import mihon.entry.interactions.EntryChildListInteraction
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryConsumptionInteraction
import mihon.entry.interactions.EntryContinueInteraction
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryDownloadOptionSelection
import mihon.entry.interactions.EntryDownloadOptions
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.EntryPreviewConfig
import mihon.entry.interactions.EntryPreviewHandle
import mihon.entry.interactions.EntryPreviewInteraction
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import mihon.entry.interactions.reader.settings.ReaderPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.interactor.GetDuplicateLibraryEntries
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryChapterFlags
import tachiyomi.domain.entry.interactor.SetEntryFavorite
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.interactor.UpdateMergedEntry
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.util.applyFilter
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

class EntryScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val entryId: Long,
    private val isFromSource: Boolean,
    private val bypassMerge: Boolean = false,
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val duplicatePreferences: tachiyomi.domain.library.service.DuplicatePreferences = Injekt.get(),
    trackPreferences: TrackPreferences = Injekt.get(),
    readerPreferences: ReaderPreferences = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val trackChapter: TrackChapter = Injekt.get(),
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
    private val entryCapabilityInteraction: EntryCapabilityInteraction = Injekt.get(),
    private val entryConsumptionInteraction: EntryConsumptionInteraction = Injekt.get(),
    private val entryContinueInteraction: EntryContinueInteraction = Injekt.get(),
    private val entryPreviewInteraction: EntryPreviewInteraction = Injekt.get(),
    private val entryChildListInteraction: EntryChildListInteraction = Injekt.get(),
    private val entryChildGroupFilterInteraction: EntryChildGroupFilterInteraction = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getMergedEntry: GetMergedEntry = Injekt.get(),
    private val updateMergedEntry: UpdateMergedEntry = Injekt.get(),
    private val getDuplicateLibraryEntries: GetDuplicateLibraryEntries = Injekt.get(),
    private val networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
    private val setEntryChapterFlags: SetEntryChapterFlags = Injekt.get(),
    private val setEntryFavorite: SetEntryFavorite = Injekt.get(),
    private val setEntryCategories: SetEntryCategories = Injekt.get(),
    private val updateEntry: UpdateEntry = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val categoryRepository: CategoryRepository = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val syncEntryWithSource: SyncEntryWithSource = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<EntryScreenModel.State>(State.Loading) {

    private val successState: State.Success?
        get() = state.value as? State.Success

    val entry: Entry?
        get() = successState?.entry

    val source: UnifiedSource?
        get() = successState?.source

    private val isFavorited: Boolean
        get() = entry?.favorite ?: false

    private val allChapters: List<EntryChapterList.Item>?
        get() = successState?.chapters

    private val filteredChapters: List<EntryChapterList.Item>?
        get() = successState?.processedChapters

    val chapterSwipeStartAction = libraryPreferences.swipeToEndAction.get()
    val chapterSwipeEndAction = libraryPreferences.swipeToStartAction.get()
    var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead.get()

    private val skipFiltered by readerPreferences.skipFiltered.asState(screenModelScope)

    private val previewConfigState = MutableStateFlow(EntryPreviewConfig.Disabled)
    val previewConfig = previewConfigState.asStateFlow()

    private val previewLoaderState = MutableStateFlow(EntryPreviewState(pageCount = previewConfigState.value.pageCount))
    val previewState = previewLoaderState.asStateFlow()

    private var previewHandle: EntryPreviewHandle? = null
    private var previewLoadJob: Job? = null
    private var previewPageJobs: Map<Int, Job> = emptyMap()
    private var previewConfigJob: Job? = null
    private var previewConfigEntryKey: Pair<Long, EntryType>? = null
    private var duplicateObservationJob: Job? = null
    private var refreshFromSourceJob: Job? = null

    val isUpdateIntervalEnabled =
        LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in libraryPreferences.autoUpdateEntryRestrictions.get()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    private val selectedChapterIds: HashSet<Long> = HashSet()

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                State.Error -> it
                is State.Success -> func(it)
            }
        }
    }

    private fun updatePreviewState(transform: (EntryPreviewState) -> EntryPreviewState) {
        previewLoaderState.update(transform)
    }

    fun isEntryPreviewEnabled(entry: Entry): Boolean {
        return previewConfigState.value.enabled && entryPreviewInteraction.isSupported(entry)
    }

    private fun isPreviewAvailable(entry: Entry? = successState?.entry): Boolean {
        return entry != null && isEntryPreviewEnabled(entry)
    }

    private fun observePreviewConfig(entry: Entry) {
        val key = entry.id to entry.type
        if (previewConfigEntryKey == key) return

        previewConfigEntryKey = key
        previewConfigJob?.cancel()
        applyPreviewConfig(
            config = entryPreviewInteraction.config(entry),
            reloadExpanded = false,
        )
        previewConfigJob = screenModelScope.launchIO {
            entryPreviewInteraction.configChanges(entry)
                .flowWithLifecycle(lifecycle)
                .collectLatest { config ->
                    applyPreviewConfig(
                        config = config,
                        reloadExpanded = true,
                    )
                }
        }
    }

    private fun applyPreviewConfig(
        config: EntryPreviewConfig,
        reloadExpanded: Boolean,
    ) {
        val previousConfig = previewConfigState.value
        previewConfigState.value = config

        updatePreviewState { preview ->
            val updatedPages = if (preview.isExpanded && preview.pages.isNotEmpty()) {
                preview.pages.take(config.pageCount)
            } else {
                preview.pages
            }
            preview.copy(pageCount = config.pageCount, pages = updatedPages)
        }

        if (!config.enabled) {
            collapsePreview()
            return
        }

        if (reloadExpanded && previewState.value.isExpanded && previousConfig.pageCount != config.pageCount) {
            loadPreview(force = true)
        }
    }

    init {
        observeChildProgressLabels()

        screenModelScope.launchIO {
            mergeAwareEntryAndChaptersFlow(
                entryAndChaptersFlow = entryAndChaptersFlow(),
                mergeGroupFlow = getMergedEntry.subscribeGroupByEntryId(entryId),
                downloadChangesFlow = entryDownloadInteraction.changes,
                downloadQueueFlow = entryDownloadInteraction.queueState,
            )
                .flowWithLifecycle(lifecycle)
                .collectLatest { (entry, chapters) ->
                    observePreviewConfig(entry)
                    val mergePresentation = getMergePresentation(entry)
                    updateSuccessState {
                        it.copy(
                            entry = entry,
                            sourceName = mergePresentation.sourceName,
                            memberIds = mergePresentation.memberIds,
                            memberTitleById = mergePresentation.memberTitleById,
                            mergedMemberTitles = mergePresentation.mergedMemberTitles,
                            mergeTargetId = mergePresentation.mergeTargetId,
                            mergeGroupMemberIds = mergePresentation.mergeGroupMemberIds,
                            childGroupFilterSupported = entryChildGroupFilterInteraction.supports(entry),
                            chapters = chapters.toChapterListItems(entry),
                            duplicateCandidates = if (it.isFromSource && !entry.favorite) {
                                it.duplicateCandidates
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
        }

        if (!bypassMerge) {
            screenModelScope.launchIO {
                combine(
                    getMergedEntry.subscribeGroupByEntryId(entryId).distinctUntilChanged(),
                    entryChildGroupFilterInteraction.excludedGroupsChanged(entryId),
                ) { _, _ -> Unit }
                    .flowWithLifecycle(lifecycle)
                    .collectLatest {
                        val mergedMemberIds = getDisplayedMemberIds()
                        updateSuccessState {
                            it.copy(
                                excludedScanlators = getExcludedChildGroups(it.entry, mergedMemberIds),
                            )
                        }
                    }
            }

            screenModelScope.launchIO {
                combine(
                    getMergedEntry.subscribeGroupByEntryId(entryId).distinctUntilChanged(),
                    entryChildGroupFilterInteraction.availableGroupsChanged(entryId),
                ) { _, _ -> Unit }
                    .flowWithLifecycle(lifecycle)
                    .collectLatest {
                        val mergedMemberIds = getDisplayedMemberIds()
                        updateSuccessState {
                            it.copy(
                                availableScanlators = getAvailableChildGroups(it.entry, mergedMemberIds),
                            )
                        }
                    }
            }
        }

        observeDownloads()

        screenModelScope.launchIO {
            val entry = getEntry.await(entryId)
            if (entry == null) {
                mutableState.update { State.Error }
                return@launchIO
            }

            val mergePresentation = getMergePresentation(entry)
            val chapters = getDisplayedChapters(entry)
                .toChapterListItems(entry)

            if (!entry.favorite) {
                setDefaultChapterFlags(entry)
            }

            val membersNeedingRefresh = getMergeMembersNeedingRefresh()
            val needRefreshInfo = if (mergePresentation.memberIds.size > 1) {
                membersNeedingRefresh.any { !it.initialized }
            } else {
                !entry.initialized
            }
            val needRefreshChapter = if (mergePresentation.memberIds.size > 1) {
                membersNeedingRefresh.isNotEmpty()
            } else {
                chapters.isEmpty()
            }

            // Show what we have earlier
            observePreviewConfig(entry)
            mutableState.update {
                State.Success(
                    entry = entry,
                    source = sourceManager.getOrStub(entry.source),
                    sourceName = mergePresentation.sourceName,
                    isSourceMissing = sourceManager.getDisplayInfo(entry.source).isMissing,
                    memberIds = mergePresentation.memberIds,
                    memberTitleById = mergePresentation.memberTitleById,
                    mergedMemberTitles = mergePresentation.mergedMemberTitles,
                    mergeTargetId = mergePresentation.mergeTargetId,
                    mergeGroupMemberIds = mergePresentation.mergeGroupMemberIds,
                    isFromSource = isFromSource,
                    chapters = chapters,
                    childListInteraction = entryChildListInteraction,
                    childGroupFilterSupported = entryChildGroupFilterInteraction.supports(entry),
                    availableScanlators = getAvailableChildGroups(entry, mergePresentation.memberIds),
                    excludedScanlators = getExcludedChildGroups(entry, mergePresentation.memberIds),
                    duplicateCandidates = emptyList(),
                    isRefreshingData = needRefreshInfo || needRefreshChapter,
                    dialog = null,
                    hideMissingChapters = libraryPreferences.hideMissingChapters.get(),
                )
            }

            observeDuplicateCandidates()

            // Start observe tracking since it only needs entryId
            observeTrackers()

            // Fetch info-chapters when needed
            if (screenModelScope.isActive && (needRefreshInfo || needRefreshChapter)) {
                launchRefreshFromSource(
                    manualFetch = false,
                    fetchInfo = needRefreshInfo,
                    fetchChapters = needRefreshChapter,
                ).join()
            }
        }
    }

    private fun observeChildProgressLabels() {
        screenModelScope.launchIO {
            state
                .filterIsInstance<State.Success>()
                .map { successState ->
                    val displayedChapters = successState.processedChapters.map { it.chapter }
                    EntryChildProgressRequest(
                        entry = successState.entry,
                        chapters = displayedChapters,
                        memberIds = successState.memberIds,
                    )
                }
                .distinctUntilChanged()
                .flatMapLatest { request ->
                    entryChildListInteraction.progressLabels(request)
                        .onStart { emit(emptyMap()) }
                        .catch { error ->
                            logcat(LogPriority.ERROR, error)
                            emit(emptyMap())
                        }
                }
                .flowWithLifecycle(lifecycle)
                .collectLatest { labels ->
                    updateSuccessState {
                        it.copy(childProgressLabels = labels)
                    }
                }
        }
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        launchRefreshFromSource(
            manualFetch = manualFetch,
            fetchInfo = true,
            fetchChapters = true,
        )
    }

    fun setPreviewExpanded(expanded: Boolean) {
        if (!isPreviewAvailable()) {
            collapsePreview()
            return
        }

        if (expanded) {
            updatePreviewState { it.copy(isExpanded = true) }
            if (!previewState.value.hasLoadedContent) {
                loadPreview()
            }
        } else {
            collapsePreview()
        }
    }

    fun retryPreview() {
        if (!previewState.value.isExpanded) return
        loadPreview(force = true)
    }

    private fun loadPreview(force: Boolean = false) {
        if (!isPreviewAvailable()) return
        val pageCount = previewConfigState.value.pageCount

        previewLoadJob?.cancel()
        previewLoadJob = screenModelScope.launchIO {
            clearPreviewResources(resetState = false, cancelLoadJob = false)
            updatePreviewState {
                it.copy(
                    isExpanded = true,
                    isLoading = true,
                    error = null,
                    chapterId = null,
                    pages = emptyList(),
                    pageCount = pageCount,
                )
            }

            awaitRefreshFromSource()
            val state = successState ?: return@launchIO
            if (!isEntryPreviewEnabled(state.entry)) {
                collapsePreview()
                return@launchIO
            }

            if (!force && previewState.value.hasLoadedContent) {
                updatePreviewState { it.copy(isLoading = false) }
                return@launchIO
            }

            runCatching {
                val latestEntry = getEntry.await(entryId) ?: return@runCatching
                val previewChapterItem = if (entryPreviewInteraction.requiresChapter(latestEntry)) {
                    getFirstPreviewChapter()
                        ?: error(context.stringResource(latestEntry.type.entryTypePresentation().noChildrenFoundLabel))
                } else {
                    null
                }
                val handle = entryPreviewInteraction.loadPreview(
                    context = context,
                    entry = latestEntry,
                    chapter = previewChapterItem?.chapter,
                    source = sourceManager.getOrStub(previewChapterItem?.entry?.source ?: latestEntry.source),
                    pageCount = pageCount,
                )
                previewHandle = handle
                val loadedPages = handle.pages.map(::PreviewPage)

                updatePreviewState {
                    it.copy(
                        isExpanded = true,
                        isLoading = false,
                        error = null,
                        chapterId = handle.chapterId,
                        pages = loadedPages,
                        pageCount = pageCount,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    return@onFailure
                }
                clearPreviewResources(resetState = false, cancelLoadJob = false)
                updatePreviewState {
                    it.copy(
                        isExpanded = true,
                        isLoading = false,
                        error = error,
                        chapterId = null,
                        pages = emptyList(),
                        pageCount = pageCount,
                    )
                }
            }
        }
    }

    fun loadPreviewPage(pageIndex: Int) {
        val handle = previewHandle ?: return
        val page = handle.pages.getOrNull(pageIndex) ?: return
        if (page.status.value == EntryPreviewPageStatus.Ready) return
        if (previewPageJobs[pageIndex]?.isActive == true) return

        previewPageJobs = previewPageJobs + (
            pageIndex to screenModelScope.launchIO {
                try {
                    entryPreviewInteraction.loadPage(handle, pageIndex)
                } catch (_: Throwable) {
                    // Page state carries the failure.
                }
            }
            )
    }

    private suspend fun getFirstPreviewChapter(): EntryChapterList.Item? {
        val entry = getEntry.await(entryId) ?: return null
        val mergePresentation = getMergePresentation(entry)
        val chapters = getDisplayedChapters()
        val chapterItems = chapters.toChapterListItems(entry)
        return chapterItems
            .previewReadingChapters(entry, mergePresentation.memberIds, entryChildListInteraction)
            .firstOrNull()
    }

    private fun launchRefreshFromSource(
        manualFetch: Boolean,
        fetchInfo: Boolean,
        fetchChapters: Boolean,
    ): Job {
        refreshFromSourceJob?.cancel()
        return screenModelScope.launchIO {
            updateSuccessState { it.copy(isRefreshingData = true) }
            try {
                refreshFromSourceJob = coroutineContext[Job]
                fetchAllFromSource(
                    manualFetch = manualFetch,
                    fetchDetails = fetchInfo,
                    fetchChapters = fetchChapters,
                )
            } finally {
                if (refreshFromSourceJob === coroutineContext[Job]) {
                    refreshFromSourceJob = null
                    updateSuccessState { it.copy(isRefreshingData = false) }
                }
            }
        }.also { refreshFromSourceJob = it }
    }

    private suspend fun awaitRefreshFromSource() {
        val refreshJob = refreshFromSourceJob
        if (refreshJob?.isActive == true) {
            refreshJob.join()
        }
    }

    private fun collapsePreview() {
        clearPreviewResources(resetState = true)
        updatePreviewState {
            EntryPreviewState(
                isExpanded = false,
                pageCount = previewConfigState.value.pageCount,
            )
        }
    }

    private fun clearPreviewResources(resetState: Boolean, cancelLoadJob: Boolean = true) {
        if (cancelLoadJob) {
            previewLoadJob?.cancel()
            previewLoadJob = null
        }
        previewPageJobs.values.forEach(Job::cancel)
        previewPageJobs = emptyMap()
        previewHandle?.let(entryPreviewInteraction::release)
        previewHandle = null
        if (resetState) {
            previewLoaderState.value = EntryPreviewState(pageCount = previewConfigState.value.pageCount)
        }
    }

    override fun onDispose() {
        super.onDispose()
        previewConfigJob?.cancel()
        duplicateObservationJob?.cancel()
        clearPreviewResources(resetState = false)
    }

    private suspend fun fetchAllFromSource(
        manualFetch: Boolean,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) {
        successState ?: return
        try {
            val membersToRefresh = getMembersToRefreshFromSource(manualFetch)
            val results = membersToRefresh.map { memberEntry ->
                syncEntryWithSource(
                    entry = memberEntry,
                    fetchDetails = fetchDetails,
                    fetchChapters = fetchChapters,
                    manualFetch = manualFetch,
                )
            }

            if (manualFetch) {
                downloadNewEntryChapters(results.flatMap { it.insertedChapters })
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (e: Exception) {
            val message = if (e is NoChaptersException) {
                val entryType = successState?.entry?.type
                context.stringResource(entryType.entryTypePresentation().noChildrenFoundLabel)
            } else if (e is SourceNotInstalledException) {
                context.stringResource(MR.strings.loader_not_implemented_error)
            } else {
                logcat(LogPriority.ERROR, e)
                with(context) { e.formattedMessage }
            }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    // Entry info - start

    fun toggleFavorite() {
        val state = successState ?: return
        if (state.isMerged && isFavorited) {
            showRemoveMergedEntryDialog()
            return
        }
        toggleFavorite(
            onRemoved = {
                screenModelScope.launch {
                    if (!hasDownloads()) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = context.stringResource(MR.strings.delete_downloads_for_manga),
                        actionLabel = context.stringResource(MR.strings.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        deleteDownloads()
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of entry, (removes / adds) entry (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val entry = state.entry

            if (isFavorited) {
                // Remove from library
                if (setEntryFavorite.await(entry.id, false)) {
                    // Remove covers and update last modified in db
                    if (entry.removeCovers() != entry) {
                        updateEntry.awaitUpdateCoverLastModified(entry.id)
                    }
                    withUIContext { onRemoved() }
                }
            } else {
                // Add to library
                // First, check if duplicate exists if callback is provided
                if (checkDuplicate) {
                    val duplicates = getDuplicateLibraryEntries(entry)

                    if (duplicates.isNotEmpty()) {
                        updateSuccessState { it.copy(dialog = Dialog.DuplicateEntry(entry, duplicates)) }
                        return@launchIO
                    }
                }

                // Now check if user previously set categories, when available
                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    // Default category set
                    defaultCategory != null -> {
                        val result = setEntryFavorite.await(entry.id, true)
                        if (!result) return@launchIO
                        moveEntryToCategory(defaultCategory)
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = setEntryFavorite.await(entry.id, true)
                        if (!result) return@launchIO
                        moveEntryToCategory(null)
                    }

                    // Choose a category
                    else -> showChangeCategoryDialog()
                }

                state.source?.let { source ->
                    addTracks.bindEnhancedTrackers(
                        entry = entry,
                        source = EntryTrackingSource.from(source, sourceManager.getDisplayInfo(entry.source)),
                    )
                }
            }
        }
    }

    fun showChangeCategoryDialog() {
        val entry = successState?.entry ?: return
        screenModelScope.launch {
            val categories = getCategories()
            val selection = getDisplayedMemberIds().flatMap { getEntryCategoryIds(it) }.distinct()
            updateSuccessState { successState ->
                successState.copy(
                    dialog = Dialog.ChangeCategory(
                        entry = entry,
                        initialSelection = categories.mapAsCheckboxState { it.id in selection },
                    ),
                )
            }
        }
    }

    fun showSetFetchIntervalDialog() {
        val entry = successState?.entry ?: return
        updateSuccessState {
            it.copy(dialog = Dialog.SetFetchInterval(entry))
        }
    }

    fun setFetchInterval(entry: Entry, interval: Int) {
        screenModelScope.launchIO {
            val updatedEntry = entry.copy(
                // Custom intervals are negative
                fetchInterval = -interval,
            )
            if (entryRepository.update(updatedEntry)) {
                val refreshedEntry = entryRepository.getEntryById(entry.id)
                if (refreshedEntry != null) {
                    updateSuccessState { it.copy(entry = refreshedEntry) }
                }
            }
        }
    }

    /**
     * Returns true if the entry has any downloads.
     */
    private fun hasDownloads(): Boolean {
        val entry = successState?.entry ?: return false
        return entryDownloadInteraction.hasDownloads(entry)
    }

    /**
     * Deletes all the downloads for the entry.
     */
    private fun deleteDownloads() {
        val state = successState ?: return
        screenModelScope.launchNonCancellable {
            entryDownloadInteraction.deleteEntryDownloads(state.entry)
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return categoryRepository.getAll().filterNot { it.isSystemCategory }
    }

    /**
     * Gets the category id's the entry is in, if the entry is not in a category, returns the default id.
     *
     * @param entry the entry to get categories from.
     * @return Array of category ids the entry is in, if none returns default id
     */
    private suspend fun getEntryCategoryIds(entry: Entry): List<Long> {
        return categoryRepository.getCategoriesByEntryId(entry.id)
            .map { it.id }
    }

    private suspend fun getEntryCategoryIds(entryId: Long): List<Long> {
        val entry = entryRepository.getEntryById(entryId) ?: return emptyList()
        return getEntryCategoryIds(entry)
    }

    fun moveEntryToCategoriesAndAddToLibrary(entry: Entry, categories: List<Long>) {
        moveEntryToCategory(categories)
        if (entry.favorite) return

        screenModelScope.launchIO {
            setEntryFavorite.await(entry.id, true)
        }
    }

    /**
     * Move the given entry to categories.
     *
     * @param categories the selected categories.
     */
    private fun moveEntryToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        moveEntryToCategory(categoryIds)
    }

    private fun moveEntryToCategory(categoryIds: List<Long>) {
        screenModelScope.launchIO {
            getDisplayedMemberIds().forEach { memberId ->
                setEntryCategories.await(memberId, categoryIds)
            }
        }
    }

    /**
     * Move the given entry to the category.
     *
     * @param category the selected category, or null for default category.
     */
    private fun moveEntryToCategory(category: Category?) {
        moveEntryToCategories(listOfNotNull(category))
    }

    // Entry info - end

    // Chapters list - start

    private fun observeDownloads() {
        screenModelScope.launchIO {
            entryDownloadInteraction.updates()
                .filter { download ->
                    successState?.chapters.orEmpty().any { item ->
                        item.entry.type == download.entryType && item.chapter.id == download.chapterId
                    }
                }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }
    }

    private fun updateDownloadState(download: EntryDownloadStatus) {
        updateSuccessState { successState ->
            val modifiedIndex = successState.chapters.indexOfFirst {
                it.entry.type == download.entryType && it.id == download.chapterId
            }
            if (modifiedIndex < 0) return@updateSuccessState successState

            val newChapters = successState.chapters.toMutableList().apply {
                val item = removeAt(modifiedIndex)
                    .copy(downloadState = download.state, downloadProgress = download.progress)
                add(modifiedIndex, item)
            }
            successState.copy(chapters = newChapters)
        }
    }

    private suspend fun List<EntryChapter>.toChapterListItems(entry: Entry): List<EntryChapterList.Item> {
        return map { chapter ->
            val chapterEntry = entryRepository.getEntryById(chapter.entryId) ?: entry
            val isLocal = chapterEntry.isLocal()
            val activeDownload = if (isLocal) {
                null
            } else {
                entryDownloadInteraction.getStatus(
                    entryType = chapterEntry.type,
                    chapterId = chapter.id,
                    chapterName = chapter.name,
                    chapterScanlator = chapter.scanlator,
                    chapterUrl = chapter.url,
                    entryTitle = chapterEntry.title,
                    sourceId = chapterEntry.source,
                )
            }
            val downloadState = when {
                isLocal -> EntryDownloadState.DOWNLOADED
                activeDownload != null -> activeDownload.state
                else -> EntryDownloadState.NOT_DOWNLOADED
            }

            EntryChapterList.Item(
                chapter = chapter,
                entry = chapterEntry,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                selected = chapter.id in selectedChapterIds,
            )
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    fun chapterSwipe(chapterItem: EntryChapterList.Item, swipeAction: LibraryPreferences.ChapterSwipeAction) {
        screenModelScope.launch {
            executeChapterSwipeAction(chapterItem, swipeAction)
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    private fun executeChapterSwipeAction(
        chapterItem: EntryChapterList.Item,
        swipeAction: LibraryPreferences.ChapterSwipeAction,
    ) {
        val chapter = chapterItem.chapter
        when (swipeAction) {
            LibraryPreferences.ChapterSwipeAction.ToggleRead -> {
                markChaptersRead(listOf(chapter), !chapter.read)
            }
            LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> {
                bookmarkChapters(listOf(chapter), !chapter.bookmark)
            }
            LibraryPreferences.ChapterSwipeAction.Download -> {
                val downloadAction: ChapterDownloadAction = when (chapterItem.downloadState) {
                    EntryDownloadState.ERROR,
                    EntryDownloadState.NOT_DOWNLOADED,
                    -> ChapterDownloadAction.START_NOW
                    EntryDownloadState.QUEUE,
                    EntryDownloadState.DOWNLOADING,
                    -> ChapterDownloadAction.CANCEL
                    EntryDownloadState.DOWNLOADED -> ChapterDownloadAction.DELETE
                }
                runChapterDownloadActions(
                    items = listOf(chapterItem),
                    action = downloadAction,
                )
            }
            LibraryPreferences.ChapterSwipeAction.Disabled -> throw IllegalStateException()
        }
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    suspend fun getNextUnreadChapter(): EntryChapter? {
        val entry = entry ?: getEntry.await(entryId) ?: return null
        return entryContinueInteraction.findNext(entry)
    }

    /**
     * Continues reading/watching the next chapter/episode for the entry.
     */
    suspend fun continueEntry(context: Context, entry: Entry) {
        entryContinueInteraction.continueEntry(context, entry)
    }

    private fun getBulkDownloadCandidateItems(): List<EntryChapterList.Item> {
        val chapterItems = if (skipFiltered) filteredChapters.orEmpty() else allChapters.orEmpty()
        return chapterItems.filter { item -> item.downloadState == EntryDownloadState.NOT_DOWNLOADED }
    }

    private fun startDownload(
        items: List<EntryChapterList.Item>,
        startNow: Boolean,
    ) {
        val successState = successState ?: return
        if (items.isEmpty()) return

        if (entryDownloadInteraction.supportsDownloadOptions(successState.entry)) {
            updateSuccessState {
                it.copy(
                    dialog = Dialog.DownloadSettings(
                        items = items,
                        startNow = startNow,
                        options = null,
                    ),
                )
            }
            screenModelScope.launchIO {
                val options = entryDownloadInteraction.resolveDownloadOptions(
                    context,
                    successState.entry,
                    items.first().chapter,
                ) ?: return@launchIO
                updateSuccessState { state ->
                    val dialog = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
                    if (dialog.items.map { it.id } != items.map { it.id }) return@updateSuccessState state
                    state.copy(dialog = dialog.copy(options = options))
                }
            }
            return
        }

        queueDownload(items, startNow)
    }

    fun confirmDownloadSettings(selection: EntryDownloadOptionSelection) {
        val dialog = successState?.dialog as? Dialog.DownloadSettings ?: return
        if (dialog.options == null) return
        dismissDialog()
        toggleAllSelection(false)
        queueDownload(dialog.items, dialog.startNow, selection)
    }

    private fun queueDownload(
        items: List<EntryChapterList.Item>,
        startNow: Boolean,
        selection: EntryDownloadOptionSelection? = null,
    ) {
        val successState = successState ?: return

        screenModelScope.launchNonCancellable {
            val entry = successState.entry
            val chapters = items.map { it.chapter }
            if (selection != null) {
                entryDownloadInteraction.downloadWithOptions(entry, chapters, selection, startNow)
            } else {
                entryDownloadInteraction.download(entry, chapters, startNow)
            }

            if (!isFavorited && !successState.hasPromptedToAddBefore) {
                updateSuccessState { state ->
                    state.copy(hasPromptedToAddBefore = true)
                }
                val result = snackbarHostState.showSnackbar(
                    message = context.stringResource(MR.strings.snack_add_to_library),
                    actionLabel = context.stringResource(MR.strings.action_add),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && !isFavorited) {
                    toggleFavorite()
                }
            }
        }
    }

    fun runChapterDownloadActions(
        items: List<EntryChapterList.Item>,
        action: ChapterDownloadAction,
    ) {
        when (action) {
            ChapterDownloadAction.START -> {
                startDownload(items, false)
                if (items.any { it.downloadState == EntryDownloadState.ERROR }) {
                    entryDownloadInteraction.startDownloads()
                }
            }
            ChapterDownloadAction.START_NOW -> {
                startDownload(items, true)
            }
            ChapterDownloadAction.CANCEL -> {
                val chapterId = items.singleOrNull()?.id ?: return
                cancelDownload(chapterId)
            }
            ChapterDownloadAction.DELETE -> {
                deleteChapters(items.map { it.chapter })
            }
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val state = successState ?: return
        val candidateItems = getBulkDownloadCandidateItems()

        screenModelScope.launchNonCancellable {
            val result = entryDownloadInteraction.resolveBulkDownloadCandidates(
                entry = state.entry,
                action = action.toEntryBulkDownloadAction(),
                candidates = candidateItems.map(EntryChapterList.Item::chapter),
                memberEntryIds = state.memberIds,
            )
            if (result is EntryBulkDownloadCandidateResult.Supported && result.chapters.isNotEmpty()) {
                val itemsByChapterId = candidateItems.associateBy { it.chapter.id }
                startDownload(result.chapters.mapNotNull { itemsByChapterId[it.id] }, false)
            }
        }
    }

    fun supportsBulkDownload(): Boolean {
        return successState?.entry?.let(entryCapabilityInteraction::supportsBulkDownload) == true
    }

    private fun cancelDownload(chapterId: Long) {
        val chapterItem = successState?.chapters.orEmpty().firstOrNull { it.id == chapterId } ?: return
        val updatedStatus = entryDownloadInteraction.cancelQueuedDownload(chapterItem.entry.type, chapterId) ?: return
        updateDownloadState(updatedStatus)
    }

    fun markPreviousChapterRead(pointer: EntryChapter) {
        val state = successState ?: return
        val prevChapters = state.readingChapters.map { it.chapter }
        val pointerPos = prevChapters.indexOf(pointer)
        if (pointerPos != -1) markChaptersRead(prevChapters.take(pointerPos), true)
    }

    /**
     * Mark the selected chapter list as read/unread.
     * @param chapters the list of selected chapters.
     * @param read whether to mark chapters as read or unread.
     */
    fun markChaptersRead(chapters: List<EntryChapter>, read: Boolean) {
        toggleAllSelection(false)
        if (chapters.isEmpty()) return
        screenModelScope.launchIO {
            setReadStatus(read, chapters)

            if (!read || successState?.hasLoggedInTrackers == false || autoTrackState == AutoTrackState.NEVER) {
                return@launchIO
            }

            refreshTrackers()

            val tracks = getTracks.await(entryId)
            val maxChapterNumber = chapters.maxOf { it.chapterNumber }
            val shouldPromptTrackingUpdate = tracks.any { track -> maxChapterNumber > track.progress }

            if (!shouldPromptTrackingUpdate) return@launchIO
            if (autoTrackState == AutoTrackState.ALWAYS) {
                trackChapter.await(context, entryId, maxChapterNumber)
                withUIContext {
                    context.toast(context.stringResource(MR.strings.trackers_updated_summary, maxChapterNumber.toInt()))
                }
                return@launchIO
            }

            val result = snackbarHostState.showSnackbar(
                message = context.stringResource(MR.strings.confirm_tracker_update, maxChapterNumber.toInt()),
                actionLabel = context.stringResource(MR.strings.action_ok),
                duration = SnackbarDuration.Short,
                withDismissAction = true,
            )

            if (result == SnackbarResult.ActionPerformed) {
                trackChapter.await(context, entryId, maxChapterNumber)
            }
        }
    }

    private suspend fun setReadStatus(read: Boolean, chapters: List<EntryChapter>) {
        chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
            val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
            entryConsumptionInteraction.setConsumed(entry, memberChapters, read)
        }
    }

    private suspend fun refreshTrackers(
        refreshTracks: RefreshTracks = Injekt.get(),
    ) {
        refreshTracks.await(entryId)
            .filter { it.first != null }
            .forEach { (track, e) ->
                logcat(LogPriority.ERROR, e) {
                    "Failed to refresh track data entryId=$entryId for service ${track!!.id}"
                }
                withUIContext {
                    context.toast(
                        context.stringResource(
                            MR.strings.track_error,
                            track!!.name,
                            e.message ?: "",
                        ),
                    )
                }
            }
    }

    /**
     * Downloads the given list of chapters with the type-specific handler.
     *
     * @param items the list of chapter items to download.
     */
    private suspend fun downloadChapters(items: List<EntryChapterList.Item>) {
        items.groupBy { it.entry.id }
            .forEach { (_, chapterItems) ->
                val entry = chapterItems.first().entry
                val chapters = chapterItems.map { it.chapter }
                entryDownloadInteraction.download(entry, chapters)
            }
        toggleAllSelection(false)
    }

    private suspend fun getChapterItems(chapters: List<EntryChapter>): List<EntryChapterList.Item> {
        return chapters.map { chapter ->
            val entry = entryRepository.getEntryById(chapter.entryId) ?: return@map null
            EntryChapterList.Item(
                chapter = chapter,
                entry = entry,
                downloadState = EntryDownloadState.NOT_DOWNLOADED,
                downloadProgress = 0,
            )
        }.filterNotNull()
    }

    /**
     * Bookmarks the given list of chapters.
     * @param chapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(chapters: List<EntryChapter>, bookmarked: Boolean) {
        screenModelScope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryConsumptionInteraction.setBookmarked(entry, memberChapters, bookmarked)
            }
        }
        toggleAllSelection(false)
    }

    /**
     * Deletes the given list of chapter.
     *
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<EntryChapter>) {
        screenModelScope.launchNonCancellable {
            try {
                chapters.groupBy { it.entryId }
                    .forEach { (memberEntryId, memberChapters) ->
                        val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                        entryDownloadInteraction.delete(entry, memberChapters)
                    }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun downloadNewEntryChapters(chapters: List<EntryChapter>) {
        screenModelScope.launchNonCancellable {
            getChapterItems(chapters)
                .groupBy { it.entry.id }
                .forEach { (_, chapterItems) ->
                    val entry = chapterItems.first().entry
                    val chaptersToDownload = entryDownloadInteraction.filterAutoDownloadCandidates(
                        entry = entry,
                        chapters = chapterItems.map { it.chapter },
                    )
                    if (chaptersToDownload.isNotEmpty()) {
                        entryDownloadInteraction.download(entry, chaptersToDownload)
                    }
                }
        }
    }

    /**
     * Sets the read filter and requests an UI update.
     * @param state whether to display only unread chapters or all chapters.
     */
    fun setUnreadFilter(state: TriState) {
        val entry = successState?.entry ?: return

        val flag = when (state) {
            TriState.DISABLED -> Entry.SHOW_ALL
            TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_UNREAD
            TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_READ
        }
        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setUnreadFilter(flag))
            }
        }
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded chapters or all chapters.
     */
    fun setDownloadedFilter(state: TriState) {
        val entry = successState?.entry ?: return

        val flag = when (state) {
            TriState.DISABLED -> Entry.SHOW_ALL
            TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_DOWNLOADED
            TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_DOWNLOADED
        }

        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setDownloadedFilter(flag))
            }
        }
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked chapters or all chapters.
     */
    fun setBookmarkedFilter(state: TriState) {
        val entry = successState?.entry ?: return

        val flag = when (state) {
            TriState.DISABLED -> Entry.SHOW_ALL
            TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_BOOKMARKED
            TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_BOOKMARKED
        }

        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setBookmarkFilter(flag))
            }
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val entry = successState?.entry ?: return

        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setDisplayMode(mode))
            }
        }
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Long) {
        val entry = successState?.entry ?: return

        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setSortingModeOrFlipOrder(sort))
            }
        }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val entry = successState?.entry ?: return
        screenModelScope.launchNonCancellable {
            libraryPreferences.setChapterSettingsDefault(entry)
            if (applyToExisting) {
                updateMergedMemberEntries { memberEntry ->
                    setDefaultChapterFlags(memberEntry)
                }
            }
            snackbarHostState.showSnackbar(
                message = context.stringResource(entry.type.entryTypePresentation().settingsUpdatedLabel),
            )
        }
    }

    fun resetToDefaultSettings() {
        screenModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setDefaultChapterFlags(memberEntry)
            }
        }
    }

    private suspend fun setDefaultChapterFlags(entry: Entry) {
        val flags = computeDefaultChapterFlags(libraryPreferences)
        setEntryChapterFlags.await(entry.id, flags)
    }

    fun toggleSelection(
        item: EntryChapterList.Item,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        updateSuccessState { successState ->
            val newChapters = successState.processedChapters.toMutableList().apply {
                val selectedIndex = successState.processedChapters.indexOfFirst { it.chapter.id == item.chapter.id }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if ((selectedItem.selected && selected) || (!selectedItem.selected && !selected)) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedChapterIds.addOrRemove(item.id, selected)

                if (selected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1..<selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1)..<selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedChapterIds.add(inbetweenItem.id)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (!fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            successState.copy(chapters = newChapters)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { successState ->
            val newChapters = successState.chapters.map {
                selectedChapterIds.addOrRemove(it.id, selected)
                it.copy(selected = selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(chapters = newChapters)
        }
    }

    fun invertSelection() {
        updateSuccessState { successState ->
            val newChapters = successState.chapters.map {
                selectedChapterIds.addOrRemove(it.id, !it.selected)
                it.copy(selected = !it.selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(chapters = newChapters)
        }
    }

    // Chapters list - end

    // Track sheet - start

    private fun observeTrackers() {
        val entry = successState?.entry ?: return

        screenModelScope.launchIO {
            combine(
                getTracks.subscribe(entry.id).catch { logcat(LogPriority.ERROR, it) },
                trackerManager.loggedInTrackersFlow(),
            ) { entryTracks, loggedInTrackers ->
                val trackingSource = source?.let {
                    EntryTrackingSource.from(it, sourceManager.getDisplayInfo(entry.source))
                }
                // Show only if the service supports this entry's source
                val supportedTrackers = loggedInTrackers.filter {
                    entry.type in it.supportedEntryTypes &&
                        (
                            (it as? EnhancedTracker)?.let { tracker -> trackingSource?.let(tracker::accept) ?: false }
                                ?: true
                            )
                }
                val supportedTrackerIds = supportedTrackers.map { it.id }.toHashSet()
                val supportedTrackerTracks = entryTracks.filter { it.trackerId in supportedTrackerIds }
                supportedTrackerTracks.size to supportedTrackers.isNotEmpty()
            }
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { (trackingCount, hasLoggedInTrackers) ->
                    updateSuccessState {
                        it.copy(
                            trackingCount = trackingCount,
                            hasLoggedInTrackers = hasLoggedInTrackers,
                        )
                    }
                }
        }
    }

    private fun observeDuplicateCandidates() {
        val state = successState ?: return
        if (!state.isFromSource || state.entry.favorite) return

        duplicateObservationJob?.cancel()
        duplicateObservationJob = screenModelScope.launchIO {
            getDuplicateLibraryEntries.subscribe(
                entry = this@EntryScreenModel.state
                    .filter { it is State.Success }
                    .map { (it as State.Success).entry }
                    .distinctUntilChanged(),
                scope = screenModelScope,
            )
                .flowWithLifecycle(lifecycle)
                .collectLatest { duplicates ->
                    updateSuccessState {
                        if (!it.isFromSource || it.entry.favorite) {
                            it.copy(duplicateCandidates = emptyList())
                        } else {
                            it.copy(duplicateCandidates = duplicates)
                        }
                    }
                }
        }
    }

    // Track sheet - end

    sealed interface Dialog {
        data class ChangeCategory(
            val entry: Entry,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteChapters(val chapters: List<EntryChapter>) : Dialog
        data class DownloadSettings(
            val items: List<EntryChapterList.Item>,
            val startNow: Boolean,
            val options: EntryDownloadOptions?,
        ) : Dialog
        data class DuplicateEntry(val entry: Entry, val duplicates: List<DuplicateEntryCandidate>) : Dialog
        data class EditDisplayName(val entry: Entry, val initialValue: String) : Dialog
        data class EditMerge(
            val entry: Entry,
            val targetId: Long,
            val targetLocked: Boolean,
            val entries: ImmutableList<MergeEditorEntry>,
            val removedIds: Set<Long>,
            val libraryRemovalIds: Set<Long>,
            val categoryIds: List<Long>,
        ) : Dialog {
            val enabled: Boolean
                get() = entries.count { it.id !in (removedIds + libraryRemovalIds) } > 1
        }
        data class ManageMerge(
            val targetId: Long,
            val savedTargetId: Long,
            val members: ImmutableList<MergeMember>,
            val removableIds: ImmutableList<Long> = persistentListOf(),
            val libraryRemovalIds: ImmutableList<Long> = persistentListOf(),
        ) : Dialog
        data class Migrate(val target: Entry, val current: Entry) : Dialog
        data class RemoveMergedEntry(val members: ImmutableList<Entry>, val containsLocalEntry: Boolean) : Dialog
        data class SelectMergeTarget(
            val entry: Entry,
            val query: String = "",
            val targets: ImmutableList<MergeTarget>,
            val visibleTargets: ImmutableList<MergeTarget>,
        ) : Dialog
        data class SetFetchInterval(val entry: Entry) : Dialog
        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
    }

    fun dismissDialog() {
        updateSuccessState { it.copy(dialog = null) }
    }

    fun showDeleteChapterDialog(chapters: List<EntryChapter>) {
        updateSuccessState { it.copy(dialog = Dialog.DeleteChapters(chapters)) }
    }

    fun showSettingsDialog() {
        updateSuccessState { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun showTrackDialog() {
        updateSuccessState { it.copy(dialog = Dialog.TrackSheet) }
    }

    fun showDuplicateDialog() {
        val state = successState ?: return
        if (state.duplicateCandidates.isEmpty()) return
        updateSuccessState { it.copy(dialog = Dialog.DuplicateEntry(state.entry, state.duplicateCandidates)) }
    }

    fun showMergeTargetPicker() {
        val state = successState ?: return
        screenModelScope.launchIO {
            val excludedIds = state.mergeGroupMemberIds.toSet()
            val libraryEntries = entryRepository.getLibraryEntries()
            val targets = buildMergeTargets(
                libraryItems = libraryEntries.toLibraryItems(),
                sourceManager = sourceManager,
                entryType = state.entry.type,
                excludedEntryIds = excludedIds,
            )
            if (targets.isEmpty()) return@launchIO
            val query = buildMergeTargetQuery(state.entry.displayTitle, duplicatePreferences)
            val visibleTargets = rankMergeTargets(targets, query).toImmutableList()
            updateSuccessState {
                it.copy(
                    dialog = Dialog.SelectMergeTarget(
                        entry = state.entry,
                        query = query,
                        targets = targets,
                        visibleTargets = visibleTargets,
                    ),
                )
            }
        }
    }

    fun updateMergeTargetQuery(query: String) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.SelectMergeTarget ?: return@updateSuccessState state
            val visibleTargets = rankMergeTargets(dialog.targets, query).toImmutableList()
            state.copy(dialog = dialog.copy(query = query, visibleTargets = visibleTargets))
        }
    }

    fun openMergeEditor(targetId: Long) {
        val dialog = successState?.dialog as? Dialog.SelectMergeTarget ?: return
        screenModelScope.launchIO {
            val target = dialog.targets.firstOrNull { it.id == targetId } ?: return@launchIO
            updateSuccessState {
                it.copy(dialog = createMergeEditorDialog(dialog.entry, target))
            }
        }
    }

    fun moveMergeEntry(fromIndex: Int, toIndex: Int) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.EditMerge ?: return@updateSuccessState state
            val entries = dialog.entries.toMutableList()
            if (fromIndex !in entries.indices || toIndex !in entries.indices) return@updateSuccessState state
            val entry = entries.removeAt(fromIndex)
            entries.add(toIndex, entry)
            state.copy(dialog = dialog.copy(entries = entries.toImmutableList()))
        }
    }

    fun setMergeTarget(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.EditMerge ?: return@updateSuccessState state
            if (dialog.targetLocked || dialog.entries.none { it.id == entryId }) return@updateSuccessState state
            state.copy(
                dialog = dialog.copy(
                    targetId = entryId,
                    removedIds = dialog.removedIds - entryId,
                    libraryRemovalIds = dialog.libraryRemovalIds - entryId,
                ),
            )
        }
    }

    fun toggleMergeEntryRemoval(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.EditMerge ?: return@updateSuccessState state
            val entry = dialog.entries.firstOrNull { it.id == entryId } ?: return@updateSuccessState state
            if (!entry.isRemovable || entryId == dialog.targetId) return@updateSuccessState state
            val removedIds = dialog.removedIds.toMutableSet().apply {
                if (!add(entryId)) remove(entryId)
            }
            state.copy(dialog = dialog.copy(removedIds = removedIds))
        }
    }

    fun toggleMergeEntryLibraryRemoval(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.EditMerge ?: return@updateSuccessState state
            val entry = dialog.entries.firstOrNull { it.id == entryId } ?: return@updateSuccessState state
            if (!entry.isRemovable || entryId == dialog.targetId) return@updateSuccessState state
            val libraryRemovalIds = dialog.libraryRemovalIds.toMutableSet().apply {
                if (!add(entryId)) remove(entryId)
            }
            state.copy(dialog = dialog.copy(libraryRemovalIds = libraryRemovalIds))
        }
    }

    fun confirmMerge() {
        val dialog = successState?.dialog as? Dialog.EditMerge ?: return
        screenModelScope.launchIO {
            val targetEntry = getEntryOrNull(dialog.targetId) ?: return@launchIO
            val remoteEntry = networkToLocalEntry(dialog.entry)
            ensureFavorite(remoteEntry, targetEntry, dialog.categoryIds)

            val orderedIds = dialog.entries
                .filterNot { it.id in (dialog.removedIds + dialog.libraryRemovalIds) }
                .map(MergeEditorEntry::id)
                .distinct()

            if (orderedIds.size > 1) {
                updateMergedEntry.awaitMerge(dialog.targetId, orderedIds)
            }
            removeMembersFromLibrary(dialog.libraryRemovalIds)
            dismissDialog()
        }
    }

    fun showCoverDialog() {
        updateSuccessState { it.copy(dialog = Dialog.FullCover) }
    }

    fun showMigrateDialog(duplicate: Entry) {
        val entry = successState?.entry ?: return
        updateSuccessState { it.copy(dialog = Dialog.Migrate(target = entry, current = duplicate)) }
    }

    private fun showRemoveMergedEntryDialog() {
        val state = successState ?: return
        if (!state.isMerged) return
        screenModelScope.launchIO {
            val members = state.memberIds.mapNotNull { memberId -> getEntryOrNull(memberId) }
                .toImmutableList()
            updateSuccessState {
                it.copy(
                    dialog = Dialog.RemoveMergedEntry(
                        members = members,
                        containsLocalEntry = members.any { it.isLocal() },
                    ),
                )
            }
        }
    }

    fun showEditDisplayNameDialog() {
        val state = successState ?: return
        updateSuccessState {
            it.copy(
                dialog = Dialog.EditDisplayName(
                    entry = state.entry,
                    initialValue = state.entry.displayTitle,
                ),
            )
        }
    }

    fun showManageMergeDialog() {
        val state = successState ?: return
        if (!state.isPartOfMerge) return
        screenModelScope.launchIO {
            val members = state.mergeGroupMemberIds.mapNotNull { memberId ->
                getEntryOrNull(memberId)?.let { entry ->
                    MergeMember(
                        id = entry.id,
                        entry = entry,
                        subtitle = getMergeSubtitle(entry),
                    )
                }
            }
                .toImmutableList()
            updateSuccessState {
                it.copy(
                    dialog = Dialog.ManageMerge(
                        targetId = state.mergeTargetId,
                        savedTargetId = state.mergeTargetId,
                        members = members,
                    ),
                )
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        val entry = successState?.entry ?: return
        screenModelScope.launchIO {
            entryRepository.updateDisplayName(entry.id, displayName.toStoredDisplayName(entry.title))
            dismissDialog()
        }
    }

    fun removeMergedMembers(entryIds: List<Long>) {
        val state = successState ?: return
        val dialog = state.dialog as? Dialog.ManageMerge
        screenModelScope.launchIO {
            if (entryIds.isEmpty()) return@launchIO
            if (dialog != null) {
                saveManageMerge(dialog, entryIds)
            } else {
                updateMergedEntry.awaitRemoveMembers(state.mergeTargetId, entryIds)
            }
            dismissDialog()
        }
    }

    fun reorderMergeMembers(fromIndex: Int, toIndex: Int) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.ManageMerge ?: return@updateSuccessState state
            if (fromIndex !in dialog.members.indices ||
                toIndex !in dialog.members.indices
            ) {
                return@updateSuccessState state
            }
            val reordered = dialog.members.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            val reorderedRemovalIds = reordered.mapNotNull { member ->
                member.id.takeIf { it in dialog.removableIds }
            }.toImmutableList()
            val reorderedLibraryRemovalIds = reordered.mapNotNull { member ->
                member.id.takeIf { it in dialog.libraryRemovalIds }
            }.toImmutableList()
            state.copy(
                dialog = dialog.copy(
                    members = reordered.toImmutableList(),
                    removableIds = reorderedRemovalIds,
                    libraryRemovalIds = reorderedLibraryRemovalIds,
                ),
            )
        }
    }

    fun setManageMergeTarget(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.ManageMerge ?: return@updateSuccessState state
            if (dialog.members.none { it.id == entryId }) return@updateSuccessState state

            val updatedRemovals = dialog.removableIds.filterNot { it == entryId }.toImmutableList()
            val updatedLibraryRemovals = dialog.libraryRemovalIds.filterNot { it == entryId }.toImmutableList()
            state.copy(
                dialog = dialog.copy(
                    targetId = entryId,
                    removableIds = updatedRemovals,
                    libraryRemovalIds = updatedLibraryRemovals,
                ),
            )
        }
    }

    fun toggleMergedMemberRemoval(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.ManageMerge ?: return@updateSuccessState state
            if (entryId == dialog.targetId || dialog.members.none { it.id == entryId }) return@updateSuccessState state

            val updatedIds = dialog.removableIds.toMutableList().apply {
                if (entryId in this) {
                    remove(entryId)
                } else {
                    add(entryId)
                }
            }.toImmutableList()

            state.copy(dialog = dialog.copy(removableIds = updatedIds))
        }
    }

    fun toggleMergedMemberLibraryRemoval(entryId: Long) {
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.ManageMerge ?: return@updateSuccessState state
            if (entryId == dialog.targetId || dialog.members.none { it.id == entryId }) return@updateSuccessState state

            val updatedIds = dialog.libraryRemovalIds.toMutableList().apply {
                if (entryId in this) {
                    remove(entryId)
                } else {
                    add(entryId)
                }
            }.toImmutableList()

            state.copy(dialog = dialog.copy(libraryRemovalIds = updatedIds))
        }
    }

    fun saveMergeOrder() {
        val dialog = successState?.dialog as? Dialog.ManageMerge ?: return
        screenModelScope.launchIO {
            saveManageMerge(dialog, dialog.removableIds + dialog.libraryRemovalIds)
            removeMembersFromLibrary(dialog.libraryRemovalIds)
            dismissDialog()
        }
    }

    private suspend fun saveManageMerge(dialog: Dialog.ManageMerge, entryIdsToRemove: Collection<Long>) {
        val remainingIds = dialogRemainingIds(dialog, entryIdsToRemove)
        val targetId = resolveManageMergeTargetId(dialog.targetId, remainingIds)

        if (targetId != null && remainingIds.size > 1) {
            updateMergedEntry.awaitMerge(targetId, remainingIds)
        } else {
            updateMergedEntry.awaitDeleteGroup(dialog.targetId)
        }
    }

    private suspend fun removeMembersFromLibrary(entryIds: Collection<Long>) {
        entryIds.distinct().forEach { memberEntryId ->
            val entry = getEntryOrNull(memberEntryId) ?: return@forEach
            if (setEntryFavorite.await(entry.id, false)) {
                if (entry.removeCovers() != entry) {
                    updateEntry.awaitUpdateCoverLastModified(entry.id)
                }
            }
            entryDownloadInteraction.deleteEntryDownloads(entry)
        }
    }

    fun removeMergedEntry(entries: List<Entry>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        val state = successState ?: return
        screenModelScope.launchIO {
            if (deleteFromLibrary) {
                updateMergedEntry.awaitDeleteGroup(state.mergeTargetId)
                entries.forEach { entry ->
                    if (setEntryFavorite.await(entry.id, false) && entry.removeCovers() != entry) {
                        updateEntry.awaitUpdateCoverLastModified(entry.id)
                    }
                }
            }

            if (deleteChapters) {
                entries.forEach { entry ->
                    entryDownloadInteraction.deleteEntryDownloads(entry)
                }
            }

            dismissDialog()
        }
    }

    fun unmergeAll() {
        val state = successState ?: return
        screenModelScope.launchIO {
            updateMergedEntry.awaitDeleteGroup(state.mergeTargetId)
            dismissDialog()
        }
    }

    suspend fun getVisibleEntryId(entryId: Long): Long {
        if (bypassMerge) return entryId
        return getMergedEntry.awaitVisibleTargetId(entryId)
    }

    fun setExcludedScanlators(excludedScanlators: Set<String>) {
        val state = successState ?: return
        if (!state.childGroupFilterSupported) return

        screenModelScope.launchIO {
            entryChildGroupFilterInteraction.setExcludedGroups(
                entry = state.entry,
                memberIds = getDisplayedMemberIds(),
                excluded = excludedScanlators,
            )
        }
    }

    private suspend fun getDisplayedMemberIds(): List<Long> {
        if (bypassMerge) return listOf(entryId)
        return getMergeGroupMemberIds()
    }

    private suspend fun getMergeGroupMemberIds(): List<Long> {
        return getMergedEntry.awaitGroupByEntryId(entryId)
            .sortedBy { it.position }
            .map { it.entryId }
            .ifEmpty { listOf(entryId) }
    }

    private suspend fun getDisplayedChapters(entry: Entry? = successState?.entry): List<EntryChapter> {
        val memberIds = getDisplayedMemberIds()
        val applyScanlatorFilter = entry?.let(entryChildGroupFilterInteraction::shouldApplyFilter) == true
        return if (memberIds.size == 1) {
            entryChapterRepository.getChaptersByEntryIdAwait(memberIds.single(), applyScanlatorFilter)
        } else {
            memberIds.flatMap { memberId ->
                entryChapterRepository.getChaptersByEntryIdAwait(memberId, applyScanlatorFilter)
            }
        }
    }

    private suspend fun getAvailableChildGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        if (!entryChildGroupFilterInteraction.supports(entry)) return emptySet()
        return entryChildGroupFilterInteraction.availableGroups(entry, memberIds)
    }

    private suspend fun getExcludedChildGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        if (!entryChildGroupFilterInteraction.supports(entry)) return emptySet()
        return entryChildGroupFilterInteraction.excludedGroups(entry, memberIds)
    }

    private suspend fun entryAndChaptersFlow(): Flow<Pair<Entry, List<EntryChapter>>> {
        return combine(
            getEntry.subscribe(entryId),
            getMergedEntry.subscribeGroupByEntryId(entryId),
        ) { entry, mergeGroup -> entry to mergeGroup }
            .flatMapLatest { (entry, mergeGroup) ->
                val memberIds = if (bypassMerge) {
                    listOf(entryId)
                } else {
                    mergeGroup.map { it.entryId }.ifEmpty { listOf(entryId) }
                }
                if (memberIds.size == 1) {
                    entryChapterRepository.getChaptersByEntryId(memberIds.single())
                } else {
                    entryChapterRepository.getChaptersByEntryIds(memberIds)
                }.map { chapters -> entry to chapters }
            }
    }

    private suspend fun getMergePresentation(entry: Entry): MergePresentation {
        val mergeGroupMemberIds = getMergeGroupMemberIds().toImmutableList()
        val memberIds = if (bypassMerge) {
            persistentListOf(entry.id)
        } else {
            mergeGroupMemberIds
        }
        val members = memberIds.mapNotNull { memberId -> getEntryOrNull(memberId) }
        return MergePresentation(
            sourceName = getSourceName(entry, memberIds),
            memberIds = memberIds,
            memberTitleById = members.associate { it.id to it.displayTitle },
            mergedMemberTitles = members.map { it.displayTitle }.filter { it.isNotBlank() }.toImmutableList(),
            mergeTargetId = getMergedEntry.awaitVisibleTargetId(entry.id),
            mergeGroupMemberIds = mergeGroupMemberIds,
        )
    }

    private suspend fun updateMergedMemberEntries(block: suspend (Entry) -> Unit) {
        getDisplayedMemberIds().forEach { memberId ->
            getEntryOrNull(memberId)?.let { memberEntry ->
                block(memberEntry)
            }
        }
    }

    private suspend fun getMergeMembers(): List<Entry> {
        return getDisplayedMemberIds().mapNotNull { memberId -> getEntryOrNull(memberId) }
    }

    private suspend fun getMergeMembersNeedingRefresh(): List<Entry> {
        val members = getMergeMembers()
        if (members.size <= 1) return members

        return members.filter { memberEntry ->
            !memberEntry.initialized ||
                entryChapterRepository.getChaptersByEntryIdAwait(memberEntry.id, applyScanlatorFilter = false).isEmpty()
        }
    }

    private suspend fun getMembersToRefreshFromSource(manualFetch: Boolean): List<Entry> {
        val mergedMembers = getMergeMembers()
        if (mergedMembers.size <= 1) {
            val fallbackEntry = successState?.entry ?: getEntryOrNull(entryId)
            return listOfNotNull(fallbackEntry)
        }

        return if (manualFetch) {
            mergedMembers
        } else {
            getMergeMembersNeedingRefresh()
        }
    }

    private suspend fun getEntryOrNull(id: Long): Entry? {
        return runCatching { entryRepository.getEntryById(id) }.getOrNull()
    }

    private suspend fun createMergeEditorDialog(
        entry: Entry,
        target: MergeTarget,
    ): Dialog.EditMerge {
        val localEntry = networkToLocalEntry(entry)
        val orderedMembers = if (target.isMerged) {
            val membersById = target.memberEntries.associateBy(Entry::id)
            getMergedEntry.awaitGroupByTargetId(target.id)
                .sortedBy { it.position }
                .mapNotNull { merge -> membersById[merge.entryId] }
                .ifEmpty { target.memberEntries }
        } else {
            target.memberEntries
        }

        val entries = buildList {
            if (target.isMerged && orderedMembers.none { it.id == localEntry.id }) {
                add(
                    MergeEditorEntry(
                        id = localEntry.id,
                        entry = localEntry,
                        subtitle = getMergeSubtitle(localEntry) + " • New",
                        isRemovable = false,
                    ),
                )
            }
            orderedMembers.forEach { member ->
                add(
                    MergeEditorEntry(
                        id = member.id,
                        entry = member,
                        subtitle = getMergeSubtitle(member),
                        isRemovable = true,
                        isMember = true,
                    ),
                )
            }
            if (!target.isMerged && none { it.id == localEntry.id }) {
                add(
                    MergeEditorEntry(
                        id = localEntry.id,
                        entry = localEntry,
                        subtitle = getMergeSubtitle(localEntry) + " • New",
                        isRemovable = false,
                    ),
                )
            }
        }.toImmutableList()

        return Dialog.EditMerge(
            entry = localEntry,
            targetId = target.id,
            targetLocked = false,
            entries = entries,
            removedIds = emptySet(),
            libraryRemovalIds = emptySet(),
            categoryIds = target.categoryIds,
        )
    }

    private suspend fun ensureFavorite(entry: Entry, targetEntry: Entry, categoryIds: List<Long>) {
        if (!entry.favorite) {
            setDefaultChapterFlags(entry)
            addTracks.bindEnhancedTrackers(
                entry = entry,
                source = EntryTrackingSource.from(source ?: return, sourceManager.getDisplayInfo(entry.source)),
            )
            entryRepository.update(
                entry.copy(
                    favorite = true,
                    dateAdded = Instant.now().toEpochMilli(),
                ),
            )
        }

        val appliedCategoryIds = if (categoryIds.isNotEmpty()) categoryIds else getEntryCategoryIds(targetEntry.id)
        setEntryCategories.await(entry.id, appliedCategoryIds)
    }

    private fun getMergeSubtitle(entry: Entry): String {
        val sourceName = sourceManager.getDisplayInfo(entry.source).name
        val creator = entry.author?.takeIf { it.isNotBlank() }
            ?: entry.artist?.takeIf { it.isNotBlank() }
        return buildString {
            append(sourceName)
            if (creator != null && !creator.equals(sourceName, ignoreCase = true)) {
                append(" • ")
                append(creator)
            }
        }
    }

    private fun getSourceName(entry: Entry, memberIds: List<Long>): String {
        return if (memberIds.size > 1) {
            context.stringResource(MR.strings.multi_lang)
        } else {
            sourceManager.getDisplayInfo(entry.source).getDisplayNameForEntryInfo()
        }
    }

    /**
     * TODO(Phase 7.5): Build real [LibraryItem]s with merged-member info so the merge-target
     * picker correctly surfaces already-merged library entries.
     */
    private fun List<Entry>.toLibraryItems(): List<LibraryItem> {
        return map { entry ->
            LibraryItem(
                entry = entry,
                categories = emptyList(),
                sourceName = "",
                sourceLanguage = "",
                sourceItemOrientation = eu.kanade.tachiyomi.source.entry.EntryItemOrientation.VERTICAL,
                displaySourceId = entry.source,
                sourceIds = setOf(entry.source),
                isLocal = entry.isLocal(),
                isMerged = false,
                memberEntryIds = emptyList(),
                memberEntries = listOf(entry),
                progress = ProgressState(
                    totalCount = 0,
                    consumedCount = 0,
                    bookmarkCount = 0,
                    hasStarted = false,
                ),
                latestUpload = 0L,
                lastRead = 0L,
                continueEntryId = null,
                downloadCount = 0,
            )
        }
    }

    private fun Entry.removeCovers(): Entry {
        if (isLocal()) return this
        return if (coverCache.deleteFromCache(this, true) > 0) {
            copy(coverLastModified = Instant.now().toEpochMilli())
        } else {
            this
        }
    }

    private data class MergePresentation(
        val sourceName: String,
        val memberIds: ImmutableList<Long>,
        val memberTitleById: Map<Long, String>,
        val mergedMemberTitles: ImmutableList<String>,
        val mergeTargetId: Long,
        val mergeGroupMemberIds: ImmutableList<Long>,
    )

    @Immutable
    data class MergeMember(
        val id: Long,
        val entry: Entry,
        val subtitle: String,
    )

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object Error : State

        @Immutable
        data class Success(
            val entry: Entry,
            val source: UnifiedSource?,
            val sourceName: String,
            val isSourceMissing: Boolean,
            val memberIds: ImmutableList<Long>,
            val memberTitleById: Map<Long, String>,
            val mergedMemberTitles: ImmutableList<String>,
            val mergeTargetId: Long,
            val mergeGroupMemberIds: ImmutableList<Long>,
            val isFromSource: Boolean,
            val chapters: List<EntryChapterList.Item>,
            val childProgressLabels: Map<Long, EntryChildProgressLabel> = emptyMap(),
            val childListInteraction: EntryChildListInteraction,
            val childGroupFilterSupported: Boolean,
            val availableScanlators: Set<String>,
            val excludedScanlators: Set<String>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val duplicateCandidates: List<DuplicateEntryCandidate> = emptyList(),
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val hideMissingChapters: Boolean = false,
        ) : State {
            val processedChapters by lazy {
                chapters.applyFilters(entry, childListInteraction).toList()
            }

            val readingChapters by lazy {
                processedChapters.sortedForReading(entry, memberIds, childListInteraction)
            }

            val isAnySelected by lazy {
                chapters.fastAny { it.selected }
            }

            val chapterListItems by lazy {
                processedChapters.toChapterListRows(
                    entry = entry,
                    memberIds = memberIds,
                    memberTitleById = memberTitleById,
                    childListInteraction = childListInteraction,
                    includeMissingCounts = !hideMissingChapters,
                )
            }

            val scanlatorFilterActive: Boolean
                get() = childGroupFilterSupported &&
                    excludedScanlators.intersect(availableScanlators).isNotEmpty()

            val filterActive: Boolean
                get() = scanlatorFilterActive || entry.chaptersFiltered()

            val isPartOfMerge: Boolean
                get() = mergeGroupMemberIds.size > 1

            val isMerged: Boolean
                get() = memberIds.size > 1

            val showMergeNotice: Boolean
                get() = isPartOfMerge && !isMerged && mergeTargetId != entry.id

            /**
             * Applies the view filters to the list of chapters obtained from the database.
             * @return an observable of the list of chapters filtered and sorted.
             */
            private fun List<EntryChapterList.Item>.applyFilters(
                entry: Entry,
                childListInteraction: EntryChildListInteraction,
            ): List<EntryChapterList.Item> {
                val isLocalEntry = entry.isLocal()
                val unreadFilter = entry.unreadFilter
                val downloadedFilter = entry.downloadedFilter
                val bookmarkedFilter = entry.bookmarkedFilter
                val filtered = asSequence()
                    .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
                    .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
                    .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalEntry } }
                    .toList()
                return filtered.sortedForDisplay(entry, memberIds, childListInteraction)
            }
        }
    }

    @Immutable
    data class EntryPreviewState(
        val isExpanded: Boolean = false,
        val isLoading: Boolean = false,
        val error: Throwable? = null,
        val chapterId: Long? = null,
        val pages: List<PreviewPage> = emptyList(),
        val pageCount: Int = 5,
    ) {
        val hasLoadedContent: Boolean
            get() = chapterId != null && !isLoading && error == null
    }

    @Immutable
    data class PreviewPage(
        val page: EntryPreviewPage,
    )
}

@Immutable
sealed class EntryChapterList {
    @Immutable
    data class MemberHeader(
        val entryId: Long,
        val title: String,
    ) : EntryChapterList()

    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EntryChapterList()

    @Immutable
    data class Item(
        val chapter: EntryChapter,
        val entry: Entry,
        val downloadState: EntryDownloadState,
        val downloadProgress: Int,
        val selected: Boolean = false,
    ) : EntryChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == EntryDownloadState.DOWNLOADED
    }
}

private fun List<EntryChapterList.Item>.previewReadingChapters(
    entry: Entry,
    memberIds: List<Long>,
    childListInteraction: EntryChildListInteraction,
): List<EntryChapterList.Item> {
    return applyFiltersForPreview(entry)
        .sortedForReading(entry, memberIds, childListInteraction)
}

private fun List<EntryChapterList.Item>.applyFiltersForPreview(
    entry: Entry,
): List<EntryChapterList.Item> {
    val isLocalEntry = entry.isLocal()
    val unreadFilter = entry.unreadFilter
    val downloadedFilter = entry.downloadedFilter
    val bookmarkedFilter = entry.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalEntry } }
        .toList()
}

private fun List<EntryChapterList.Item>.toChapterListRows(
    entry: Entry,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    childListInteraction: EntryChildListInteraction,
    includeMissingCounts: Boolean,
): List<EntryChapterList> {
    val itemByChapterId = associateBy { it.chapter.id }
    return childListInteraction.buildDisplayList(
        EntryChildListRequest(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            includeMissingCounts = includeMissingCounts,
        ),
    ).mapNotNull { row ->
        when (row) {
            is EntryChildListRow.Child -> itemByChapterId[row.chapter.id]
            is EntryChildListRow.MemberHeader -> EntryChapterList.MemberHeader(
                entryId = row.entryId,
                title = row.title,
            )
            is EntryChildListRow.MissingCount -> EntryChapterList.MissingCount(
                id = row.id,
                count = row.count,
            )
        }
    }
}

private fun List<EntryChapterList.Item>.sortedForReading(
    entry: Entry,
    memberIds: List<Long>,
    childListInteraction: EntryChildListInteraction,
): List<EntryChapterList.Item> {
    return sortedWithChapterOrder(
        childListInteraction.sortedForReading(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
        ),
    )
}

private fun List<EntryChapterList.Item>.sortedForDisplay(
    entry: Entry,
    memberIds: List<Long>,
    childListInteraction: EntryChildListInteraction,
): List<EntryChapterList.Item> {
    return sortedWithChapterOrder(
        childListInteraction.sortedForDisplay(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
        ),
    )
}

private fun List<EntryChapterList.Item>.sortedWithChapterOrder(
    chapters: List<EntryChapter>,
): List<EntryChapterList.Item> {
    val orderedIds = chapters.mapIndexed { index, chapter -> chapter.id to index }.toMap()
    return sortedBy { item ->
        orderedIds[item.chapter.id] ?: Int.MAX_VALUE
    }
}

private fun DownloadAction.toEntryBulkDownloadAction(): EntryBulkDownloadAction {
    return when (this) {
        DownloadAction.NEXT_1_CHAPTER -> EntryBulkDownloadAction.next(1)
        DownloadAction.NEXT_5_CHAPTERS -> EntryBulkDownloadAction.next(5)
        DownloadAction.NEXT_10_CHAPTERS -> EntryBulkDownloadAction.next(10)
        DownloadAction.NEXT_25_CHAPTERS -> EntryBulkDownloadAction.next(25)
        DownloadAction.UNREAD_CHAPTERS -> EntryBulkDownloadAction.unread
        DownloadAction.BOOKMARKED_CHAPTERS -> EntryBulkDownloadAction.bookmarked
    }
}

internal fun <DownloadChanges, DownloadQueue> mergeAwareEntryAndChaptersFlow(
    entryAndChaptersFlow: Flow<Pair<Entry, List<EntryChapter>>>,
    mergeGroupFlow: Flow<List<EntryMerge>>,
    downloadChangesFlow: Flow<DownloadChanges>,
    downloadQueueFlow: Flow<DownloadQueue>,
): Flow<Pair<Entry, List<EntryChapter>>> {
    return combine(
        entryAndChaptersFlow.distinctUntilChanged(),
        mergeGroupFlow.distinctUntilChanged(),
        downloadChangesFlow,
        downloadQueueFlow,
    ) { entryAndChapters, _, _, _ -> entryAndChapters }
}

internal fun dialogRemainingIds(
    dialog: EntryScreenModel.Dialog.ManageMerge,
    entryIdsToRemove: Collection<Long>,
): List<Long> {
    val entryIdsToRemoveSet = entryIdsToRemove.toSet()
    return dialog.members.map { it.id }
        .filterNot(entryIdsToRemoveSet::contains)
}

internal fun resolveManageMergeTargetId(targetId: Long, remainingIds: List<Long>): Long? {
    return remainingIds.firstOrNull { it == targetId } ?: remainingIds.firstOrNull()
}

private fun Long.setUnreadFilter(flag: Long): Long {
    return this and Entry.CHAPTER_UNREAD_MASK.inv() or flag
}

private fun Long.setDownloadedFilter(flag: Long): Long {
    return this and Entry.CHAPTER_DOWNLOADED_MASK.inv() or flag
}

private fun Long.setBookmarkFilter(flag: Long): Long {
    return this and Entry.CHAPTER_BOOKMARKED_MASK.inv() or flag
}

private fun Long.setDisplayMode(mode: Long): Long {
    return this and Entry.CHAPTER_DISPLAY_MASK.inv() or mode
}

private fun Long.setSortingModeOrFlipOrder(sort: Long): Long {
    return if (this and Entry.CHAPTER_SORTING_MASK == sort) {
        this xor Entry.CHAPTER_SORT_DIR_MASK
    } else {
        this and Entry.CHAPTER_SORTING_MASK.inv() or sort
    }
}

internal fun Entry.isLocal(): Boolean = source == LocalSource.ID

private fun computeDefaultChapterFlags(libraryPreferences: LibraryPreferences): Long {
    return Entry.SHOW_ALL or
        libraryPreferences.sortChapterBySourceOrNumber.get() or
        libraryPreferences.displayChapterByNameOrNumber.get() or
        libraryPreferences.sortChapterByAscendingOrDescending.get()
}
