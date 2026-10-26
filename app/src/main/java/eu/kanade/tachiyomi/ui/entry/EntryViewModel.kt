package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.base.viewmodel.GatedStateViewModel
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterDisplaySettings
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterMarks
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterPresentation
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterProgress
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterSelection
import eu.kanade.tachiyomi.ui.entry.chapters.buildChapterPresentation
import eu.kanade.tachiyomi.ui.entry.downloads.EntryChapterDownloads
import eu.kanade.tachiyomi.ui.entry.library.EntryLibraryModel
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import eu.kanade.tachiyomi.ui.entry.merge.EntryMergeModel
import eu.kanade.tachiyomi.ui.entry.olderchapters.EntryOlderChapters
import eu.kanade.tachiyomi.ui.entry.olderchapters.OlderChaptersStatus
import eu.kanade.tachiyomi.ui.entry.preview.EntryPreviewModel
import eu.kanade.tachiyomi.ui.entry.refresh.EntrySourceRefresh
import eu.kanade.tachiyomi.ui.entry.translation.ChapterTranslationModel
import eu.kanade.tachiyomi.util.lang.toStoredDisplayName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
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
import mihon.entry.interactions.child.EntryChildOrderResult
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.child.EntryChildProgressResult
import mihon.entry.interactions.child.EntryScanlatorFilterFeature
import mihon.entry.interactions.child.EntryScanlatorFilterObservationResult
import mihon.entry.interactions.child.EntryScanlatorFilterScope
import mihon.entry.interactions.child.EntryScanlatorFilterStateResult
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueTargetResult
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.tracking.EntryTrackingAvailability
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingSession
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.pausedWhileUnsubscribed
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
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
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.service.EntryChapterFilters
import tachiyomi.domain.entry.service.SectionExpansion
import tachiyomi.domain.entry.service.expansionSession
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*

