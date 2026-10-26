package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.core.util.addOrRemove
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryDownloadChoices
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorLock
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.MergeTargetSearchController
import eu.kanade.presentation.entry.components.buildMergeTargetQuery
import eu.kanade.presentation.entry.components.buildMergeTargets
import eu.kanade.presentation.entry.components.mergedSourceNames
import eu.kanade.presentation.entry.components.rankMergeTargets
import eu.kanade.presentation.entry.components.toChoices
import eu.kanade.presentation.entry.components.toMergeEditorLock
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import eu.kanade.tachiyomi.ui.base.viewmodel.GatedStateViewModel
import eu.kanade.tachiyomi.ui.entry.olderchapters.EntryOlderChapters
import eu.kanade.tachiyomi.ui.entry.olderchapters.OlderChaptersStatus
import eu.kanade.tachiyomi.ui.entry.translation.ChapterTranslationModel
import eu.kanade.tachiyomi.util.lang.toStoredDisplayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildListRequest
import mihon.entry.interactions.child.EntryChildListResult
import mihon.entry.interactions.child.EntryChildListRow
import mihon.entry.interactions.child.EntryChildOrderResult
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.child.EntryChildProgressResult
import mihon.entry.interactions.child.EntryScanlatorFilterFeature
import mihon.entry.interactions.child.EntryScanlatorFilterObservationResult
import mihon.entry.interactions.child.EntryScanlatorFilterResult
import mihon.entry.interactions.child.EntryScanlatorFilterScope
import mihon.entry.interactions.child.EntryScanlatorFilterStateResult
import mihon.entry.interactions.download.EntryBulkDownloadAction
import mihon.entry.interactions.download.EntryBulkDownloadRequest
import mihon.entry.interactions.download.EntryBulkDownloadResolutionResult
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadActionRequest
import mihon.entry.interactions.download.EntryDownloadCancellationResult
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadOptionSelection
import mihon.entry.interactions.download.EntryDownloadOptions
import mihon.entry.interactions.download.EntryDownloadOptionsFeature
import mihon.entry.interactions.download.EntryDownloadOptionsResolution
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.download.sampleProgress
import mihon.entry.interactions.library.membership.EntryLibraryAddRequest
import mihon.entry.interactions.library.membership.EntryLibraryAddResult
import mihon.entry.interactions.library.membership.EntryLibraryCategorySelection
import mihon.entry.interactions.library.membership.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.library.membership.EntryLibraryRemovalResult
import mihon.entry.interactions.media.EntryPreviewAvailability
import mihon.entry.interactions.media.EntryPreviewChildCandidate
import mihon.entry.interactions.media.EntryPreviewConfig
import mihon.entry.interactions.media.EntryPreviewContext
import mihon.entry.interactions.media.EntryPreviewFeature
import mihon.entry.interactions.media.EntryPreviewHandle
import mihon.entry.interactions.media.EntryPreviewLoadRequest
import mihon.entry.interactions.media.EntryPreviewLoadResult
import mihon.entry.interactions.media.EntryPreviewOpenTargetResult
import mihon.entry.interactions.media.EntryPreviewPage
import mihon.entry.interactions.media.EntryPreviewPageStatus
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import mihon.entry.interactions.merge.EntryMergeCommitIntent
import mihon.entry.interactions.merge.EntryMergeDissolveIntent
import mihon.entry.interactions.merge.EntryMergeEditReference
import mihon.entry.interactions.merge.EntryMergeEditorEntry
import mihon.entry.interactions.merge.EntryMergeEditorEntryOrigin
import mihon.entry.interactions.merge.EntryMergeEditorEntryReference
import mihon.entry.interactions.merge.EntryMergeEditorProjection
import mihon.entry.interactions.merge.EntryMergeExecutionResult
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergeMemberPreparationIntent
import mihon.entry.interactions.merge.EntryMergeMetadataRefreshFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergePreparationResult
import mihon.entry.interactions.merge.EntryMergePrepareIntent
import mihon.entry.interactions.merge.EntryMergeRemoveEntriesIntent
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueTargetResult
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.state.EntryConsumptionFeature
import mihon.entry.interactions.tracking.EntryTrackingAvailability
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingProgressInspection
import mihon.entry.interactions.tracking.EntryTrackingProgressSynchronizationResult
import mihon.entry.interactions.tracking.EntryTrackingRefreshResult
import mihon.entry.interactions.tracking.EntryTrackingSession
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.feature.library.search.librarySearchQuery
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.pausedWhileUnsubscribed
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryChapterFlags
import tachiyomi.domain.entry.interactor.SetEntryDefaultChapterFlags
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.groupId
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.service.EntryChapterFilters
import tachiyomi.domain.entry.service.SectionExpansion
import tachiyomi.domain.entry.service.expansionSession
import tachiyomi.domain.entry.service.showsMergedMembersReversed
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.LocalSource
import kotlin.time.Duration.Companion.milliseconds

