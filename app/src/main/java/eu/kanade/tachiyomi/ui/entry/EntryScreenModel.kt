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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryBookmarkFeature
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadRequest
import mihon.entry.interactions.EntryBulkDownloadResolutionResult
import mihon.entry.interactions.EntryChildGroupFilterFeature
import mihon.entry.interactions.EntryChildGroupFilterObservationResult
import mihon.entry.interactions.EntryChildGroupFilterResult
import mihon.entry.interactions.EntryChildGroupFilterScope
import mihon.entry.interactions.EntryChildGroupFilterStateResult
import mihon.entry.interactions.EntryChildListFeature
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListResult
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildOrderResult
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryChildProgressResult
import mihon.entry.interactions.EntryConsumptionFeature
import mihon.entry.interactions.EntryContinueFeature
import mihon.entry.interactions.EntryDownloadActionFeature
import mihon.entry.interactions.EntryDownloadActionRequest
import mihon.entry.interactions.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.EntryDownloadOptionSelection
import mihon.entry.interactions.EntryDownloadOptions
import mihon.entry.interactions.EntryDownloadOptionsFeature
import mihon.entry.interactions.EntryDownloadOptionsResolution
import mihon.entry.interactions.EntryDownloadRuntimeFeature
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.EntryLibraryAddRequest
import mihon.entry.interactions.EntryLibraryAddResult
import mihon.entry.interactions.EntryLibraryCategorySelection
import mihon.entry.interactions.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.EntryLibraryMembershipFeature
import mihon.entry.interactions.EntryLibraryRemovalResult
import mihon.entry.interactions.EntryMergeCandidateFeature
import mihon.entry.interactions.EntryMergeCommitIntent
import mihon.entry.interactions.EntryMergeDissolveIntent
import mihon.entry.interactions.EntryMergeEditReference
import mihon.entry.interactions.EntryMergeEditorEntry
import mihon.entry.interactions.EntryMergeEditorEntryReference
import mihon.entry.interactions.EntryMergeEditorProjection
import mihon.entry.interactions.EntryMergeExecutionResult
import mihon.entry.interactions.EntryMergeFeature
import mihon.entry.interactions.EntryMergeMemberPreparationIntent
import mihon.entry.interactions.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.EntryMergeNavigationFeature
import mihon.entry.interactions.EntryMergePreparationResult
import mihon.entry.interactions.EntryMergePrepareIntent
import mihon.entry.interactions.EntryMergeRemoveEntriesIntent
import mihon.entry.interactions.EntryMergeSubject
import mihon.entry.interactions.EntryMigrationAvailability
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationSelectionResult
import mihon.entry.interactions.EntryMigrationSubject
import mihon.entry.interactions.EntryPreviewAvailability
import mihon.entry.interactions.EntryPreviewChildCandidate
import mihon.entry.interactions.EntryPreviewConfig
import mihon.entry.interactions.EntryPreviewContext
import mihon.entry.interactions.EntryPreviewFeature
import mihon.entry.interactions.EntryPreviewHandle
import mihon.entry.interactions.EntryPreviewLoadRequest
import mihon.entry.interactions.EntryPreviewLoadResult
import mihon.entry.interactions.EntryPreviewOpenTargetResult
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import mihon.entry.interactions.EntrySourceRefreshFailure
import mihon.entry.interactions.EntrySourceRefreshFeature
import mihon.entry.interactions.EntrySourceRefreshRequest
import mihon.entry.interactions.EntrySourceRefreshResult
import mihon.entry.interactions.EntryTrackingAvailability
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingProgressInspection
import mihon.entry.interactions.EntryTrackingProgressSynchronizationResult
import mihon.entry.interactions.EntryTrackingRefreshResult
import mihon.entry.interactions.EntryTrackingSession
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
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryChapterFlags
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.util.applyFilter
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val entryId: Long,
    private val isFromSource: Boolean,
    private val bypassMerge: Boolean = false,
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val duplicatePreferences: tachiyomi.domain.library.service.DuplicatePreferences = Injekt.get(),
    trackPreferences: TrackPreferences = Injekt.get(),
    private val downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    private val entryDownloadActionFeature: EntryDownloadActionFeature = Injekt.get(),
    private val entryDownloadOptionsFeature: EntryDownloadOptionsFeature = Injekt.get(),
    private val downloadMaintenance: EntryDownloadMaintenanceFeature = Injekt.get(),
    private val entryMigrationFeature: EntryMigrationFeature = Injekt.get(),
    private val entryConsumptionFeature: EntryConsumptionFeature = Injekt.get(),
    private val entryBookmarkFeature: EntryBookmarkFeature = Injekt.get(),
    private val entryContinueFeature: EntryContinueFeature = Injekt.get(),
    private val entryPreviewFeature: EntryPreviewFeature = Injekt.get(),
    private val entryChildListFeature: EntryChildListFeature = Injekt.get(),
    private val entryChildGroupFilterFeature: EntryChildGroupFilterFeature = Injekt.get(),
    private val entryMergeFeature: EntryMergeFeature = Injekt.get(),
    private val entryMergeCandidateFeature: EntryMergeCandidateFeature = Injekt.get(),
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature = Injekt.get(),
    private val entryMergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature = Injekt.get(),
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getEntryWithChapters: GetEntryWithChapters = Injekt.get(),
    private val setEntryChapterFlags: SetEntryChapterFlags = Injekt.get(),
    private val setEntryCategories: SetEntryCategories = Injekt.get(),
    private val updateEntry: UpdateEntry = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val categoryRepository: CategoryRepository = Injekt.get(),
    private val sourceRefreshFeature: EntrySourceRefreshFeature = Injekt.get(),
    private val entryTrackingFeature: EntryTrackingFeature = Injekt.get(),
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

    private val filteredChapters: List<EntryChapterList.Item>?
        get() = successState?.processedChapters

    val chapterSwipeStartAction = libraryPreferences.swipeToEndAction.get()
    val chapterSwipeEndAction = libraryPreferences.swipeToStartAction.get()
    var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead.get()

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
        return entryPreviewFeature.availability(previewContext(entry)) is EntryPreviewAvailability.Available
    }

    private fun isPreviewAvailable(entry: Entry? = successState?.entry): Boolean {
        return entry != null && isEntryPreviewEnabled(entry)
    }

    private fun observePreviewConfig(entry: Entry) {
        val key = entry.id to entry.type
        if (previewConfigEntryKey == key) return

        previewConfigEntryKey = key
        previewConfigJob?.cancel()
        applyPreviewAvailability(
            availability = entryPreviewFeature.availability(previewContext(entry)),
            reloadExpanded = false,
        )
        previewConfigJob = screenModelScope.launchIO {
            entryPreviewFeature.availabilityChanges(previewContext(entry))
                .flowWithLifecycle(lifecycle)
                .collectLatest { availability ->
                    applyPreviewAvailability(
                        availability = availability,
                        reloadExpanded = true,
                    )
                }
        }
    }

    private fun previewContext(entry: Entry): EntryPreviewContext = EntryPreviewContext(
        entry = entry,
        source = sourceManager.getOrStub(entry.source),
    )

    private fun applyPreviewAvailability(
        availability: EntryPreviewAvailability,
        reloadExpanded: Boolean,
    ) {
        applyPreviewConfig(
            config = availability.config.copy(enabled = availability is EntryPreviewAvailability.Available),
            reloadExpanded = reloadExpanded,
        )
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
        observeChildGroupFilter()

        screenModelScope.launchIO {
            mergeAwareEntryAndChaptersFlow(
                entryAndChaptersFlow = entryAndChaptersFlow(),
                downloadChangesFlow = downloadRuntime.changes,
                downloadQueueFlow = downloadRuntime.state.map { it.queue },
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

        observeDownloads()

        screenModelScope.launchIO {
            val entry = getEntry.await(entryId)
            if (entry == null) {
                mutableState.update { State.Error }
                return@launchIO
            }

            val mergePresentation = getMergePresentation(entry)
            val chapters = getDisplayedChapters()
                .toChapterListItems(entry)
            val childGroupFilterState = entryChildGroupFilterFeature.state(
                EntryChildGroupFilterScope(entry, mergePresentation.memberIds),
            )

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
                    childListFeature = entryChildListFeature,
                    childGroupFilterFeature = entryChildGroupFilterFeature,
                    childGroupFilterState = childGroupFilterState,
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
                    when (val result = entryChildListFeature.progressLabels(request)) {
                        is EntryChildProgressResult.Available -> result.labels
                        is EntryChildProgressResult.Inapplicable -> flowOf(emptyMap())
                    }
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

    private fun observeChildGroupFilter() {
        screenModelScope.launchIO {
            getEntry.subscribe(entryId)
                .flatMapLatest { entry ->
                    entryMergeFeature.observeExisting(entry).map { editor ->
                        val memberIds = if (bypassMerge) {
                            listOf(entry.id)
                        } else {
                            editor?.entries?.map { it.entry.id }.orEmpty().ifEmpty { listOf(entry.id) }
                        }
                        EntryChildGroupFilterScope(entry, memberIds)
                    }
                }
                .flatMapLatest { scope ->
                    when (val observation = entryChildGroupFilterFeature.observe(scope)) {
                        is EntryChildGroupFilterObservationResult.Available -> observation.states.map {
                            EntryChildGroupFilterStateResult.Available(it)
                        }
                        is EntryChildGroupFilterObservationResult.Inapplicable -> flowOf(
                            EntryChildGroupFilterStateResult.Inapplicable(observation.type),
                        )
                    }
                }
                .flowWithLifecycle(lifecycle)
                .collectLatest { childGroupFilterState ->
                    updateSuccessState { it.copy(childGroupFilterState = childGroupFilterState) }
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
                    hasLoaded = false,
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
                val latestEntry = requireNotNull(getEntry.await(entryId)) {
                    "Preview entry $entryId no longer exists"
                }
                val candidates = getPreviewCandidates(latestEntry)
                when (
                    val result = entryPreviewFeature.load(
                        EntryPreviewLoadRequest(
                            context = context,
                            previewContext = previewContext(latestEntry),
                            children = candidates,
                            memberIds = getMergePresentation(latestEntry).memberIds,
                        ),
                    )
                ) {
                    is EntryPreviewLoadResult.Loaded -> result.handle
                    is EntryPreviewLoadResult.Disabled -> error("Preview was disabled before loading")
                    is EntryPreviewLoadResult.Inapplicable -> error("Preview is not supported for ${result.type}")
                    is EntryPreviewLoadResult.ContextuallyUnavailable -> error(
                        context.stringResource(latestEntry.type.entryTypePresentation().noChildrenFoundLabel),
                    )
                }
            }.onSuccess { handle ->
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
                        hasLoaded = true,
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
                        hasLoaded = false,
                    )
                }
            }
        }
    }

    fun loadPreviewPage(pageIndex: Int) {
        val handle = previewHandle ?: return
        val page = handle.pages.firstOrNull { it.index == pageIndex } ?: return
        if (page.status.value == EntryPreviewPageStatus.Ready) return
        if (previewPageJobs[pageIndex]?.isActive == true) return

        previewPageJobs = previewPageJobs + (
            pageIndex to screenModelScope.launchIO {
                try {
                    entryPreviewFeature.loadPage(handle, pageIndex)
                } catch (_: Throwable) {
                    // Page state carries the failure.
                }
            }
            )
    }

    private suspend fun getPreviewCandidates(entry: Entry): List<EntryPreviewChildCandidate> {
        val chapters = getDisplayedChapters()
        val chapterItems = chapters.toChapterListItems(entry)
        return chapterItems.applyFiltersForPreview(entry).map { item ->
            EntryPreviewChildCandidate(
                entry = item.entry,
                child = item.chapter,
                source = sourceManager.getOrStub(item.entry.source),
            )
        }
    }

    fun previewOpenTarget(pageIndex: Int): EntryPreviewOpenTargetResult {
        val handle = previewHandle ?: return EntryPreviewOpenTargetResult.NotOpenable
        return entryPreviewFeature.openTarget(handle, pageIndex)
    }

    fun isPreviewOpenApplicable(type: EntryType): Boolean = entryPreviewFeature.isOpenApplicable(type)

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
        previewHandle?.let(entryPreviewFeature::release)
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
            for (memberEntry in membersToRefresh) {
                when (
                    val result = sourceRefreshFeature.refresh(
                        EntrySourceRefreshRequest(
                            entry = memberEntry,
                            fetchDetails = fetchDetails,
                            fetchChildren = fetchChapters,
                            manual = manualFetch,
                        ),
                    )
                ) {
                    is EntrySourceRefreshResult.Refreshed -> Unit
                    is EntrySourceRefreshResult.SourceUnavailable -> {
                        showRefreshFailure(context.stringResource(MR.strings.loader_not_implemented_error))
                        return
                    }
                    is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                        EntrySourceRefreshFailure.NoChildren -> {
                            val entryType = successState?.entry?.type
                            showRefreshFailure(
                                context.stringResource(entryType.entryTypePresentation().noChildrenFoundLabel),
                            )
                            return
                        }
                        is EntrySourceRefreshFailure.Operation -> {
                            logcat(LogPriority.ERROR, reason.error)
                            showRefreshFailure(with(context) { reason.error.formattedMessage })
                            return
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            val message = with(context) { e.formattedMessage }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    private fun showRefreshFailure(message: String) {
        screenModelScope.launch {
            snackbarHostState.showSnackbar(message = message)
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
            onRemoved = { downloadEntries ->
                screenModelScope.launch {
                    if (downloadEntries.isEmpty()) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = context.stringResource(MR.strings.delete_downloads_for_manga),
                        actionLabel = context.stringResource(MR.strings.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        deleteDownloads(downloadEntries)
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of entry, (removes / adds) entry (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: (List<Entry>) -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val entry = state.entry

            if (isFavorited) {
                when (val result = entryLibraryMembershipFeature.remove(listOf(entry))) {
                    is EntryLibraryRemovalResult.Removed -> withUIContext {
                        onRemoved(result.downloadDecisionEntries)
                    }
                    is EntryLibraryRemovalResult.Failed -> logcat(LogPriority.ERROR, result.cause)
                    EntryLibraryRemovalResult.NoChange -> Unit
                }
            } else {
                addToLibrary(
                    EntryLibraryAddRequest(
                        entry = entry,
                        duplicatePolicy = if (checkDuplicate) {
                            EntryLibraryDuplicatePolicy.CHECK
                        } else {
                            EntryLibraryDuplicatePolicy.ALLOW
                        },
                    ),
                )
            }
        }
    }

    private suspend fun addToLibrary(request: EntryLibraryAddRequest) {
        when (val result = entryLibraryMembershipFeature.add(request)) {
            is EntryLibraryAddResult.DuplicateCandidates -> updateSuccessState {
                it.copy(dialog = Dialog.DuplicateEntry(result.entry, result.candidates))
            }
            is EntryLibraryAddResult.CategorySelectionRequired -> updateSuccessState {
                it.copy(
                    dialog = Dialog.ChangeCategory(
                        entry = result.entry,
                        initialSelection = result.categories.mapAsCheckboxState {
                            it.id in result.selectedCategoryIds
                        },
                    ),
                )
            }
            is EntryLibraryAddResult.Failed -> logcat(LogPriority.ERROR, result.cause)
            is EntryLibraryAddResult.Added,
            is EntryLibraryAddResult.AlreadyInLibrary,
            -> Unit
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
     * Deletes all the downloads selected from the structured Library-removal follow-up.
     */
    private fun deleteDownloads(entries: List<Entry>) {
        screenModelScope.launchNonCancellable {
            entries.forEach { entry -> downloadMaintenance.removeEntryDownloads(entry) }
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
        screenModelScope.launchIO {
            if (entry.favorite) {
                getDisplayedMemberIds().forEach { memberId ->
                    setEntryCategories.await(memberId, categories)
                }
                return@launchIO
            }
            addToLibrary(
                EntryLibraryAddRequest(
                    entry = entry,
                    duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW,
                    categorySelection = EntryLibraryCategorySelection.Selected(categories),
                ),
            )
        }
    }

    // Entry info - end

    // Chapters list - start

    private fun observeDownloads() {
        screenModelScope.launchIO {
            downloadRuntime.statusUpdates()
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
                downloadRuntime.status(
                    type = chapterEntry.type,
                    childId = chapter.id,
                    childName = chapter.name,
                    childScanlator = chapter.scanlator,
                    childUrl = chapter.url,
                    entryTitle = chapterEntry.title,
                    sourceId = chapterEntry.source,
                ) ?: EntryDownloadStatus(chapterEntry.type, chapter.id, EntryDownloadState.NOT_DOWNLOADED)
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
     * Continues reading/watching the next chapter/episode for the entry.
     */
    suspend fun continueEntry(context: Context, entry: Entry) {
        entryContinueFeature.continueEntry(context, entry)
    }

    fun isContinueApplicable(entry: Entry): Boolean = entryContinueFeature.isApplicable(entry.type)

    private fun getBulkDownloadCandidateItems(): List<EntryChapterList.Item> {
        return filteredChapters.orEmpty()
            .filter { item -> item.downloadState == EntryDownloadState.NOT_DOWNLOADED }
    }

    private fun startDownload(
        items: List<EntryChapterList.Item>,
        startNow: Boolean,
    ) {
        val successState = successState ?: return
        if (items.isEmpty()) return

        if (entryDownloadOptionsFeature.isApplicable(successState.entry.type)) {
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
                when (
                    val resolution = entryDownloadOptionsFeature.resolve(
                        context,
                        successState.entry,
                        items.first().chapter,
                    )
                ) {
                    is EntryDownloadOptionsResolution.Resolved -> updateSuccessState { state ->
                        val dialog = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
                        if (dialog.items.map { it.id } != items.map { it.id }) return@updateSuccessState state
                        state.copy(dialog = dialog.copy(options = resolution.options))
                    }
                    EntryDownloadOptionsResolution.ContextuallyUnavailable,
                    EntryDownloadOptionsResolution.Inapplicable,
                    -> {
                        var stillCurrent = false
                        updateSuccessState { state ->
                            val dialog = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
                            if (dialog.items.map { it.id } != items.map { it.id }) return@updateSuccessState state
                            stillCurrent = true
                            state.copy(dialog = null)
                        }
                        if (stillCurrent) queueDownload(items, startNow)
                    }
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
                entryDownloadOptionsFeature.download(entry, chapters, selection, startNow)
            } else {
                entryDownloadActionFeature.download(entry, chapters, startNow)
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
                    entryDownloadActionFeature.retry(items.map { EntryDownloadActionRequest.forEntry(it.entry) })
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
            val result = entryDownloadActionFeature.resolveBulkDownloadCandidates(
                EntryBulkDownloadRequest(
                    entry = state.entry,
                    action = action.toEntryBulkDownloadAction(),
                    sourceIds = state.chapters.mapTo(mutableSetOf(state.entry.source)) { it.entry.source },
                    visibleCandidates = candidateItems.map(EntryChapterList.Item::chapter),
                    memberEntryIds = state.memberIds,
                ),
            )
            if (result is EntryBulkDownloadResolutionResult.Candidates) {
                val itemsByChapterId = candidateItems.associateBy { it.chapter.id }
                startDownload(result.chapters.mapNotNull { itemsByChapterId[it.id] }, false)
            }
        }
    }

    fun migrationAvailable(): Boolean {
        return successState?.entry?.let(entryMigrationFeature::availability) is EntryMigrationAvailability.Available
    }

    fun migrationSubject(): EntryMigrationSubject? {
        val entry = successState?.entry ?: return null
        return (entryMigrationFeature.prepareSelection(listOf(entry)) as? EntryMigrationSelectionResult.Ready)
            ?.subjects
            ?.singleOrNull()
    }

    fun supportsTracking(): Boolean {
        val entryType = successState?.entry?.type ?: return false
        return entryTrackingFeature.availability(entryType) is EntryTrackingAvailability.Available
    }

    private fun cancelDownload(chapterId: Long) {
        val chapterItem = successState?.chapters.orEmpty().firstOrNull { it.id == chapterId } ?: return
        val result = entryDownloadActionFeature.cancel(
            EntryDownloadActionRequest.forEntry(chapterItem.entry),
            chapterId,
        )
        if (result is mihon.entry.interactions.EntryDownloadCancellationResult.Cancelled) {
            updateDownloadState(result.status)
        }
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

            val maxChapterNumber = chapters.maxOf { it.chapterNumber }
            if (
                entryTrackingFeature.inspectProgressSynchronization(
                    successState?.entry ?: return@launchIO,
                    maxChapterNumber,
                ) !is
                    EntryTrackingProgressInspection.UpdateRequired
            ) {
                return@launchIO
            }
            if (autoTrackState == AutoTrackState.ALWAYS) {
                synchronizeTrackingProgress(maxChapterNumber)
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
                synchronizeTrackingProgress(maxChapterNumber)
            }
        }
    }

    private suspend fun setReadStatus(read: Boolean, chapters: List<EntryChapter>) {
        chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
            val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
            entryConsumptionFeature.setConsumed(entry, memberChapters, read)
        }
    }

    private suspend fun refreshTrackers() {
        val entry = successState?.entry ?: return
        val result = entryTrackingFeature.refresh(entry)
        (result as? EntryTrackingRefreshResult.Completed)?.failures.orEmpty()
            .forEach { failure ->
                logcat(LogPriority.ERROR, failure.cause) {
                    "Failed to refresh track data entryId=$entryId for service ${failure.serviceId.value}"
                }
                withUIContext {
                    context.toast(
                        context.stringResource(
                            MR.strings.track_error,
                            failure.serviceName,
                            failure.cause.message ?: "",
                        ),
                    )
                }
            }
    }

    private suspend fun synchronizeTrackingProgress(progress: Double) {
        val entry = successState?.entry ?: return
        val result = entryTrackingFeature.synchronizeProgress(entry, progress)
        if (result is EntryTrackingProgressSynchronizationResult.Completed) {
            result.failures.forEach { failure -> logcat(LogPriority.WARN, failure.cause) }
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
                entryDownloadActionFeature.download(entry, chapters)
            }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param chapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(chapters: List<EntryChapter>, bookmarked: Boolean) {
        screenModelScope.launchIO {
            chapters.groupBy { it.entryId }.forEach { (memberEntryId, memberChapters) ->
                val entry = entryRepository.getEntryById(memberEntryId) ?: return@forEach
                entryBookmarkFeature.setBookmarked(entry, memberChapters, bookmarked)
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
                        entryDownloadActionFeature.delete(entry, memberChapters)
                    }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
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
            entryTrackingFeature.observeSession(entry)
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { session ->
                    val services = (session as? EntryTrackingSession.Available)?.services.orEmpty()
                    updateSuccessState {
                        it.copy(
                            trackingCount = services.count { service -> service.track != null },
                            hasLoggedInTrackers = services.isNotEmpty(),
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
            entryMergeCandidateFeature.observeCandidates(
                this@EntryScreenModel.state
                    .filter { it is State.Success }
                    .map { (it as State.Success).entry }
                    .distinctUntilChanged(),
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
            val editReference: EntryMergeEditReference,
            val targetId: Long,
            val targetReference: EntryMergeEditorEntryReference,
            val targetLocked: Boolean,
            val entries: ImmutableList<MergeEditorEntry>,
            val referencesById: Map<Long, EntryMergeEditorEntryReference>,
            val removedIds: Set<Long>,
            val libraryRemovalIds: Set<Long>,
        ) : Dialog {
            val enabled: Boolean
                get() = entries.count { it.id !in (removedIds + libraryRemovalIds) } > 1
        }
        data class ManageMerge(
            val editReference: EntryMergeEditReference,
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
            val editor = createMergeEditorDialog(dialog.entry, target) ?: return@launchIO
            updateSuccessState {
                it.copy(dialog = editor)
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
            val entry = dialog.entries.firstOrNull { it.id == entryId } ?: return@updateSuccessState state
            val reference = dialog.referencesById[entryId] ?: return@updateSuccessState state
            if (dialog.targetLocked) return@updateSuccessState state
            state.copy(
                dialog = dialog.copy(
                    targetId = entryId,
                    targetReference = reference,
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
            val result = entryMergeFeature.execute(
                EntryMergeCommitIntent(
                    editReference = dialog.editReference,
                    target = dialog.targetReference,
                    orderedEntries = dialog.entries.mapNotNull { dialog.referencesById[it.id] },
                    removedEntries = dialog.referencesById.referencesFor(dialog.removedIds),
                    libraryRemovalEntries = dialog.referencesById.referencesFor(dialog.libraryRemovalIds),
                ),
            )
            if (result is EntryMergeExecutionResult.Applied) {
                dismissDialog()
            }
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
            val editor = getExistingMergeEditor(state.entry) ?: return@launchIO
            val target = editor.entries.firstOrNull { it.reference == editor.target } ?: return@launchIO
            val members = editor.entries.map { editorEntry ->
                MergeMember(
                    id = editorEntry.entry.id,
                    reference = editorEntry.reference,
                    entry = editorEntry.entry,
                    subtitle = getMergeSubtitle(editorEntry.entry),
                )
            }.toImmutableList()
            updateSuccessState {
                it.copy(
                    dialog = Dialog.ManageMerge(
                        editReference = editor.editReference,
                        targetId = target.entry.id,
                        savedTargetId = target.entry.id,
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
                entryMergeFeature.execute(
                    EntryMergeRemoveEntriesIntent(
                        subject = EntryMergeSubject(state.entry.profileId, state.entry.id),
                        entryIds = entryIds.toSet(),
                        removeFromLibrary = false,
                        removeDownloads = false,
                    ),
                )
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
            dismissDialog()
        }
    }

    private suspend fun saveManageMerge(dialog: Dialog.ManageMerge, entryIdsToRemove: Collection<Long>) {
        val target = dialog.members.firstOrNull { it.id == dialog.targetId } ?: return
        entryMergeFeature.execute(
            EntryMergeCommitIntent(
                editReference = dialog.editReference,
                target = target.reference,
                orderedEntries = dialog.members.map(MergeMember::reference),
                removedEntries = dialog.members.referencesFor(entryIdsToRemove - dialog.libraryRemovalIds.toSet()),
                libraryRemovalEntries = dialog.members.referencesFor(dialog.libraryRemovalIds),
            ),
        )
    }

    fun removeMergedEntry(entries: List<Entry>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val result = entryMergeFeature.execute(
                EntryMergeRemoveEntriesIntent(
                    subject = EntryMergeSubject(state.entry.profileId, state.entry.id),
                    entryIds = entries.mapTo(mutableSetOf(), Entry::id),
                    removeFromLibrary = deleteFromLibrary,
                    removeDownloads = deleteChapters,
                ),
            )
            if (result is EntryMergeExecutionResult.Applied) dismissDialog()
        }
    }

    fun unmergeAll() {
        val state = successState ?: return
        screenModelScope.launchIO {
            val result = entryMergeFeature.execute(
                EntryMergeDissolveIntent(EntryMergeSubject(state.entry.profileId, state.entry.id)),
            )
            if (result is EntryMergeExecutionResult.Applied) dismissDialog()
        }
    }

    suspend fun getVisibleEntryId(entryId: Long): Long {
        if (bypassMerge) return entryId
        val entry = getEntryOrNull(entryId) ?: return entryId
        return entryMergeNavigationFeature.resolveNavigation(EntryMergeSubject(entry.profileId, entryId)).visibleEntryId
    }

    fun setExcludedScanlators(excludedScanlators: Set<String>) {
        val state = successState ?: return

        screenModelScope.launchIO {
            entryChildGroupFilterFeature.setExcludedGroups(
                scope = EntryChildGroupFilterScope(state.entry, getDisplayedMemberIds()),
                excludedGroups = excludedScanlators,
            )
        }
    }

    private suspend fun getDisplayedMemberIds(): List<Long> {
        if (bypassMerge) return listOf(entryId)
        return getMergeGroupMemberIds()
    }

    private suspend fun getMergeGroupMemberIds(): List<Long> {
        val entry = getEntryOrNull(entryId) ?: return listOf(entryId)
        return getExistingMergeEditor(entry)?.entries?.map { it.entry.id }.orEmpty().ifEmpty { listOf(entryId) }
    }

    private suspend fun getDisplayedChapters(): List<EntryChapter> {
        val entry = getEntryOrNull(entryId) ?: return emptyList()
        return getEntryWithChapters.awaitChapters(entry, bypassMerge = bypassMerge)
    }

    private suspend fun entryAndChaptersFlow(): Flow<Pair<Entry, List<EntryChapter>>> {
        return getEntry.subscribe(entryId).flatMapLatest { entry ->
            getEntryWithChapters.subscribe(entry, bypassMerge = bypassMerge)
        }
    }

    private suspend fun getMergePresentation(entry: Entry): MergePresentation {
        val editor = getExistingMergeEditor(entry)
        val mergeGroupMemberIds = editor?.entries?.map { it.entry.id }.orEmpty()
            .ifEmpty { listOf(entry.id) }
            .toImmutableList()
        val memberIds = if (bypassMerge) {
            persistentListOf(entry.id)
        } else {
            mergeGroupMemberIds
        }
        val membersById = editor?.entries.orEmpty().associateBy { it.entry.id }
        val members = memberIds.mapNotNull { memberId -> membersById[memberId]?.entry ?: getEntryOrNull(memberId) }
        return MergePresentation(
            sourceName = getSourceName(entry, memberIds),
            memberIds = memberIds,
            memberTitleById = members.associate { it.id to it.displayTitle },
            mergedMemberTitles = members.map { it.displayTitle }.filter { it.isNotBlank() }.toImmutableList(),
            mergeTargetId = editor?.entries?.firstOrNull { it.reference == editor.target }?.entry?.id
                ?: entryMergeNavigationFeature.resolveNavigation(
                    EntryMergeSubject(entry.profileId, entry.id),
                ).visibleEntryId,
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
        val entry = successState?.entry ?: getEntryOrNull(entryId) ?: return emptyList()
        return if (bypassMerge) {
            listOf(entry)
        } else {
            entryMergeMetadataRefreshFeature.resolveOwners(entry).orderedOwners
        }
    }

    private suspend fun getExistingMergeEditor(entry: Entry): EntryMergeEditorProjection? {
        return when (val result = entryMergeFeature.prepare(EntryMergePrepareIntent(listOf(entry)))) {
            is EntryMergePreparationResult.Ready -> result.editor
            is EntryMergePreparationResult.Rejected -> null
        }
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
    ): Dialog.EditMerge? {
        val targetEntry = target.memberEntries.firstOrNull { it.id == target.id }
            ?: target.memberEntries.firstOrNull()
            ?: return null
        val categoryIds = target.categoryIds.ifEmpty { getEntryCategoryIds(targetEntry.id) }
        val preparations = if (entry.favorite) {
            emptyList()
        } else {
            listOf(EntryMergeMemberPreparationIntent(entry, categoryIds))
        }
        val editor = when (
            val result = entryMergeFeature.prepare(
                EntryMergePrepareIntent(
                    selectedEntries = if (target.isMerged) {
                        listOf(entry, targetEntry)
                    } else {
                        listOf(targetEntry, entry)
                    },
                    preparations = preparations,
                ),
            )
        ) {
            is EntryMergePreparationResult.Ready -> result.editor
            is EntryMergePreparationResult.Rejected -> return null
        }
        val targetEditorEntry = editor.entries.firstOrNull { it.reference == editor.target } ?: return null
        return Dialog.EditMerge(
            entry = entry,
            editReference = editor.editReference,
            targetId = targetEditorEntry.entry.id,
            targetReference = targetEditorEntry.reference,
            targetLocked = editor.targetLocked,
            entries = editor.entries.map(::toMergeEditorEntry).toImmutableList(),
            referencesById = editor.entries.associate { it.entry.id to it.reference },
            removedIds = emptySet(),
            libraryRemovalIds = emptySet(),
        )
    }

    private fun toMergeEditorEntry(editorEntry: EntryMergeEditorEntry): MergeEditorEntry {
        val isExisting = editorEntry.origin == mihon.entry.interactions.EntryMergeEditorEntryOrigin.EXISTING_MEMBER
        return MergeEditorEntry(
            id = editorEntry.entry.id,
            entry = editorEntry.entry,
            subtitle = getMergeSubtitle(editorEntry.entry) + if (isExisting) "" else " • New",
            isRemovable = editorEntry.removable,
            isMember = isExisting,
        )
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
     * TODO: Resolve real [LibraryItem] grouping data so the merge-target picker can surface
     * already-merged library entries instead of treating these candidates as standalone entries.
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
                progressSummary = EntryLibraryProgressResolution.Inapplicable(entry.type),
                latestUpload = 0L,
                downloadCount = 0,
            )
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
        val reference: EntryMergeEditorEntryReference,
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
            val childListFeature: EntryChildListFeature,
            val childGroupFilterFeature: EntryChildGroupFilterFeature,
            val childGroupFilterState: EntryChildGroupFilterStateResult,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val duplicateCandidates: List<DuplicateEntryCandidate> = emptyList(),
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val hideMissingChapters: Boolean = false,
        ) : State {
            val processedChapters by lazy {
                val groupFilteredChapters = when (val result = childGroupFilterState) {
                    is EntryChildGroupFilterStateResult.Available -> {
                        when (
                            val filtered = childGroupFilterFeature.filter(
                                entry = entry,
                                chapters = chapters.map { it.chapter },
                                excludedGroups = result.state.excludedGroups,
                            )
                        ) {
                            is EntryChildGroupFilterResult.Available -> {
                                val visibleIds = filtered.chapters.mapTo(hashSetOf(), EntryChapter::id)
                                chapters.filter { it.chapter.id in visibleIds }
                            }
                            is EntryChildGroupFilterResult.Inapplicable -> chapters
                        }
                    }
                    is EntryChildGroupFilterStateResult.Inapplicable -> chapters
                }
                groupFilteredChapters.applyFilters(entry, childListFeature).toList()
            }

            val readingChapters by lazy {
                processedChapters.sortedForReading(entry, memberIds, childListFeature)
            }

            val isAnySelected by lazy {
                chapters.fastAny { it.selected }
            }

            private val childListDisplay by lazy {
                processedChapters.toChapterListDisplay(
                    entry = entry,
                    memberIds = memberIds,
                    memberTitleById = memberTitleById,
                    childListFeature = childListFeature,
                    includeMissingCounts = !hideMissingChapters,
                )
            }

            val chapterListItems: List<EntryChapterList>
                get() = childListDisplay.rows

            val missingChildCount: Int
                get() = childListDisplay.aggregateMissingCount

            val childListApplicable: Boolean
                get() = childListFeature.isApplicable(entry.type)

            val scanlatorFilterActive: Boolean
                get() = (childGroupFilterState as? EntryChildGroupFilterStateResult.Available)?.state?.active == true

            val childGroupFilterApplicable: Boolean
                get() = childGroupFilterState is EntryChildGroupFilterStateResult.Available

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
                childListFeature: EntryChildListFeature,
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
                return filtered.sortedForDisplay(entry, memberIds, childListFeature)
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
        val hasLoaded: Boolean = false,
    ) {
        val hasLoadedContent: Boolean
            get() = (hasLoaded || chapterId != null) && !isLoading && error == null
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

private data class EntryChildListDisplayState(
    val rows: List<EntryChapterList>,
    val aggregateMissingCount: Int,
)

private fun List<EntryChapterList.Item>.toChapterListDisplay(
    entry: Entry,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    childListFeature: EntryChildListFeature,
    includeMissingCounts: Boolean,
): EntryChildListDisplayState {
    val itemByChapterId = associateBy { it.chapter.id }
    val result = childListFeature.displayList(
        EntryChildListRequest(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            includeMissingCounts = includeMissingCounts,
        ),
    )
    if (result is EntryChildListResult.Inapplicable) {
        return EntryChildListDisplayState(rows = emptyList(), aggregateMissingCount = 0)
    }
    val display = (result as EntryChildListResult.Available).display
    val rows = display.rows.mapNotNull { row ->
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
    return EntryChildListDisplayState(
        rows = rows,
        aggregateMissingCount = display.aggregateMissingCount,
    )
}

private fun List<EntryChapterList.Item>.sortedForReading(
    entry: Entry,
    memberIds: List<Long>,
    childListFeature: EntryChildListFeature,
): List<EntryChapterList.Item> {
    return when (
        val result = childListFeature.readingOrder(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
        )
    ) {
        is EntryChildOrderResult.Available -> sortedWithChapterOrder(result.chapters)
        is EntryChildOrderResult.Inapplicable -> emptyList()
    }
}

private fun List<EntryChapterList.Item>.sortedForDisplay(
    entry: Entry,
    memberIds: List<Long>,
    childListFeature: EntryChildListFeature,
): List<EntryChapterList.Item> {
    return when (
        val result = childListFeature.displayOrder(
            entry = entry,
            chapters = map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
        )
    ) {
        is EntryChildOrderResult.Available -> sortedWithChapterOrder(result.chapters)
        is EntryChildOrderResult.Inapplicable -> emptyList()
    }
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
    downloadChangesFlow: Flow<DownloadChanges>,
    downloadQueueFlow: Flow<DownloadQueue>,
): Flow<Pair<Entry, List<EntryChapter>>> {
    return combine(
        entryAndChaptersFlow,
        downloadChangesFlow,
        downloadQueueFlow,
    ) { entryAndChapters, _, _ -> entryAndChapters }
}

private fun Map<Long, EntryMergeEditorEntryReference>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filterKeys { it in ids }.values.toSet()
}

private fun List<EntryScreenModel.MergeMember>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filter { it.id in ids }.mapTo(linkedSetOf(), EntryScreenModel.MergeMember::reference)
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