@AssistedInject
class EntryViewModel(
    @Assisted private val context: Context,
    @Assisted private val entryId: Long,
    @Assisted private val isFromSource: Boolean,
    @Assisted bypassMerge: Boolean,
    private val libraryPreferences: LibraryPreferences,
    private val downloadRuntime: EntryDownloadRuntimeFeature,
    private val entryMigrationFeature: EntryMigrationFeature,
    private val entryContinueFeature: EntryContinueFeature,
    private val entryChildListFeature: EntryChildListFeature,
    private val entryScanlatorFilterFeature: EntryScanlatorFilterFeature,
    private val entryMergeFeature: EntryMergeFeature,
    private val entryMergeCandidateFeature: EntryMergeCandidateFeature,
    private val getEntry: GetEntry,
    private val setEntryDefaultChapterFlags: SetEntryDefaultChapterFlags,
    private val updateEntry: UpdateEntry,
    private val entryRepository: EntryRepository,
    mergedMemberExpansionRepository: MergedMemberExpansionRepository,
    private val entryChapterGroupRepository: EntryChapterGroupRepository,
    chapterGroupExpansionRepository: ChapterGroupExpansionRepository,
    private val entryTrackingFeature: EntryTrackingFeature,
    private val sourceManager: SourceManager,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
    membersFactory: EntryMembers.Factory,
    sourceRefreshFactory: EntrySourceRefresh.Factory,
    previewFactory: EntryPreviewModel.Factory,
    mergeFactory: EntryMergeModel.Factory,
    libraryFactory: EntryLibraryModel.Factory,
    displaySettingsFactory: EntryChapterDisplaySettings.Factory,
    marksFactory: EntryChapterMarks.Factory,
    downloadsFactory: EntryChapterDownloads.Factory,
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

    /** Translation of the listed chapters; chapters to translate that are not downloaded yet are downloaded. */
    val translation = ChapterTranslationModel(
        scope = viewModelScope,
        context = context,
        snackbarHostState = snackbarHostState,
        download = { chapters ->
            val ids = chapters.mapTo(HashSet()) { it.id }
            downloads.start(successState?.chapters.orEmpty().filter { it.id in ids }, startNow = false)
        },
        feature = translateFeature,
        languages = translationLanguages,
        recognitionHost = recognitionHost,
        translationHost = translationHost,
        modelStore = modelStore,
    )

    private val successState: State.Success?
        get() = mutableState.value as? State.Success

    private val stateAccess = object : EntryStateAccess {
        override val loaded: State.Success?
            get() = successState

        override fun update(transform: (State.Success) -> State.Success) = updateSuccessState(transform)
    }

    private val members = membersFactory.create(entryId, bypassMerge)

    val sourceRefresh: EntrySourceRefresh = sourceRefreshFactory.create(
        viewModelScope,
        context,
        snackbarHostState,
        stateAccess,
        members,
    )

    val preview: EntryPreviewModel = previewFactory.create(
        scope = viewModelScope,
        gatedScope = gatedScope,
        context = context,
        entryId = entryId,
        state = stateAccess,
        members = members,
        sourceRefresh = sourceRefresh,
        chapterItems = { entry -> downloads.chapterItems(members.displayedChapters(), entry) },
    )

    val merge: EntryMergeModel = mergeFactory.create(viewModelScope, context, snackbarHostState, stateAccess, members)

    val library: EntryLibraryModel = libraryFactory.create(
        viewModelScope,
        context,
        snackbarHostState,
        stateAccess,
        members,
    )

    val selection = EntryChapterSelection(stateAccess)

    val displaySettings: EntryChapterDisplaySettings = displaySettingsFactory.create(
        viewModelScope,
        context,
        snackbarHostState,
        stateAccess,
        members,
    )

    val marks: EntryChapterMarks = marksFactory.create(
        viewModelScope,
        context,
        snackbarHostState,
        stateAccess,
        selection,
    )

    val downloads: EntryChapterDownloads = downloadsFactory.create(
        scope = viewModelScope,
        context = context,
        snackbarHostState = snackbarHostState,
        state = stateAccess,
        members = members,
        selection = selection,
        library = library,
    )

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

    val chapterSwipeStartAction = libraryPreferences.swipeToEndAction.get()
    val chapterSwipeEndAction = libraryPreferences.swipeToStartAction.get()

    private var duplicateObservationJob: Job? = null

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

    init {
        observeChildProgressLabels()
        observeScanlatorFilter()
        observeContinueTarget()
        observeExpandedMembers()
        observeChapterGroups()
        observeOlderChapters()
        observeExpandedGroups()

        gatedScope.launchIO {
            // Chapter items read their download states when built, so a change to the downloads builds them again
            val downloadChanges = downloadRuntime.changes.pausedWhileUnsubscribed()
            combine(members.entryAndChapters(), downloadChanges) { entryAndChapters, _ -> entryAndChapters }
                .collectLatest { (entry, chapters) ->
                    preview.observeConfig(entry)
                    val mergePresentation = members.presentation(entry)
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
                            chapters = downloads.chapterItems(chapters, entry),
                            duplicateCandidates = if (it.isFromSource && !entry.favorite) {
                                it.duplicateCandidates
                            } else {
                                emptyList()
                            },
                        ).rebuildChapterPresentation()
                    }
                }
        }

        viewModelScope.launchIO {
            val entry = getEntry.await(entryId)
            if (entry == null) {
                mutableState.update { State.Error }
                return@launchIO
            }

            val mergePresentation = members.presentation(entry)
            val chapters = downloads.chapterItems(members.displayedChapters(), entry)
            val scanlatorFilterState = entryScanlatorFilterFeature.state(
                EntryScanlatorFilterScope(entry, mergePresentation.memberIds),
            )

            if (!entry.favorite) {
                setEntryDefaultChapterFlags.await(listOf(entry.id))
            }

            val membersNeedingRefresh = members.needingRefresh(current = null)
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
            preview.observeConfig(entry)
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
                sourceRefresh.launch(
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
        if (group in successState.expandedGroupIds) selection.deselect { it.groupId == group }
        groupExpansion.toggle(group, successState.currentGroupId)
    }

    /** Selects the chapters of [group] the filters show, opening it so they're in sight, or clears them if all are. */
    fun toggleGroupSelection(group: ChapterGroupId) {
        val successState = successState ?: return
        val groupItems = successState.processedChapters.filter { it.chapter.groupId == group }
        if (groupItems.isEmpty()) return
        if (groupItems.all(EntryChapterList.Item::selected)) {
            selection.deselect { it.groupId == group }
            return
        }
        if (group !in successState.expandedGroupIds) groupExpansion.toggle(group, successState.currentGroupId)
        selection.selectShown { it.groupId == group }
    }

    fun toggleMemberExpanded(memberId: Long) {
        val successState = successState ?: return
        // Chapters hidden by collapsing stay out of the selection, so selection actions only reach chapters in sight.
        if (memberId in successState.expandedMemberIds) selection.deselect { it.entryId == memberId }
        memberExpansion.toggle(memberId, successState.currentMemberId)
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
            members.displayedIdChanges()
                .flatMapLatest { (entry, memberIds) ->
                    val scope = EntryScanlatorFilterScope(entry, memberIds)
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

    override fun onCleared() {
        super.onCleared()
        duplicateObservationJob?.cancel()
        preview.release()
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
                marks.markRead(listOf(chapter), !chapter.read)
            }
            LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> {
                marks.bookmark(listOf(chapter), !chapter.bookmark)
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
                downloads.runChapterActions(
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

    sealed interface Dialog {
        data class DeleteChapters(val chapters: List<EntryChapter>) : Dialog

        data class EditDisplayName(val entry: Entry, val initialValue: String) : Dialog
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

    fun showCoverDialog() {
        updateSuccessState { it.copy(dialog = Dialog.FullCover) }
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

    fun updateDisplayName(displayName: String) {
        val entry = successState?.entry ?: return
        viewModelScope.launchIO {
            entryRepository.updateDisplayName(entry.id, displayName.toStoredDisplayName(entry.title))
            dismissDialog()
        }
    }

    private fun EntryType?.presentation() = entryTypePresentation(entryTypePresentationFeature)

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

private fun List<EntryChapterList.Item>.memberOf(chapterId: Long?): Long? {
    return chapterId?.let { id -> firstOrNull { it.id == id }?.chapter?.entryId }
}

private fun List<EntryChapterList.Item>.groupOf(chapterId: Long?): ChapterGroupId? {
    return chapterId?.let { id -> firstOrNull { it.id == id }?.chapter?.groupId }
}