@AssistedInject
class EntryViewModel(
    @Assisted private val context: Context,
    @Assisted private val entryId: Long,
    @Assisted private val isFromSource: Boolean,
    @Assisted private val bypassMerge: Boolean,
    private val libraryPreferences: LibraryPreferences,
    private val duplicatePreferences: tachiyomi.domain.library.service.DuplicatePreferences,
    trackPreferences: TrackPreferences,
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val entryDownloadActionFeature: EntryDownloadActionFeature,
    private val entryDownloadOptionsFeature: EntryDownloadOptionsFeature,
    private val downloadMaintenance: EntryDownloadMaintenanceFeature,
    private val entryMigrationFeature: EntryMigrationFeature,
    private val entryConsumptionFeature: EntryConsumptionFeature,
    private val entryBookmarkFeature: EntryBookmarkFeature,
    private val entryContinueFeature: EntryContinueFeature,
    private val entryPreviewFeature: EntryPreviewFeature,
    private val entryChildListFeature: EntryChildListFeature,
    private val entryScanlatorFilterFeature: EntryScanlatorFilterFeature,
    private val entryMergeFeature: EntryMergeFeature,
    private val entryMergeCandidateFeature: EntryMergeCandidateFeature,
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature,
    private val entryMergeMetadataRefreshFeature: EntryMergeMetadataRefreshFeature,
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature,
    private val getEntry: GetEntry,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val getLibraryEntries: GetLibraryEntries,
    private val setEntryChapterFlags: SetEntryChapterFlags,
    private val setEntryDefaultChapterFlags: SetEntryDefaultChapterFlags,
    private val setEntryCategories: SetEntryCategories,
    private val updateEntry: UpdateEntry,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryRepository: EntryRepository,
    mergedMemberExpansionRepository: MergedMemberExpansionRepository,
    private val entryChapterGroupRepository: EntryChapterGroupRepository,
    chapterGroupExpansionRepository: ChapterGroupExpansionRepository,
    private val categoryRepository: CategoryRepository,
    private val sourceRefreshFeature: EntrySourceRefreshFeature,
    private val entryTrackingFeature: EntryTrackingFeature,
    private val sourceManager: SourceManager,
    private val sourceNames: SourceInfoNames,
    private val sourcePreferences: SourcePreferences,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
    private val olderChapters: EntryOlderChapters,
    translateFeature: EntryTranslateFeature,
    translationLanguages: EntryTranslationLanguagesFeature,
    recognitionHost: TextRecognitionHostActions,
    translationHost: TranslationHostActions,
    modelStore: ModelArtifactStore,
) : GatedStateViewModel<EntryViewModel.State>(State.Loading) {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(context: Context, entryId: Long, isFromSource: Boolean, bypassMerge: Boolean): EntryViewModel
    }

    val snackbarHostState = SnackbarHostState()

    private val mergeTargetSearchController = MergeTargetSearchController<MergeTarget>(viewModelScope)

    /** Translation of the listed chapters; chapters to translate that are not downloaded yet are downloaded. */
    val translation = ChapterTranslationModel(
        scope = viewModelScope,
        context = context,
        snackbarHostState = snackbarHostState,
        download = { chapters ->
            val ids = chapters.mapTo(HashSet()) { it.id }
            startDownload(successState?.chapters.orEmpty().filter { it.id in ids }, startNow = false)
        },
        feature = translateFeature,
        languages = translationLanguages,
        recognitionHost = recognitionHost,
        translationHost = translationHost,
        modelStore = modelStore,
    )

    private val successState: State.Success?
        get() = mutableState.value as? State.Success

    private val memberExpansion = mergedMemberExpansionRepository.expansionSession(
        scope = viewModelScope,
        memberIds = mutableState.filterIsInstance<State.Success>().map { it.memberIds },
    )

    private val groupExpansion = chapterGroupExpansionRepository.expansionSession(
        scope = viewModelScope,
        entryIds = mutableState.filterIsInstance<State.Success>().map { it.memberIds },
    )

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
    private var downloadOptionsReloadJob: Job? = null

    val isUpdateIntervalEnabled =
        LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD in libraryPreferences.updateSkipRules.get()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    private val selectedChapterIds: HashSet<Long> = HashSet()

    // Progress ticks often, so only the download indicators that read it recompose instead of every tick rebuilding
    // the chapter list
    private val downloadProgress = mutableStateMapOf<Long, Int>()

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
        previewConfigJob = gatedScope.launchIO {
            entryPreviewFeature.availabilityChanges(previewContext(entry))
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
        observeScanlatorFilter()
        observeContinueTarget()
        observeExpandedMembers()
        observeChapterGroups()
        observeOlderChapters()
        observeExpandedGroups()

        gatedScope.launchIO {
            mergeAwareEntryAndChaptersFlow(
                entryAndChaptersFlow = entryAndChaptersFlow(),
                downloadChangesFlow = downloadRuntime.changes.pausedWhileUnsubscribed(),
            )
                .collectLatest { (entry, chapters) ->
                    observePreviewConfig(entry)
                    val mergePresentation = getMergePresentation(entry)
                    updateSuccessState {
                        it.copy(
                            entry = entry,
                            sourceName = mergePresentation.sourceName,
                            sourceSearchQuery = mergePresentation.sourceSearchQuery,
                            isMultiSource = mergePresentation.isMultiSource,
                            isSourceMissing = mergePresentation.isSourceMissing,
                            memberIds = mergePresentation.memberIds,
                            memberTitleById = mergePresentation.memberTitleById,
                            memberSourceNameById = mergePresentation.memberSourceNameById,
                            mergeTargetId = mergePresentation.mergeTargetId,
                            mergeGroupMemberIds = mergePresentation.mergeGroupMemberIds,
                            chapters = chapters.toChapterListItems(entry),
                            duplicateCandidates = if (it.isFromSource && !entry.favorite) {
                                it.duplicateCandidates
                            } else {
                                emptyList()
                            },
                        ).rebuildChapterPresentation()
                    }
                }
        }

        observeDownloads()

        viewModelScope.launchIO {
            val entry = getEntry.await(entryId)
            if (entry == null) {
                mutableState.update { State.Error }
                return@launchIO
            }

            val mergePresentation = getMergePresentation(entry)
            val chapters = getDisplayedChapters()
                .toChapterListItems(entry)
            val scanlatorFilterState = entryScanlatorFilterFeature.state(
                EntryScanlatorFilterScope(entry, mergePresentation.memberIds),
            )

            if (!entry.favorite) {
                setEntryDefaultChapterFlags.await(listOf(entry.id))
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
                    sourceSearchQuery = mergePresentation.sourceSearchQuery,
                    isMultiSource = mergePresentation.isMultiSource,
                    isSourceMissing = mergePresentation.isSourceMissing,
                    memberIds = mergePresentation.memberIds,
                    memberTitleById = mergePresentation.memberTitleById,
                    memberSourceNameById = mergePresentation.memberSourceNameById,
                    mergeTargetId = mergePresentation.mergeTargetId,
                    mergeGroupMemberIds = mergePresentation.mergeGroupMemberIds,
                    isFromSource = isFromSource,
                    chapters = chapters,
                    childListFeature = entryChildListFeature,
                    scanlatorFilterFeature = entryScanlatorFilterFeature,
                    scanlatorFilterState = scanlatorFilterState,
                    duplicateCandidates = emptyList(),
                    isRefreshingData = needRefreshInfo || needRefreshChapter,
                    dialog = null,
                    hideMissingChapters = libraryPreferences.hideMissingChapters.get(),
                    downloadedOnly = libraryPreferences.downloadedOnly.get(),
                )
            }

            observeDuplicateCandidates()

            // Start observe tracking since it only needs entryId
            observeTrackers()

            // Fetch info-chapters when needed
            if (viewModelScope.isActive && (needRefreshInfo || needRefreshChapter)) {
                launchRefreshFromSource(
                    manualFetch = false,
                    fetchInfo = needRefreshInfo,
                    fetchChapters = needRefreshChapter,
                ).join()
            }
        }
    }

    private fun observeContinueTarget() {
        gatedScope.launchIO {
            mutableState
                .filterIsInstance<State.Success>()
                .map { successState ->
                    ContinueTargetRequest(
                        entry = successState.entry,
                        chapters = successState.chapters.map { item ->
                            ContinueTargetChapter(
                                id = item.chapter.id,
                                read = item.chapter.read,
                                bookmark = item.chapter.bookmark,
                                kind = item.chapter.kind,
                                isDownloaded = item.isDownloaded,
                            )
                        },
                    )
                }
                .distinctUntilChanged()
                .map { request ->
                    when (val result = entryContinueFeature.nextTarget(request.entry)) {
                        is EntryContinueTargetResult.Available -> result.chapter.id
                        EntryContinueTargetResult.Inapplicable,
                        EntryContinueTargetResult.NoNext,
                        -> null
                    }
                }
                .collectLatest { chapterId ->
                    updateSuccessState { it.copy(continueTargetChapterId = chapterId).rebuildChapterPresentation() }
                }
        }
    }

    private fun observeExpandedMembers() {
        gatedScope.launchIO {
            memberExpansion.state
                .collectLatest { expansion ->
                    updateSuccessState { it.copy(memberExpansion = expansion).rebuildChapterPresentation() }
                }
        }
    }

    private fun observeChapterGroups() {
        gatedScope.launchIO {
            mutableState
                .filterIsInstance<State.Success>()
                .map { it.memberIds }
                .distinctUntilChanged()
                .flatMapLatest(entryChapterGroupRepository::subscribe)
                .collectLatest { groups ->
                    updateSuccessState { it.copy(chapterGroups = groups).rebuildChapterPresentation() }
                }
        }
    }

    private fun observeOlderChapters() {
        gatedScope.launchIO {
            mutableState
                .filterIsInstance<State.Success>()
                .map { it.memberIds }
                .distinctUntilChanged()
                .flatMapLatest(olderChapters::subscribe)
                .collectLatest { status -> updateSuccessState { it.copy(olderChapters = status) } }
        }
    }

    fun retryOlderChapters() {
        val memberIds = successState?.memberIds ?: return
        viewModelScope.launchIO { olderChapters.retry(memberIds) }
    }

    private fun observeExpandedGroups() {
        gatedScope.launchIO {
            groupExpansion.state
                .collectLatest { expansion ->
                    updateSuccessState { it.copy(groupExpansion = expansion).rebuildChapterPresentation() }
                }
        }
    }

    fun toggleGroupExpanded(group: ChapterGroupId) {
        val successState = successState ?: return
        if (group in successState.expandedGroupIds) deselectChapters { it.groupId == group }
        groupExpansion.toggle(group, successState.currentGroupId)
    }

    /** Selects the chapters of [group] the filters show, opening it so they're in sight, or clears them if all are. */
    fun toggleGroupSelection(group: ChapterGroupId) {
        val successState = successState ?: return
        val groupItems = successState.processedChapters.filter { it.chapter.groupId == group }
        if (groupItems.isEmpty()) return
        if (groupItems.all(EntryChapterList.Item::selected)) {
            deselectChapters { it.groupId == group }
            return
        }
        if (group !in successState.expandedGroupIds) groupExpansion.toggle(group, successState.currentGroupId)
        updateSuccessState { state ->
            val updates = state.processedChapters.mapNotNull { item ->
                if (item.selected || item.chapter.groupId != group) return@mapNotNull null
                selectedChapterIds.add(item.id)
                item.copy(selected = true)
            }
            state.updateChapterItems(updates).also { updated ->
                selectedPositions[0] = updated.processedChapters.indexOfFirst { it.selected }
                selectedPositions[1] = updated.processedChapters.indexOfLast { it.selected }
            }
        }
    }

    fun toggleMemberExpanded(memberId: Long) {
        val successState = successState ?: return
        // Chapters hidden by collapsing stay out of the selection, so selection actions only reach chapters in sight.
        if (memberId in successState.expandedMemberIds) deselectChapters { it.entryId == memberId }
        memberExpansion.toggle(memberId, successState.currentMemberId)
    }

    private fun deselectChapters(predicate: (EntryChapter) -> Boolean) {
        updateSuccessState { successState ->
            val updates = successState.chapters.mapNotNull { item ->
                if (!item.selected || !predicate(item.chapter)) return@mapNotNull null
                selectedChapterIds.remove(item.id)
                item.copy(selected = false)
            }
            successState.updateChapterItems(updates).also { updated ->
                selectedPositions[0] = updated.processedChapters.indexOfFirst { it.selected }
                selectedPositions[1] = updated.processedChapters.indexOfLast { it.selected }
            }
        }
    }

    private fun observeChildProgressLabels() {
        gatedScope.launchIO {
            mutableState
                .filterIsInstance<State.Success>()
                .map { successState ->
                    // Every chapter, not only those the filters show: whether reading has started counts hidden ones.
                    EntryChildProgressRequest(
                        entry = successState.entry,
                        chapters = successState.chapters.map { it.chapter },
                        memberIds = successState.memberIds,
                    )
                }
                .distinctUntilChanged()
                .flatMapLatest { request ->
                    // Labels of the previous request stay until these arrive; clearing them would flash every
                    // chapter as unstarted.
                    when (val result = entryChildListFeature.progressLabels(request)) {
                        is EntryChildProgressResult.Available -> result.labels
                        is EntryChildProgressResult.Inapplicable -> flowOf(emptyMap())
                    }
                        .catch { error ->
                            logcat(LogPriority.ERROR, error)
                            emit(emptyMap())
                        }
                }
                .collectLatest { labels ->
                    updateSuccessState {
                        it.copy(childProgressLabels = labels, childProgressLoaded = true)
                    }
                }
        }
    }

    private fun observeScanlatorFilter() {
        gatedScope.launchIO {
            getEntry.subscribe(entryId)
                .flatMapLatest { entry ->
                    entryMergeFeature.observeExisting(entry).map { editor ->
                        val memberIds = if (bypassMerge) {
                            listOf(entry.id)
                        } else {
                            editor?.entries?.map { it.entry.id }.orEmpty().ifEmpty { listOf(entry.id) }
                        }
                        EntryScanlatorFilterScope(entry, memberIds)
                    }
                }
                .flatMapLatest { scope ->
                    when (val observation = entryScanlatorFilterFeature.observe(scope)) {
                        is EntryScanlatorFilterObservationResult.Available -> observation.states.map {
                            EntryScanlatorFilterStateResult.Available(it)
                        }
                        is EntryScanlatorFilterObservationResult.Inapplicable -> flowOf(
                            EntryScanlatorFilterStateResult.Inapplicable(observation.type),
                        )
                    }
                }
                .collectLatest { scanlatorFilterState ->
                    updateSuccessState {
                        it.copy(scanlatorFilterState = scanlatorFilterState).rebuildChapterPresentation()
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
        previewLoadJob = viewModelScope.launchIO {
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
                        context.stringResource(latestEntry.type.presentation().noChildrenFoundLabel),
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
            pageIndex to viewModelScope.launchIO {
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
        return chapterItems.applyFilters(entry, libraryPreferences.downloadedOnly.get()).map { item ->
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
        return viewModelScope.launchIO {
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

    override fun onCleared() {
        super.onCleared()
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
            // Refreshing a merged entry can bring in members, which are refreshed in the same pass
            val refreshed = mutableSetOf<Long>()
            while (true) {
                val membersToRefresh = getMembersToRefreshFromSource(manualFetch).filter { refreshed.add(it.id) }
                if (membersToRefresh.isEmpty()) break
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
                                    context.stringResource(entryType.presentation().noChildrenFoundLabel),
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
            }
        } catch (_: CancellationException) {
            // ignore
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            val message = with(context) { e.formattedMessage }

            viewModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    private fun showRefreshFailure(message: String) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(message = message)
        }
    }

    // Entry info - start

    fun toggleFavorite() {
        val state = successState ?: return
        if (isFavorited) {
            if (state.isMerged) {
                showRemoveMergedEntryDialog()
            } else {
                updateSuccessState { it.copy(dialog = Dialog.RemoveFromLibrary) }
            }
            return
        }
        toggleFavorite(onRemoved = ::offerDownloadDeletion)
    }

    fun removeFromLibrary() {
        if (!isFavorited) return
        toggleFavorite(onRemoved = ::offerDownloadDeletion)
    }

    private fun offerDownloadDeletion(downloadEntries: List<Entry>) {
        viewModelScope.launch {
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
    }

    /**
     * Update favorite status of entry, (removes / adds) entry (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: (List<Entry>) -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        viewModelScope.launchIO {
            val entry = state.entry

            if (isFavorited) {
                when (val result = entryLibraryMembershipFeature.remove(listOf(entry))) {
                    is EntryLibraryRemovalResult.Removed -> withUIContext {
                        onRemoved(result.downloadDecisionEntries)
                    }
                    is EntryLibraryRemovalResult.Failed -> logcat(LogPriority.ERROR, result.cause)
                    is EntryLibraryRemovalResult.LockedInMerge -> withUIContext {
                        val locked = result.members.first()
                        snackbarHostState.showSnackbar(
                            context.stringResource(
                                MR.strings.merge_locked_member_removal,
                                locked.member.displayTitle,
                                locked.mergedEntry.displayTitle,
                            ),
                        )
                    }
                    EntryLibraryRemovalResult.NoChange -> Unit
                }
            } else {
                addToLibrary(
                    EntryLibraryAddRequest(
                        entry = state.libraryAdditionEntry(),
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

    /**
     * A member shown inside its merge, such as a box-set title opened from Browse, adds the merged entry the screen
     * shows. Adding the member alone would take it out of a merge outside the Library.
     */
    private suspend fun State.Success.libraryAdditionEntry(): Entry {
        if (!isMerged || mergeTargetId == entry.id) return entry
        return getEntryOrNull(mergeTargetId)?.takeUnless(Entry::favorite) ?: entry
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
        viewModelScope.launch {
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
        viewModelScope.launchIO {
            val updatedEntry = entry.copy(
                // Custom intervals are negative
                fetchInterval = -interval,
            )
            if (entryRepository.update(updatedEntry)) {
                val refreshedEntry = entryRepository.getEntryById(entry.id)
                if (refreshedEntry != null) {
                    updateSuccessState { it.copy(entry = refreshedEntry).rebuildChapterPresentation() }
                }
            }
        }
    }

    /**
     * Deletes all the downloads selected from the structured Library-removal follow-up.
     */
    private fun deleteDownloads(entries: List<Entry>) {
        viewModelScope.launchNonCancellable {
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
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
            downloadRuntime.statusUpdates()
                .filter { download ->
                    successState?.containsChapter(download) == true
                }
                // Each tick looks up its chapter row, and the progress shown needn't move more often than this
                .sampleProgress(200.milliseconds)
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .collect(::updateDownloadState)
        }
    }

    fun getDownloadProgress(chapterId: Long): Int = downloadProgress[chapterId] ?: 0

    private fun updateDownloadProgress(download: EntryDownloadStatus) {
        if (download.state == EntryDownloadState.DOWNLOADING) {
            downloadProgress[download.chapterId] = download.progress
        } else {
            downloadProgress.remove(download.chapterId)
        }
    }

    private fun updateDownloadState(download: EntryDownloadStatus) {
        updateDownloadProgress(download)
        updateSuccessState { successState ->
            val item = successState.chapter(download) ?: return@updateSuccessState successState
            if (item.downloadState == download.state) return@updateSuccessState successState
            val updatedItem = item.copy(downloadState = download.state)
            successState.updateChapterItems(
                items = listOf(updatedItem),
                rebuildPresentation = successState.entry.downloadedFilter != TriState.DISABLED &&
                    item.isDownloaded != updatedItem.isDownloaded,
            )
        }
    }

    private suspend fun List<EntryChapter>.toChapterListItems(entry: Entry): List<EntryChapterList.Item> {
        val entriesById = mutableMapOf(entry.id to entry)
        val downloadStatuses = mutableMapOf<Long, EntryDownloadStatus>()
        groupBy { it.entryId }.forEach { (entryId, chapters) ->
            val chapterEntry = entriesById[entryId]
                ?: (entryRepository.getEntryById(entryId) ?: entry).also { entriesById[entryId] = it }
            if (!chapterEntry.isLocal()) {
                downloadRuntime.statuses(chapterEntry, chapters)?.let(downloadStatuses::putAll)
            }
        }
        return map { chapter ->
            val chapterEntry = entriesById.getValue(chapter.entryId)
            val isLocal = chapterEntry.isLocal()
            val activeDownload = if (isLocal) {
                null
            } else {
                downloadStatuses[chapter.id]
                    ?: EntryDownloadStatus(chapterEntry.type, chapter.id, EntryDownloadState.NOT_DOWNLOADED)
            }
            val downloadState = when {
                isLocal -> EntryDownloadState.DOWNLOADED
                activeDownload != null -> activeDownload.state
                else -> EntryDownloadState.NOT_DOWNLOADED
            }
            activeDownload?.let(::updateDownloadProgress)

            EntryChapterList.Item(
                chapter = chapter,
                entry = chapterEntry,
                downloadState = downloadState,
                selected = chapter.id in selectedChapterIds,
            )
        }.toPersistentList()
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.ChapterSwipeAction.Disabled]
     */
    fun chapterSwipe(chapterItem: EntryChapterList.Item, swipeAction: LibraryPreferences.ChapterSwipeAction) {
        viewModelScope.launch {
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
                        choices = null,
                    ),
                )
            }
            viewModelScope.launchIO {
                when (
                    val resolution = entryDownloadOptionsFeature.resolve(
                        context,
                        successState.entry,
                        items.first().chapter,
                        selection = null,
                    )
                ) {
                    is EntryDownloadOptionsResolution.Resolved -> updateSuccessState { state ->
                        val dialog = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
                        if (dialog.items.map { it.id } != items.map { it.id }) return@updateSuccessState state
                        state.copy(dialog = dialog.copy(choices = resolution.options.toChoices()))
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

    fun selectDownloadOption(groupKey: String, optionKey: String?) {
        val entry = successState?.entry ?: return
        var listing: Pair<Dialog.DownloadSettings, EntryDownloadChoices.Reload>? = null
        updateSuccessState { state ->
            val dialog = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
            val choices = dialog.choices ?: return@updateSuccessState state
            val picked = choices.pick(groupKey, optionKey)
            listing = picked.reload?.takeIf { it != choices.reload }?.let { dialog to it }
            state.copy(dialog = dialog.copy(choices = picked))
        }
        val (dialog, reload) = listing ?: return
        downloadOptionsReloadJob?.cancel()
        downloadOptionsReloadJob = viewModelScope.launchIO {
            val resolution = entryDownloadOptionsFeature.resolve(
                context,
                entry,
                dialog.items.first().chapter,
                EntryDownloadOptionSelection(reload.picks),
            )
            // The sheet opened with options, so getting none now means the source couldn't list them.
            val options = (resolution as? EntryDownloadOptionsResolution.Resolved)?.options
                ?: EntryDownloadOptions(groups = emptyList(), listingFailed = true)
            updateSuccessState { state ->
                val current = state.dialog as? Dialog.DownloadSettings ?: return@updateSuccessState state
                val choices = current.choices
                if (current.items != dialog.items || choices?.reload != reload) return@updateSuccessState state
                state.copy(dialog = current.copy(choices = choices.reloaded(options)))
            }
        }
    }

    fun confirmDownloadSettings() {
        val dialog = successState?.dialog as? Dialog.DownloadSettings ?: return
        val choices = dialog.choices?.takeIf { it.canDownload } ?: return
        dismissDialog()
        queueDownload(dialog.items, dialog.startNow, choices.selection)
    }

    private fun queueDownload(
        items: List<EntryChapterList.Item>,
        startNow: Boolean,
        selection: EntryDownloadOptionSelection? = null,
    ) {
        val successState = successState ?: return
        toggleAllSelection(false)

        viewModelScope.launchNonCancellable {
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
        val bulkAction = action.toEntryBulkDownloadAction()
        viewModelScope.launchNonCancellable {
            if (bulkAction.needsWholeChildList && successState?.olderChapters != null) finishOlderChapters()
            val state = successState ?: return@launchNonCancellable
            val candidateItems = getBulkDownloadCandidateItems()
            val result = entryDownloadActionFeature.resolveBulkDownloadCandidates(
                EntryBulkDownloadRequest(
                    entry = state.entry,
                    action = bulkAction,
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

    // Reads the rest of the chapter lists, then the chapters from storage, so candidates come from every chapter rather
    // than waiting for the list's own update
    private suspend fun finishOlderChapters() {
        val state = successState ?: return
        olderChapters.finish(state.chapters.map(EntryChapterList.Item::entry).plus(state.entry).distinctBy(Entry::id))
        val entry = getEntryOrNull(entryId) ?: return
        val chapters = getDisplayedChapters().toChapterListItems(entry)
        updateSuccessState { it.copy(entry = entry, chapters = chapters).rebuildChapterPresentation() }
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
        if (result is EntryDownloadCancellationResult.Cancelled) {
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
        viewModelScope.launchIO {
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
                snackbarHostState.showSnackbar(
                    context.stringResource(MR.strings.trackers_updated_summary, maxChapterNumber.toInt()),
                )
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
                viewModelScope.launch {
                    snackbarHostState.showSnackbar(
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
     * Bookmarks the given list of chapters.
     * @param chapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(chapters: List<EntryChapter>, bookmarked: Boolean) {
        viewModelScope.launchIO {
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
        viewModelScope.launchNonCancellable {
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
        viewModelScope.launchNonCancellable {
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

        viewModelScope.launchNonCancellable {
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

        viewModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setBookmarkFilter(flag))
            }
        }
    }

    fun setFillerFilter(state: TriState) {
        successState ?: return

        val flag = when (state) {
            TriState.DISABLED -> Entry.SHOW_ALL
            TriState.ENABLED_IS -> Entry.CHAPTER_SHOW_FILLER
            TriState.ENABLED_NOT -> Entry.CHAPTER_SHOW_NOT_FILLER
        }

        viewModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setFillerFilter(flag))
            }
        }
    }

    fun setShowChapterPreviews(show: Boolean) {
        successState ?: return

        viewModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setShowChapterPreviews(show))
            }
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val entry = successState?.entry ?: return

        viewModelScope.launchNonCancellable {
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

        viewModelScope.launchNonCancellable {
            updateMergedMemberEntries { memberEntry ->
                setEntryChapterFlags.await(memberEntry.id, memberEntry.chapterFlags.setSortingModeOrFlipOrder(sort))
            }
        }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val entry = successState?.entry ?: return
        viewModelScope.launchNonCancellable {
            libraryPreferences.setChapterSettingsDefault(entry)
            if (applyToExisting) {
                setEntryDefaultChapterFlags.awaitWithLibrary(getDisplayedMemberIds())
            }
            snackbarHostState.showSnackbar(
                message = context.stringResource(entry.type.presentation().settingsUpdatedLabel),
            )
        }
    }

    fun resetToDefaultSettings() {
        viewModelScope.launchNonCancellable {
            setEntryDefaultChapterFlags.await(getDisplayedMemberIds())
        }
    }

    fun toggleSelection(
        item: EntryChapterList.Item,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        updateSuccessState { successState ->
            val chapters = successState.processedChapters
            val selectedIndex = successState.processedChapterIndex(item.id)
                ?: return@updateSuccessState successState

            val selectedItem = chapters[selectedIndex]
            if (selectedItem.selected == selected) return@updateSuccessState successState

            val updates = mutableListOf(selectedItem.copy(selected = selected))
            selectedChapterIds.addOrRemove(item.id, selected)

            if (selected && fromLongPress) {
                if (successState.selectedChapters.isEmpty()) {
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

                    range.forEach { index ->
                        val inbetweenItem = chapters[index]
                        // Chapters in a closed member or group stay out of the range
                        if (!inbetweenItem.selected && successState.chapterListIndex(inbetweenItem.id) != null) {
                            selectedChapterIds.add(inbetweenItem.id)
                            updates += inbetweenItem.copy(selected = true)
                        }
                    }
                }
            } else if (!fromLongPress) {
                if (!selected) {
                    if (selectedIndex == selectedPositions[0]) {
                        selectedPositions[0] = chapters.indexOfFirst { it.selected && it.id != item.id }
                    } else if (selectedIndex == selectedPositions[1]) {
                        selectedPositions[1] = chapters.indexOfLast { it.selected && it.id != item.id }
                    }
                } else if (selectedIndex < selectedPositions[0]) {
                    selectedPositions[0] = selectedIndex
                } else if (selectedIndex > selectedPositions[1]) {
                    selectedPositions[1] = selectedIndex
                }
            }
            successState.updateChapterItems(updates)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        if (!selected && selectedChapterIds.isEmpty()) return
        updateSuccessState { successState ->
            val updates = successState.chapters.mapNotNull {
                if (it.selected == selected) return@mapNotNull null
                selectedChapterIds.addOrRemove(it.id, selected)
                it.copy(selected = selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.updateChapterItems(updates)
        }
    }

    fun invertSelection() {
        updateSuccessState { successState ->
            val updates = successState.chapters.map {
                selectedChapterIds.addOrRemove(it.id, !it.selected)
                it.copy(selected = !it.selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.updateChapterItems(updates)
        }
    }

    // Chapters list - end

    // Track sheet - start

    private fun observeTrackers() {
        val entry = successState?.entry ?: return

        gatedScope.launchIO {
            entryTrackingFeature.observeSession(entry)
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
        duplicateObservationJob = gatedScope.launchIO {
            entryMergeCandidateFeature.observeCandidates(
                mutableState
                    .filter { it is State.Success }
                    .map { (it as State.Success).entry }
                    .distinctUntilChanged(),
            )
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

        /** [choices] is null until the sheet's options are listed. */
        data class DownloadSettings(
            val items: List<EntryChapterList.Item>,
            val startNow: Boolean,
            val choices: EntryDownloadChoices?,
        ) : Dialog
        data class DuplicateEntry(val entry: Entry, val duplicates: List<DuplicateEntryCandidate>) : Dialog
        data class EditDisplayName(val entry: Entry, val initialValue: String) : Dialog
        data class EditMerge(
            val entry: Entry,
            val editReference: EntryMergeEditReference,
            val targetId: Long,
            val targetReference: EntryMergeEditorEntryReference,
            val targetLocked: Boolean,
            val lock: MergeEditorLock?,
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
            val targetLocked: Boolean,
            val lock: MergeEditorLock?,
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
        data object RemoveFromLibrary : Dialog
        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
    }

    fun dismissDialog() {
        mergeTargetSearchController.cancelPending()
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
        mergeTargetSearchController.cancelPending()
        viewModelScope.launchIO {
            val excludedIds = state.mergeGroupMemberIds.toSet()
            val targets = buildMergeTargets(
                libraryItems = getLibraryEntries.await(),
                sourceNames = sourceNames,
                entryType = state.entry.type,
                excludedEntryIds = excludedIds,
            )
            if (targets.isEmpty()) {
                snackbarHostState.showSnackbar(context.stringResource(MR.strings.merge_target_picker_none))
                return@launchIO
            }
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
        val dialog = successState?.dialog as? Dialog.SelectMergeTarget ?: return
        if (dialog.query == query) return
        updateSuccessState { state ->
            val currentDialog = state.dialog as? Dialog.SelectMergeTarget ?: return@updateSuccessState state
            if (currentDialog.targets !== dialog.targets) return@updateSuccessState state
            // The last results stay until the new ones are ranked; emptying them would flash the empty state
            state.copy(dialog = currentDialog.copy(query = query))
        }
        mergeTargetSearchController.submit(dialog.targets, query) { visibleTargets ->
            updateSuccessState { state ->
                val currentDialog = state.dialog as? Dialog.SelectMergeTarget ?: return@updateSuccessState state
                if (currentDialog.targets !== dialog.targets || currentDialog.query != query) {
                    return@updateSuccessState state
                }
                state.copy(dialog = currentDialog.copy(visibleTargets = visibleTargets))
            }
        }
    }

    fun openMergeEditor(targetId: Long) {
        val dialog = successState?.dialog as? Dialog.SelectMergeTarget ?: return
        viewModelScope.launchIO {
            val target = dialog.visibleTargets.firstOrNull { it.id == targetId } ?: return@launchIO
            val editor = createMergeEditorDialog(dialog.entry, target) ?: return@launchIO
            updateSuccessState {
                val current = it.dialog as? Dialog.SelectMergeTarget ?: return@updateSuccessState it
                if (current.targets !== dialog.targets) return@updateSuccessState it
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
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
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

    fun showManageMergeSheet() {
        val state = successState ?: return
        if (!state.isPartOfMerge) return
        viewModelScope.launchIO {
            val editor = getExistingMergeEditor(state.entry) ?: return@launchIO
            val target = editor.entries.firstOrNull { it.reference == editor.target } ?: return@launchIO
            val members = editor.entries.map { editorEntry ->
                MergeMember(
                    id = editorEntry.entry.id,
                    reference = editorEntry.reference,
                    entry = editorEntry.entry,
                    subtitle = getMergeSubtitle(editorEntry.entry),
                    removable = editorEntry.removable,
                    locked = editorEntry.locked,
                )
            }.inMergeSheetOrder(state.entry, target.entry.id).toImmutableList()
            updateSuccessState {
                it.copy(
                    dialog = Dialog.ManageMerge(
                        editReference = editor.editReference,
                        targetId = target.entry.id,
                        savedTargetId = target.entry.id,
                        targetLocked = editor.targetLocked,
                        lock = editor.toMergeEditorLock(::getSourceName),
                        members = members,
                    ),
                )
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        val entry = successState?.entry ?: return
        viewModelScope.launchIO {
            entryRepository.updateDisplayName(entry.id, displayName.toStoredDisplayName(entry.title))
            dismissDialog()
        }
    }

    fun removeMergedMembers(entryIds: List<Long>) {
        val state = successState ?: return
        val dialog = state.dialog as? Dialog.ManageMerge
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
            saveManageMerge(dialog, dialog.removableIds + dialog.libraryRemovalIds)
            dismissDialog()
        }
    }

    private suspend fun saveManageMerge(dialog: Dialog.ManageMerge, entryIdsToRemove: Collection<Long>) {
        val entry = successState?.entry ?: return
        val target = dialog.members.firstOrNull { it.id == dialog.targetId } ?: return
        entryMergeFeature.execute(
            EntryMergeCommitIntent(
                editReference = dialog.editReference,
                target = target.reference,
                // The sheet's order is its own inverse, so applying it again restores the stored order
                orderedEntries = dialog.members.inMergeSheetOrder(entry, dialog.targetId).map(MergeMember::reference),
                removedEntries = dialog.members.referencesFor(entryIdsToRemove - dialog.libraryRemovalIds.toSet()),
                libraryRemovalEntries = dialog.members.referencesFor(dialog.libraryRemovalIds),
            ),
        )
    }

    fun removeMergedEntry(entries: List<Entry>, deleteFromLibrary: Boolean, deleteChapters: Boolean) {
        val state = successState ?: return
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
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

        viewModelScope.launchIO {
            entryScanlatorFilterFeature.setExcludedScanlators(
                scope = EntryScanlatorFilterScope(state.entry, getDisplayedMemberIds()),
                excludedScanlators = excludedScanlators,
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

    private fun EntryType?.presentation() = entryTypePresentation(entryTypePresentationFeature)

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
        val sources = members.ifEmpty { listOf(entry) }
            .map(Entry::source)
            .distinct()
            .map(sourceManager::getDisplayInfo)
        val sourceNameById = sources.associate { it.id to it.getDisplayNameForEntryInfo(sourcePreferences) }
        val sourceName = mergedSourceNames(sourceNameById.values.toList())
        return MergePresentation(
            sourceName = sourceName,
            sourceSearchQuery = sources.singleOrNull()?.librarySearchQuery() ?: sourceName,
            isMultiSource = sources.size > 1,
            isSourceMissing = sources.any(SourceDisplayInfo::isMissing),
            memberIds = memberIds,
            memberTitleById = members.associate { it.id to it.displayTitle },
            memberSourceNameById = if (sources.size > 1) {
                members.associate { it.id to sourceNameById.getValue(it.source) }
            } else {
                emptyMap()
            },
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
            // A merged entry from a source holds no chapters; its members are in the merge already
            !memberEntry.initialized || (
                !memberEntry.mergedBySource &&
                    entryChapterRepository.getChaptersByEntryIdAwait(memberEntry.id, applyScanlatorFilter = false)
                        .isEmpty()
                )
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
                    // The entry being added goes first, so it tops the editor whether the target is merged yet or not.
                    selectedEntries = listOf(entry, targetEntry),
                    root = targetEntry,
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
            lock = editor.toMergeEditorLock(::getSourceName),
            entries = editor.entries.map(::toMergeEditorEntry).toImmutableList(),
            referencesById = editor.entries.associate { it.entry.id to it.reference },
            removedIds = emptySet(),
            libraryRemovalIds = emptySet(),
        )
    }

    private fun toMergeEditorEntry(editorEntry: EntryMergeEditorEntry): MergeEditorEntry {
        val isExisting = editorEntry.origin == EntryMergeEditorEntryOrigin.EXISTING_MEMBER
        return MergeEditorEntry(
            id = editorEntry.entry.id,
            entry = editorEntry.entry,
            subtitle = getMergeSubtitle(editorEntry.entry) + if (isExisting) "" else " • New",
            isRemovable = editorEntry.removable,
            isMember = isExisting,
            locked = editorEntry.locked,
        )
    }

    private fun getSourceName(sourceId: Long): String = sourceManager.getDisplayInfo(sourceId).name

    private fun getMergeSubtitle(entry: Entry): String {
        val sourceName = getSourceName(entry.source)
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

    private data class MergePresentation(
        val sourceName: String,
        val sourceSearchQuery: String,
        val isMultiSource: Boolean,
        /** Whether any member's source is missing, not just the visible entry's. */
        val isSourceMissing: Boolean,
        val memberIds: ImmutableList<Long>,
        val memberTitleById: Map<Long, String>,
        val memberSourceNameById: Map<Long, String>,
        val mergeTargetId: Long,
        val mergeGroupMemberIds: ImmutableList<Long>,
    )

    private data class ContinueTargetRequest(
        val entry: Entry,
        val chapters: List<ContinueTargetChapter>,
    )

    /** What Continue's pick and the list's filters read from a chapter, so a change to any of them picks again. */
    private data class ContinueTargetChapter(
        val id: Long,
        val read: Boolean,
        val bookmark: Boolean,
        val kind: ChapterKind,
        val isDownloaded: Boolean,
    )

    @Immutable
    data class MergeMember(
        val id: Long,
        val reference: EntryMergeEditorEntryReference,
        val entry: Entry,
        val subtitle: String,
        val removable: Boolean,
        val locked: Boolean,
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
            /** What tapping the source line searches the library for. */
            val sourceSearchQuery: String,
            val isMultiSource: Boolean,
            val isSourceMissing: Boolean,
            val memberIds: ImmutableList<Long>,
            val memberTitleById: Map<Long, String>,
            val memberSourceNameById: Map<Long, String>,
            val mergeTargetId: Long,
            val mergeGroupMemberIds: ImmutableList<Long>,
            val isFromSource: Boolean,
            val chapters: List<EntryChapterList.Item>,
            val continueTargetChapterId: Long? = null,
            val childProgressLabels: Map<Long, EntryChildProgressLabel> = emptyMap(),
            /** Until the labels first load, whether reading has started is unknown. */
            val childProgressLoaded: Boolean = false,
            val childListFeature: EntryChildListFeature,
            val scanlatorFilterFeature: EntryScanlatorFilterFeature,
            val scanlatorFilterState: EntryScanlatorFilterStateResult,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val duplicateCandidates: List<DuplicateEntryCandidate> = emptyList(),
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val hideMissingChapters: Boolean = false,
            val downloadedOnly: Boolean = false,
            val memberExpansion: SectionExpansion<Long> = SectionExpansion(),
            val chapterGroups: List<EntryChapterGroup> = emptyList(),
            /** Null once every chapter list is complete. */
            val olderChapters: OlderChaptersStatus? = null,
            val groupExpansion: SectionExpansion<ChapterGroupId> = SectionExpansion(),
            private val chapterPresentation: EntryChapterPresentation = buildChapterPresentation(
                entry = entry,
                chapters = chapters,
                memberIds = memberIds,
                memberTitleById = memberTitleById,
                memberSourceNameById = memberSourceNameById,
                expandedMemberIds = memberExpansion.shown(chapters.memberOf(continueTargetChapterId)),
                chapterGroups = chapterGroups,
                expandedGroupIds = groupExpansion.shown(chapters.groupOf(continueTargetChapterId)),
                childListFeature = childListFeature,
                scanlatorFilterFeature = scanlatorFilterFeature,
                scanlatorFilterState = scanlatorFilterState,
                hideMissingChapters = hideMissingChapters,
                downloadedOnly = downloadedOnly,
            ),
        ) : State {
            fun containsChapter(download: EntryDownloadStatus): Boolean =
                chapter(download) != null

            fun chapter(download: EntryDownloadStatus): EntryChapterList.Item? {
                val item = chapterPresentation.rawIndexOf(download.chapterId)?.let(chapters::get) ?: return null
                return item.takeIf { it.entry.type == download.entryType }
            }

            fun updateChapterItems(
                items: Collection<EntryChapterList.Item>,
                rebuildPresentation: Boolean = false,
            ): Success {
                if (items.isEmpty()) return this
                var updatedChapters: PersistentList<EntryChapterList.Item> = chapters.toPersistentList()
                items.forEach { item ->
                    chapterPresentation.rawIndexOf(item.id)?.let { index ->
                        updatedChapters = updatedChapters.replacingAt(index, item)
                    }
                }
                return if (rebuildPresentation) {
                    copy(chapters = updatedChapters).rebuildChapterPresentation()
                } else {
                    copy(
                        chapters = updatedChapters,
                        chapterPresentation = chapterPresentation.updateItems(items),
                    )
                }
            }

            val processedChapters: List<EntryChapterList.Item>
                get() = chapterPresentation.processedChapters

            val readingChapters by lazy {
                processedChapters.sortedForReading(entry, memberIds, childListFeature)
            }

            val isAnySelected: Boolean
                get() = chapterPresentation.isAnySelected

            val selectedChapters: List<EntryChapterList.Item>
                get() = chapterPresentation.selectedChapters

            val hasUnreadChapters: Boolean
                get() = chapterPresentation.hasUnreadChapters

            val hasReadChapters: Boolean
                get() = chapterPresentation.hasReadChapters

            /** A chapter is finished or holds a position partway through, like a single-file book being read. */
            val hasStartedReading: Boolean
                get() = hasReadChapters || childProgressLabels.isNotEmpty()

            val chapterProgress: EntryChapterProgress
                get() = chapterPresentation.progress

            val hasFiller: Boolean
                get() = chapterPresentation.hasFiller

            val hasChapterPreviews: Boolean
                get() = chapterPresentation.hasPreviews

            /** The chapter the continue action opens, if there is one. */
            val continueTargetChapter: EntryChapter?
                get() = continueTargetChapterId?.let { id -> chapters.firstOrNull { it.chapter.id == id }?.chapter }

            fun chapterListIndex(chapterId: Long?): Int? =
                chapterId?.let(chapterPresentation::rowIndexOf)

            fun processedChapterIndex(chapterId: Long): Int? =
                chapterPresentation.processedIndexOf(chapterId)

            val chapterListItems: List<EntryChapterList>
                get() = chapterPresentation.rows

            val missingChildCount: Int
                get() = chapterPresentation.aggregateMissingCount

            val downloadSourceIds: Set<Long>
                get() = chapterPresentation.sourceIds

            /** The merged member holding the chapter to continue with, which opens expanded. */
            val currentMemberId: Long?
                get() = chapters.memberOf(continueTargetChapterId)

            val expandedMemberIds: Set<Long>
                get() = memberExpansion.shown(currentMemberId)

            /** The group holding the chapter to continue with, which opens expanded. */
            val currentGroupId: ChapterGroupId?
                get() = chapters.groupOf(continueTargetChapterId)

            val expandedGroupIds: Set<ChapterGroupId>
                get() = groupExpansion.shown(currentGroupId)

            fun rebuildChapterPresentation(): Success {
                return copy(
                    chapterPresentation = buildChapterPresentation(
                        entry = entry,
                        chapters = chapters,
                        memberIds = memberIds,
                        memberTitleById = memberTitleById,
                        memberSourceNameById = memberSourceNameById,
                        expandedMemberIds = expandedMemberIds,
                        chapterGroups = chapterGroups,
                        expandedGroupIds = expandedGroupIds,
                        childListFeature = childListFeature,
                        scanlatorFilterFeature = scanlatorFilterFeature,
                        scanlatorFilterState = scanlatorFilterState,
                        hideMissingChapters = hideMissingChapters,
                        downloadedOnly = downloadedOnly,
                    ),
                )
            }

            val childListApplicable: Boolean
                get() = childListFeature.isApplicable(entry.type)

            val scanlatorFilterActive: Boolean
                get() = (scanlatorFilterState as? EntryScanlatorFilterStateResult.Available)?.state?.active == true

            val scanlatorFilterApplicable: Boolean
                get() = scanlatorFilterState is EntryScanlatorFilterStateResult.Available

            val filterActive: Boolean
                get() = scanlatorFilterActive || EntryChapterFilters(entry, downloadedOnly).active

            val isPartOfMerge: Boolean
                get() = mergeGroupMemberIds.size > 1

            val isMerged: Boolean
                get() = memberIds.size > 1

            val showMergeNotice: Boolean
                get() = isPartOfMerge && !isMerged && mergeTargetId != entry.id
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
        /** Null when every member comes from one source. */
        val sourceName: String?,
        val childCount: Int,
        val unreadCount: Int,
        val expanded: Boolean,
    ) : EntryChapterList()

    /**
     * Heads one group of chapters; a null [title] is the chapters in no group, and a [childCount] of 0 a group with no
     * chapters yet. A [nested] header sits inside its merged member's card.
     */
    @Immutable
    data class GroupHeader(
        val group: ChapterGroupId,
        val title: String?,
        val childCount: Int,
        val unreadCount: Int,
        val expanded: Boolean,
        val nested: Boolean,
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
        val selected: Boolean = false,
    ) : EntryChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == EntryDownloadState.DOWNLOADED
    }
}

/**
 * [this] stored merge order as the Manage merge sheet lists it: like the episode list, with the merged entry itself, which
 * the source keeps first, staying on top.
 */
private fun List<EntryViewModel.MergeMember>.inMergeSheetOrder(
    entry: Entry,
    targetId: Long,
): List<EntryViewModel.MergeMember> {
    if (!entry.showsMergedMembersReversed) return this
    val (target, others) = partition { it.id == targetId }
    return target + others.asReversed()
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

internal fun <DownloadChanges> mergeAwareEntryAndChaptersFlow(
    entryAndChaptersFlow: Flow<Pair<Entry, List<EntryChapter>>>,
    downloadChangesFlow: Flow<DownloadChanges>,
): Flow<Pair<Entry, List<EntryChapter>>> {
    return combine(
        entryAndChaptersFlow,
        downloadChangesFlow,
    ) { entryAndChapters, _ -> entryAndChapters }
}

/** Compatibility overload for callers that previously supplied queue changes as a refresh signal. */
internal fun <DownloadChanges, DownloadQueue> mergeAwareEntryAndChaptersFlow(
    entryAndChaptersFlow: Flow<Pair<Entry, List<EntryChapter>>>,
    downloadChangesFlow: Flow<DownloadChanges>,
    downloadQueueFlow: Flow<DownloadQueue>,
): Flow<Pair<Entry, List<EntryChapter>>> {
    return mergeAwareEntryAndChaptersFlow(entryAndChaptersFlow, downloadChangesFlow)
}

private fun Map<Long, EntryMergeEditorEntryReference>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filterKeys { it in ids }.values.toSet()
}

private fun List<EntryViewModel.MergeMember>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filter { it.id in ids }.mapTo(linkedSetOf(), EntryViewModel.MergeMember::reference)
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

private fun Long.setFillerFilter(flag: Long): Long {
    return this and Entry.CHAPTER_FILLER_MASK.inv() or flag
}

private fun Long.setShowChapterPreviews(show: Boolean): Long {
    return if (show) this and Entry.CHAPTER_HIDE_PREVIEWS.inv() else this or Entry.CHAPTER_HIDE_PREVIEWS
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

private fun List<EntryChapterList.Item>.memberOf(chapterId: Long?): Long? {
    return chapterId?.let { id -> firstOrNull { it.id == id }?.chapter?.entryId }
}

private fun List<EntryChapterList.Item>.groupOf(chapterId: Long?): ChapterGroupId? {
    return chapterId?.let { id -> firstOrNull { it.id == id }?.chapter?.groupId }
}
