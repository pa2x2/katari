package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.library.components.LibraryDisplaySettings
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import eu.kanade.tachiyomi.ui.library.grouping.resolveLibraryPages
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import mihon.core.common.utils.mutate
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.download.EntryBulkDownloadAction
import mihon.entry.interactions.download.EntryBulkDownloadRequest
import mihon.entry.interactions.download.EntryBulkDownloadResolutionResult
import mihon.entry.interactions.download.EntryDownloadActionAvailability
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadActionRequest
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.library.EntryLibraryFilterAvailability
import mihon.entry.interactions.library.EntryLibraryFilterControlAvailability
import mihon.entry.interactions.library.EntryLibraryFilterFeature
import mihon.entry.interactions.library.EntryLibraryFilterPolicy
import mihon.entry.interactions.library.EntryLibraryFilterRequest
import mihon.entry.interactions.library.EntryLibraryFilterTarget
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.library.membership.EntryLibraryRemovalResult
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveConflictResolution
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveFeature
import mihon.entry.interactions.lifecycle.profile.EntryProfileMovePreview
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveRequest
import mihon.entry.interactions.lifecycle.profile.EntryProfileMoveResult
import mihon.entry.interactions.merge.EntryMergeCommitIntent
import mihon.entry.interactions.merge.EntryMergeEditReference
import mihon.entry.interactions.merge.EntryMergeEditorEntryReference
import mihon.entry.interactions.merge.EntryMergeExecutionResult
import mihon.entry.interactions.merge.EntryMergeFeature
import mihon.entry.interactions.merge.EntryMergePreparationResult
import mihon.entry.interactions.merge.EntryMergePrepareIntent
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.state.EntryConsumptionFeature
import mihon.entry.interactions.tracking.EntryTrackingAccount
import mihon.entry.interactions.tracking.EntryTrackingCollectionTrack
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.feature.library.search.LibrarySearchMatcher
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileScopedStateEvent
import mihon.feature.profiles.core.observeProfileScopedState
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetLibraryPinned
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibrarySortKey
import tachiyomi.domain.library.service.librarySortComparator
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class LibraryScreenModel(
    private val context: Context,
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val setLibraryPinned: SetLibraryPinned = Injekt.get(),
    private val setEntryCategories: SetEntryCategories = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    private val entryDownloadActionFeature: EntryDownloadActionFeature = Injekt.get(),
    private val entryMigrationFeature: EntryMigrationFeature = Injekt.get(),
    private val entryMergeFeature: EntryMergeFeature = Injekt.get(),
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature = Injekt.get(),
    private val entryConsumptionFeature: EntryConsumptionFeature = Injekt.get(),
    private val entryLibraryFilterFeature: EntryLibraryFilterFeature = Injekt.get(),
    private val entryCatalogueFeature: EntryCatalogueFeature = Injekt.get(),
    private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    private val profileStore: ProfileAwareStore = Injekt.get(),
    private val profileDatabase: ProfileDatabase = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val entryProfileMoveFeature: EntryProfileMoveFeature = Injekt.get(),
) : StateScreenModel<LibraryScreenModel.State>(State()) {

    val moveEvents = Channel<MoveEvent>(Channel.BUFFERED)
    private var moveInProgress = false
    private val pagePersistenceRequests = Channel<PagePersistenceRequest>(Channel.UNLIMITED)

    private val displayModeState = libraryPreferences.displayMode.asState(screenModelScope)
    private val portraitColumnsState = libraryPreferences.portraitColumns.asState(screenModelScope)
    private val landscapeColumnsState = libraryPreferences.landscapeColumns.asState(screenModelScope)

    init {
        screenModelScope.launchIO {
            for (request in pagePersistenceRequests) {
                val preferences = LibraryPreferences(profileStore.profileStore(request.profileId))
                if (preferences.lastUsedCategory.get() != request.pageIndex) {
                    preferences.lastUsedCategory.set(request.pageIndex)
                }
                request.pageId?.let { pageId ->
                    if (preferences.lastUsedPageId.get() != pageId) {
                        preferences.lastUsedPageId.set(pageId)
                    }
                }
            }
        }
        mutableState.update { state ->
            state.copy(activePageIndex = libraryPreferences.lastUsedCategory.get())
        }
        state.map(::selectionActionInput)
            .distinctUntilChanged()
            .mapLatest(::resolveSelectionActions)
            .flowOn(Dispatchers.IO)
            .onEach { actions ->
                mutableState.update { state -> state.copy(selectionActions = actions) }
            }
            .launchIn(screenModelScope)
        screenModelScope.launchIO {
            observeProfileScopedState(profileStore.currentProfileIdFlow) { profileId ->
                combine(
                    state.map { it.searchQuery }.distinctUntilChanged().debounce(0.25.seconds),
                    getCategories.subscribeForProfile(profileId),
                    getLibraryItemsFlow(profileId),
                    combine(trackingFeature.observeCollection(), getTrackingFiltersFlow(), ::Pair),
                    observeLibraryFilterPreferences(LibraryPreferences(profileStore.profileStore(profileId))),
                ) { searchQuery, categories, favorites, (tracking, trackingFilters), itemPreferences ->
                    val showSystemCategory = favorites.any { it.categories.contains(0L) }
                    val categoryNamesById = categories.associate { it.id to it.name }
                    val searchMatcher = searchQuery?.let { query ->
                        LibrarySearchMatcher(
                            query = query,
                            categoryNamesById = categoryNamesById,
                            sourceDisplayName = { item -> item.getSourceDisplayName(sourceManager) },
                            sourceNames = { item ->
                                item.sourceIds.map { sourceId -> sourceManager.getDisplayInfo(sourceId).name }
                            },
                        )
                    }
                    val filterResult = favorites.applyFilters(tracking.entries, trackingFilters, itemPreferences)
                    val filteredFavorites = filterResult.items
                        .let {
                            if (searchMatcher == null) {
                                it
                            } else {
                                it.filter(searchMatcher::matches)
                            }
                        }

                    LibraryData(
                        profileId = profileId,
                        isInitialized = true,
                        showSystemCategory = showSystemCategory,
                        categories = categories,
                        favorites = filteredFavorites,
                        trackingEntries = tracking.entries,
                        trackingScoreSupportedEntryTypes = tracking.scoreSupportedEntryTypes,
                        hasActiveFilters = filterResult.hasActiveFilters,
                        filterAvailability = filterResult.availability,
                    )
                }.distinctUntilChanged()
            }.collectLatest { event ->
                when (event) {
                    is ProfileScopedStateEvent.Reset -> {
                        val scopedLibraryPreferences = LibraryPreferences(
                            profileStore.profileStore(event.profileId),
                        )
                        mutableState.update { state ->
                            state.copy(
                                isLoading = true,
                                selection = emptySet(),
                                selectionActions = SelectionActions(),
                                hasActiveFilters = false,
                                dialog = null,
                                libraryData = LibraryData(),
                                activePageIndex = scopedLibraryPreferences.lastUsedCategory.get(),
                                groupedFavorites = emptyList(),
                                pageItemsById = emptyMap(),
                            )
                        }
                        lastSelectionPageId = null
                    }
                    is ProfileScopedStateEvent.Value -> {
                        mutableState.update { state ->
                            state.copy(
                                libraryData = event.value,
                                hasActiveFilters = event.value.hasActiveFilters,
                            )
                        }
                    }
                }
            }
        }

        screenModelScope.launchIO {
            profileStore.currentProfileIdFlow
                .distinctUntilChanged()
                .flatMapLatest { profileId ->
                    val scopedLibraryPreferences = LibraryPreferences(profileStore.profileStore(profileId))
                    observeGroupedLibraryPages(
                        libraryData = state
                            .map { it.libraryData }
                            .filter { it.isInitialized && it.profileId == profileId }
                            .distinctUntilChanged(),
                        grouping = scopedLibraryPreferences.grouping.changes(),
                        sortingMode = scopedLibraryPreferences.sortingMode.changes(),
                        randomSortSeed = scopedLibraryPreferences.randomSortSeed.changes(),
                        applyGrouping = { data, grouping ->
                            resolveLibraryPages(
                                items = data.favorites,
                                categories = data.categories,
                                showSystemCategory = data.showSystemCategory,
                                grouping = grouping,
                                libraryTitle = context.stringResource(MR.strings.label_library),
                                entryTypeTitle = { entryType ->
                                    context.stringResource(entryType.entryTypePresentation().displayNameLabel)
                                },
                            )
                        },
                        applySort = { pages, data, sortingMode, randomSortSeed ->
                            pages.applySort(
                                favoritesById = data.favoritesById,
                                trackingEntries = data.trackingEntries,
                                trackingScoreSupportedEntryTypes = data.trackingScoreSupportedEntryTypes,
                                globalSort = sortingMode,
                                randomSortSeed = randomSortSeed,
                            )
                        },
                    )
                }
                .collectLatest { groupedPages ->
                    mutableState.update { state ->
                        if (state.libraryData.profileId != groupedPages.profileId) return@update state

                        val scopedLibraryPreferences = LibraryPreferences(
                            profileStore.profileStore(groupedPages.profileId),
                        )
                        val activePageIndex = groupedPages.pages.indexOfMatchingPage(
                            previousPage = state.activePage,
                            restoredPageId = scopedLibraryPreferences.lastUsedPageId.get(),
                            fallbackPageIndex = state.requestedActivePageIndex,
                            previousGrouping = state.grouping,
                            newGrouping = groupedPages.grouping,
                        )
                        state.copy(
                            isLoading = false,
                            groupedFavorites = groupedPages.pages,
                            pageItemsById = groupedPages.itemsByPageId,
                            grouping = groupedPages.grouping,
                            activePageIndex = activePageIndex,
                        )
                    }
                }
        }

        combine(
            libraryPreferences.categoryTabs.changes(),
            libraryPreferences.categoryNumberOfItems.changes(),
            libraryPreferences.showContinueReadingButton.changes(),
        ) { a, b, c -> arrayOf(a, b, c) }
            .onEach { (showCategoryTabs, showEntryCount, showContinueButton) ->
                mutableState.update { state ->
                    state.copy(
                        showCategoryTabs = showCategoryTabs,
                        showEntryCount = showEntryCount,
                        showContinueButton = showContinueButton,
                    )
                }
            }
            .launchIn(screenModelScope)

        observeLibraryDisplaySettings(libraryPreferences)
            .onEach { displaySettings ->
                mutableState.update { state ->
                    state.copy(displaySettings = displaySettings)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun List<LibraryItem>.applyFilters(
        trackingEntries: Map<Long, List<EntryTrackingCollectionTrack>>,
        trackingFilter: Map<Long, TriState>,
        preferences: LibraryFilterPreferences,
    ): AppliedLibraryFilters {
        val targets = map { item ->
            EntryLibraryFilterTarget(
                type = item.entry.type,
                isDownloadedOrLocal = item.isLocal || item.downloadCount > 0,
                hasUnconsumed = item.unconsumedCount?.let { it > 0 },
                hasStarted = item.hasStarted,
                hasBookmarks = item.hasBookmarks,
                isCompleted = item.entry.status == EntryStatus.COMPLETED,
                isOutsideReleasePeriod = item.entry.fetchInterval < 0,
                trackerIds = trackingEntries[item.key.id].orEmpty()
                    .mapTo(mutableSetOf()) { it.serviceId.value },
            )
        }
        val result = entryLibraryFilterFeature.filter(
            EntryLibraryFilterRequest(
                targets = targets,
                policy = EntryLibraryFilterPolicy(
                    downloadedOnly = preferences.globalFilterDownloaded,
                    downloaded = preferences.filterDownloaded,
                    unconsumed = preferences.filterUnread,
                    notStarted = preferences.filterNotStarted,
                    bookmarked = preferences.filterBookmarked,
                    completed = preferences.filterCompleted,
                    outsideReleasePeriod = preferences.filterIntervalCustom,
                    outsideReleasePeriodEnabled = !isReleaseBuildType && preferences.skipOutsideReleasePeriod,
                    tracking = trackingFilter,
                ),
            ),
        )
        return AppliedLibraryFilters(
            items = result.includedTargetIndices.map(::get),
            hasActiveFilters = result.hasActiveFilters,
            availability = result.availability,
        )
    }

    private fun List<LibraryPage>.applySort(
        favoritesById: Map<LibraryItemKey, LibraryItem>,
        trackingEntries: Map<Long, List<EntryTrackingCollectionTrack>>,
        trackingScoreSupportedEntryTypes: Set<EntryType>,
        globalSort: LibrarySort,
        randomSortSeed: Int,
    ): List<LibraryPage> {
        val defaultTrackerScoreSortValue = -1.0
        val trackerScores by lazy {
            trackingEntries.mapValues { entry ->
                when {
                    entry.value.isEmpty() -> null
                    else -> entry.value.map(EntryTrackingCollectionTrack::normalizedScore).average()
                }
            }
        }

        return map { page ->
            val sort = page.category.effectiveLibrarySort(globalSort)
            if (sort.type == LibrarySort.Type.Random) {
                return@map page.copy(
                    itemIds = page.itemIds
                        .shuffled(Random(randomSortSeed))
                        .prioritizePinned(favoritesById),
                )
            }

            val comparator = librarySortComparator(
                sort = sort,
                trackerScores = trackerScores,
                defaultTrackerScore = defaultTrackerScoreSortValue,
            )
            val itemsWithSortKeys = page.itemIds.mapNotNull { itemId ->
                favoritesById[itemId]?.let { item ->
                    itemId to item.toLibrarySortKey(
                        trackingScoreSupportedEntryTypes,
                        defaultTrackerScoreSortValue,
                    )
                }
            }

            val sortedItemIds = itemsWithSortKeys
                .sortedWith { first, second -> comparator.compare(first.second, second.second) }
                .map { it.first }

            page.copy(itemIds = sortedItemIds.prioritizePinned(favoritesById))
        }
    }

    private fun LibraryItem.toLibrarySortKey(
        trackerSupportedEntryTypes: Set<EntryType>,
        defaultTrackerScoreSortValue: Double,
    ): LibrarySortKey {
        return LibrarySortKey(
            id = entry.id,
            title = entry.displayTitle,
            lastRead = lastRead,
            lastUpdate = entry.lastUpdate,
            unreadCount = unconsumedCount,
            totalEntries = totalCount,
            latestUpload = latestUpload,
            entryFetchDate = entry.lastUpdate,
            dateAdded = entry.dateAdded,
            trackerScore = if (entry.type in trackerSupportedEntryTypes) null else defaultTrackerScoreSortValue,
        )
    }

    private fun getLibraryItemsFlow(profileId: Long): Flow<List<LibraryItem>> {
        val enrichedItems = combine(
            getLibraryEntries.subscribe(profileId),
            downloadRuntime.changes,
        ) { items, _ ->
            items.enrichEntryItems()
        }
        return enrichedItems.flatMapLatest { initialItems ->
            observeLibraryDownloadCountUpdates(
                initialItems = initialItems,
                statusUpdates = downloadRuntime.statusUpdates(),
                calculateDownloadCount = { item -> item.calculateDownloadCount(downloadRuntime) },
            )
        }
    }

    private fun List<LibraryItem>.enrichEntryItems(): List<LibraryItem> {
        val multiSourceName = context.stringResource(MR.strings.multi_lang)
        return map { item ->
            val isMulti = item.displaySourceId == LibraryItem.MULTI_SOURCE_ID
            val displayUnifiedSource = if (isMulti) null else sourceManager.getOrStub(item.displaySourceId)
            val sourceDisplayInfo = if (isMulti) null else sourceManager.getDisplayInfo(item.displaySourceId)
            val sourceName = if (isMulti) multiSourceName else sourceDisplayInfo?.name.orEmpty()
            val sourceLanguage = if (isMulti) {
                LibraryItem.MULTI_SOURCE_ID.toString()
            } else {
                sourceDisplayInfo?.lang.orEmpty()
            }
            val downloadCount = item.calculateDownloadCount(downloadRuntime)

            item.copy(
                sourceName = sourceName,
                sourceLanguage = sourceLanguage,
                sourceItemOrientation = displayUnifiedSource?.let { entryCatalogueFeature.description(it.id) }
                    ?.itemOrientation
                    ?: EntryItemOrientation.VERTICAL,
                isLocal = item.sourceIds.size == 1 && item.entry.source == LocalSource.ID,
                downloadCount = downloadCount,
            )
        }
    }

    /**
     * Flow of tracking filter preferences
     *
     * @return map of track id with the filter value
     */
    private fun getTrackingFiltersFlow(): Flow<Map<Long, TriState>> {
        return trackingFeature.observeAccounts().flatMapLatest { snapshot ->
            val loggedInServices = snapshot.accounts.filter(EntryTrackingAccount::isLoggedIn)
            if (loggedInServices.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val filterFlows = loggedInServices.map { account ->
                    val serviceId = account.service.id.value
                    libraryPreferences.filterTracking(serviceId.toInt()).changes().map { serviceId to it }
                }
                combine(filterFlows) { it.toMap() }
            }
        }
    }

    /**
     * Queues the amount specified of unread chapters from the list of selected entries.
     */
    fun performDownloadAction(action: DownloadAction) {
        downloadBulkDownloadCandidates(action, state.value.selectedLibraryItems)
        clearSelection()
    }

    private fun downloadBulkDownloadCandidates(action: DownloadAction, items: List<LibraryItem>) {
        screenModelScope.launchNonCancellable {
            val entries = getActionEntries(selectedActionEntryIds(items))
            entries.forEach { entry ->
                val result = entryDownloadActionFeature.resolveBulkDownloadCandidates(
                    EntryBulkDownloadRequest(
                        entry = entry,
                        action = action.toEntryBulkDownloadAction(),
                        sourceIds = items.downloadSourceIdsFor(entry),
                    ),
                )
                if (result is EntryBulkDownloadResolutionResult.Candidates) {
                    entryDownloadActionFeature.download(entry, result.chapters)
                }
            }
        }
    }

    fun canDownloadSelection(action: DownloadAction = DownloadAction.UNREAD_CHAPTERS): Boolean {
        val state = state.value
        val actions = state.selectionActions.takeIf { it.selection == state.selection } ?: return false
        return when (action) {
            DownloadAction.BOOKMARKED_CHAPTERS -> actions.bookmarkedDownloadsAvailable
            else -> actions.downloadsAvailable
        }
    }

    /**
     * Marks selected entries' chapters/episodes read/watch status.
     */
    fun markReadSelection(read: Boolean) {
        val entryIds = selectedActionEntryIds(state.value.selectedLibraryItems)
        screenModelScope.launchNonCancellable {
            val entries = getActionEntries(entryIds)
            entries.forEach { entry ->
                val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                if (chapters.isNotEmpty()) {
                    entryConsumptionFeature.setConsumed(entry, chapters, read)
                }
            }
        }
        clearSelection()
    }

    fun setSelectionPinned() {
        val state = state.value
        val profileId = state.libraryData.profileId ?: return
        val selectedItems = state.selectedLibraryItems
        val entryIds = selectedActionEntryIds(selectedItems)
        if (entryIds.isEmpty()) return
        val libraryPinned = selectedItems.any { !it.isPinned }
        clearSelection()
        screenModelScope.launchNonCancellable {
            setLibraryPinned.await(profileId, entryIds, libraryPinned)
        }
    }

    fun canSetConsumedSelection(): Boolean {
        return state.value.selectedEntryTypes.any(entryConsumptionFeature::isApplicable)
    }

    /**
     * Remove the selected entries.
     */
    fun removeEntries(
        entries: List<Entry>,
        deleteFromLibrary: Boolean,
        deleteChapters: Boolean,
    ) {
        screenModelScope.launchNonCancellable {
            val distinctEntries = entries.distinctBy { it.id }

            if (deleteFromLibrary) {
                when (val result = entryLibraryMembershipFeature.remove(distinctEntries)) {
                    is EntryLibraryRemovalResult.Failed -> throw result.cause
                    is EntryLibraryRemovalResult.Removed,
                    EntryLibraryRemovalResult.NoChange,
                    -> Unit
                }
            }

            if (deleteChapters) {
                distinctEntries.forEach { entry ->
                    val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                    if (chapters.isNotEmpty()) {
                        entryDownloadActionFeature.delete(entry, chapters)
                    }
                }
            }
        }
    }

    /**
     * Bulk update categories of selected items using old and new common categories.
     */
    fun setEntryCategories(items: List<LibraryItem>, addCategories: List<Long>, removeCategories: List<Long>) {
        screenModelScope.launchNonCancellable {
            updateLibraryItemCategories(
                items = items,
                addCategories = addCategories,
                removeCategories = removeCategories,
                getCategoryIds = { entryId -> getCategories.await(entryId).map(Category::id) },
                setCategoryIds = setEntryCategories::await,
            )
        }
    }

    fun getDisplayMode(): PreferenceMutableState<LibraryDisplayMode> {
        return displayModeState
    }

    fun getColumnsForOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return if (isLandscape) landscapeColumnsState else portraitColumnsState
    }

    fun getRandomLibraryItemForCurrentPage(): LibraryItem? {
        val state = state.value
        return state.getItemsForPageId(state.activePage?.id).randomOrNull()
    }

    fun getSourceDisplayName(sourceId: Long): String {
        return sourceManager.getDisplayInfo(sourceId).getDisplayNameForEntryInfo()
    }

    fun showSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.SettingsSheet) }
    }

    private var lastSelectionPageId: String? = null

    fun clearSelection() {
        lastSelectionPageId = null
        mutableState.update { it.copy(selection = setOf()) }
    }

    fun toggleSelection(page: LibraryPage, item: LibraryItem) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { set ->
                if (!set.remove(item.key)) set.add(item.key)
            }
            lastSelectionPageId = page.id.takeIf { newSelection.isNotEmpty() }
            state.copy(selection = newSelection)
        }
    }

    /**
     * Selects all entries between and including the given item and the last pressed item from the
     * same group as the given item
     */
    fun toggleRangeSelection(page: LibraryPage, item: LibraryItem) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                val lastSelected = list.lastOrNull()
                if (lastSelectionPageId != page.id) {
                    list.add(item.key)
                    return@mutate
                }

                val items = state.getItemsForPageId(page.id).fastMap { it.key }
                val lastItemIndex = items.indexOf(lastSelected)
                val currentItemIndex = items.indexOf(item.key)

                val selectionRange = when {
                    lastItemIndex < currentItemIndex -> lastItemIndex..currentItemIndex
                    currentItemIndex < lastItemIndex -> currentItemIndex..lastItemIndex
                    // We shouldn't reach this point
                    else -> return@mutate
                }
                selectionRange.mapNotNull { items[it] }.let(list::addAll)
            }
            lastSelectionPageId = page.id
            state.copy(selection = newSelection)
        }
    }

    fun selectAll() {
        lastSelectionPageId = null
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                state.getItemsForPageId(state.activePage?.id).map { it.key }.let(list::addAll)
            }
            state.copy(selection = newSelection)
        }
    }

    fun invertSelection() {
        lastSelectionPageId = null
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                val itemIds = state.getItemsForPageId(state.activePage?.id).fastMap { it.key }
                val (toRemove, toAdd) = itemIds.partition { it in list }
                list.removeAll(toRemove)
                list.addAll(toAdd)
            }
            state.copy(selection = newSelection)
        }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun updateActivePageIndex(profileId: Long, index: Int) {
        val newState = mutableState.updateAndGet { state ->
            if (state.libraryData.profileId != profileId) {
                state
            } else if (state.requestedActivePageIndex == index) {
                state
            } else {
                state.copy(activePageIndex = index)
            }
        }
        if (newState.libraryData.profileId != profileId) return

        pagePersistenceRequests.trySend(
            PagePersistenceRequest(
                profileId = profileId,
                pageIndex = newState.coercedActivePageIndex,
                pageId = newState.activePage?.id,
            ),
        )
    }

    fun openChangeCategoryDialog() {
        val state = state.value
        val items = state.selection.mapNotNull { state.libraryData.favoritesById[it] }
        // Hide the default category because it has a different behavior than the ones from db.
        val categories = state.libraryData.categories.filter { it.id != 0L }
        screenModelScope.launchIO {
            val selection = prepareLibraryCategorySelection(items) { item ->
                categoriesForLibraryItem(item, getCategories::await)
            }
            val preselected = categories
                .map {
                    when (it) {
                        in selection.common -> CheckboxState.State.Checked(it)
                        in selection.mixed -> CheckboxState.TriState.Exclude(it)
                        else -> CheckboxState.State.None(it)
                    }
                }

            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(items = items, preselected)) }
        }
    }

    fun openDeleteEntriesDialog() {
        val selectedItems = state.value.selectedLibraryItems
        val entryIds = selectedActionEntryIds(selectedItems)
        val containsMergedEntries = selectedItems.any(LibraryItem::isMerged)
        screenModelScope.launchIO {
            val entries = getActionEntries(entryIds)
            val containsLocalEntries = entries.any { it.source == LocalSource.ID }
            mutableState.update {
                it.copy(
                    dialog = Dialog.DeleteEntries(
                        entries = entries,
                        containsLocalEntries = containsLocalEntries,
                        containsMergedEntries = containsMergedEntries,
                    ),
                )
            }
        }
    }

    fun openMoveProfileDialog() {
        screenModelScope.launchIO {
            val sourceProfileId = profileStore.currentProfileId
            val profiles = availableMoveProfiles(
                profiles = profileManager.visibleProfiles.value,
                sourceProfileId = sourceProfileId,
                requiresUnlock = profileManager::profileRequiresUnlock,
            )
            if (profiles.isNotEmpty()) {
                mutableState.update { it.copy(dialog = Dialog.MoveProfile(profiles)) }
            }
        }
    }

    fun openMoveCategoryDialog(profile: Profile) {
        screenModelScope.launchIO {
            val categories = profileDatabase.getAllCategories(profile.id).filter { it.id != Category.UNCATEGORIZED_ID }
            mutableState.update { it.copy(dialog = Dialog.MoveCategory(profile, categories)) }
        }
    }

    fun prepareMoveToProfile(profile: Profile, destinationCategoryId: Long?) {
        if (moveInProgress) return
        val sourceProfileId = profileStore.currentProfileId
        val selectedIds = state.value.selectedLibraryItems.map { it.entry.id }.distinct()
        if (selectedIds.isEmpty()) return
        moveInProgress = true
        screenModelScope.launchNonCancellable {
            try {
                require(profileStore.currentProfileId == sourceProfileId) {
                    "Active profile changed before the move"
                }
                val preview = entryProfileMoveFeature.preview(
                    EntryProfileMoveRequest(
                        sourceProfileId = sourceProfileId,
                        destinationProfileId = profile.id,
                        destinationCategoryId = destinationCategoryId,
                        selectedVisibleEntryIds = selectedIds,
                    ),
                )
                require(profileStore.currentProfileId == preview.request.sourceProfileId) {
                    "Active profile changed before the move"
                }
                if (preview.conflicts.isEmpty()) {
                    executeClaimedMove(preview, emptyMap())
                } else {
                    mutableState.update {
                        it.copy(dialog = Dialog.MoveConflict(profile, preview, 0, emptyMap()))
                    }
                    moveInProgress = false
                }
            } catch (e: Exception) {
                mutableState.update { it.copy(dialog = null) }
                moveEvents.send(MoveEvent.Error)
                moveInProgress = false
            }
        }
    }

    fun resolveMoveConflict(resolution: EntryProfileMoveConflictResolution) {
        val dialog = state.value.dialog as? Dialog.MoveConflict ?: return
        if (moveInProgress) return
        val conflict = dialog.preview.conflicts[dialog.conflictIndex]
        val resolutions = dialog.resolutions + (conflict.sourceEntry.id to resolution)
        val nextIndex = dialog.conflictIndex + 1
        if (nextIndex < dialog.preview.conflicts.size) {
            mutableState.update {
                it.copy(dialog = dialog.copy(conflictIndex = nextIndex, resolutions = resolutions))
            }
        } else {
            moveInProgress = true
            screenModelScope.launchNonCancellable {
                executeClaimedMove(dialog.preview, resolutions)
            }
        }
    }

    private suspend fun executeClaimedMove(
        preview: EntryProfileMovePreview,
        resolutions: Map<Long, EntryProfileMoveConflictResolution>,
    ) {
        try {
            val result = entryProfileMoveFeature.execute(preview, resolutions)
            clearSelection()
            mutableState.update { it.copy(dialog = null) }
            moveEvents.send(MoveEvent.Success(result))
        } catch (e: Exception) {
            mutableState.update { it.copy(dialog = null) }
            moveEvents.send(MoveEvent.Error)
        } finally {
            moveInProgress = false
        }
    }

    fun isMergeSelectionAvailable(): Boolean {
        val state = state.value
        return state.selectionActions.takeIf { it.selection == state.selection }?.mergeAvailable == true
    }

    fun canMigrateSelection(): Boolean {
        val state = state.value
        return state.selectionActions.takeIf { it.selection == state.selection }?.migrationSubjects != null
    }

    fun selectedMigrationSubjects(): List<EntryMigrationSubject> {
        val state = state.value
        return state.selectionActions.takeIf { it.selection == state.selection }
            ?.migrationSubjects
            .orEmpty()
    }

    fun openMergeDialog() {
        val selectedItems = state.value.selectedLibraryItems
        screenModelScope.launchIO {
            val entries = selectedItems.flatMap(LibraryItem::memberEntries).distinctBy(Entry::id)
            val editor = when (val result = entryMergeFeature.prepare(EntryMergePrepareIntent(entries))) {
                is EntryMergePreparationResult.Ready -> result.editor
                is EntryMergePreparationResult.Rejected -> return@launchIO
            }
            val target = editor.entries.firstOrNull { it.reference == editor.target } ?: return@launchIO
            val dialog = Dialog.MergeEntry(
                editReference = editor.editReference,
                entries = editor.entries.map { item ->
                    MergeEntry(
                        id = item.entry.id,
                        reference = item.reference,
                        entry = item.entry,
                        isFromExistingMerge = item.removable,
                    )
                }.toImmutableList(),
                targetId = target.entry.id,
                targetReference = target.reference,
                targetLocked = editor.targetLocked,
            )
            mutableState.update {
                it.copy(
                    dialog = dialog,
                )
            }
        }
    }

    fun reorderMergeSelection(fromIndex: Int, toIndex: Int) {
        mutableState.update { state ->
            when (val dialog = state.dialog) {
                is Dialog.MergeEntry -> {
                    if (fromIndex !in dialog.entries.indices || toIndex !in dialog.entries.indices) return@update state
                    val reordered = dialog.entries.toMutableList().apply {
                        val item = removeAt(fromIndex)
                        add(toIndex, item)
                    }
                    state.copy(dialog = dialog.copy(entries = reordered.toImmutableList()))
                }
                else -> state
            }
        }
    }

    fun setMergeTarget(id: Long) {
        mutableState.update { state ->
            when (val dialog = state.dialog) {
                is Dialog.MergeEntry -> {
                    val entry = dialog.entries.firstOrNull { it.id == id } ?: return@update state
                    if (dialog.targetLocked) return@update state
                    state.copy(dialog = dialog.copy(targetId = id, targetReference = entry.reference))
                }
                else -> state
            }
        }
    }

    fun confirmMergeSelection() {
        when (val dialog = state.value.dialog) {
            is Dialog.MergeEntry -> {
                screenModelScope.launchNonCancellable {
                    val result = entryMergeFeature.execute(
                        EntryMergeCommitIntent(
                            editReference = dialog.editReference,
                            target = dialog.targetReference,
                            orderedEntries = dialog.entries.map(MergeEntry::reference),
                        ),
                    )
                    if (result is EntryMergeExecutionResult.Applied) {
                        clearSelection()
                        closeDialog()
                    }
                }
            }
            else -> return
        }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    private suspend fun getActionEntries(entryIds: List<Long>): List<Entry> {
        return getEntry.await(entryIds)
    }

    sealed interface Dialog {
        data object SettingsSheet : Dialog
        data class ChangeCategory(
            val items: List<LibraryItem>,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class MergeEntry(
            val editReference: EntryMergeEditReference,
            val entries: ImmutableList<LibraryScreenModel.MergeEntry>,
            val targetId: Long,
            val targetReference: EntryMergeEditorEntryReference,
            val targetLocked: Boolean,
        ) : Dialog
        data class DeleteEntries(
            val entries: List<Entry>,
            val containsLocalEntries: Boolean,
            val containsMergedEntries: Boolean,
        ) : Dialog
        data class MoveProfile(val profiles: List<Profile>) : Dialog
        data class MoveCategory(val profile: Profile, val categories: List<Category>) : Dialog
        data class MoveConflict(
            val profile: Profile,
            val preview: EntryProfileMovePreview,
            val conflictIndex: Int,
            val resolutions: Map<Long, EntryProfileMoveConflictResolution>,
        ) : Dialog
    }

    sealed interface MoveEvent {
        data class Success(val result: EntryProfileMoveResult) : MoveEvent
        data object Error : MoveEvent
    }

    @Immutable
    data class MergeEntry(
        val id: Long,
        val reference: EntryMergeEditorEntryReference,
        val entry: Entry,
        val isFromExistingMerge: Boolean,
    ) {
        val title: String
            get() = entry.displayTitle

        val subtitle: String
            get() = buildString {
                val sourceName = Injekt.get<SourceManager>().getDisplayInfo(
                    entry.source,
                ).getDisplayNameForEntryInfo()
                val creator = entry.author?.takeIf { it.isNotBlank() }
                    ?: entry.artist?.takeIf { it.isNotBlank() }
                append(sourceName)
                if (creator != null && !creator.equals(sourceName, ignoreCase = true)) {
                    append(" • ")
                    append(creator)
                }
            }
    }

    private data class AppliedLibraryFilters(
        val items: List<LibraryItem>,
        val hasActiveFilters: Boolean,
        val availability: EntryLibraryFilterAvailability,
    )

    @Immutable
    data class SelectionActions(
        val selection: Set<LibraryItemKey> = emptySet(),
        val mergeAvailable: Boolean = false,
        val downloadsAvailable: Boolean = false,
        val bookmarkedDownloadsAvailable: Boolean = false,
        val migrationSubjects: List<EntryMigrationSubject>? = null,
    )

    private data class SelectionActionInput(
        val selection: Set<LibraryItemKey>,
        val items: List<LibraryItem>,
        val entries: List<Entry>,
    )

    private fun selectionActionInput(state: State): SelectionActionInput {
        val items = state.selectedLibraryItems
        return SelectionActionInput(
            selection = state.selection,
            items = items,
            entries = items.flatMap(LibraryItem::memberEntries).distinctBy(Entry::id),
        )
    }

    private suspend fun resolveSelectionActions(input: SelectionActionInput): SelectionActions {
        if (input.selection.isEmpty()) return SelectionActions()

        val requests = input.items.map { item ->
            EntryDownloadActionRequest(item.entry.type, item.sourceIds)
        }
        val migrationSubjects = when (
            val result = entryMigrationFeature.prepareSelection(input.items.map { it.entry })
        ) {
            is EntryMigrationSelectionResult.Ready -> result.subjects
            is EntryMigrationSelectionResult.Rejected -> null
        }
        return SelectionActions(
            selection = input.selection,
            mergeAvailable = entryMergeFeature.prepare(
                EntryMergePrepareIntent(input.entries),
            ) is EntryMergePreparationResult.Ready,
            downloadsAvailable = entryDownloadActionFeature.bulkAvailability(
                requests = requests,
                action = DownloadAction.UNREAD_CHAPTERS.toEntryBulkDownloadAction(),
            ) == EntryDownloadActionAvailability.Available,
            bookmarkedDownloadsAvailable = entryDownloadActionFeature.bulkAvailability(
                requests = requests,
                action = DownloadAction.BOOKMARKED_CHAPTERS.toEntryBulkDownloadAction(),
            ) == EntryDownloadActionAvailability.Available,
            migrationSubjects = migrationSubjects,
        )
    }

    @Immutable
    data class LibraryData(
        val profileId: Long? = null,
        val isInitialized: Boolean = false,
        val showSystemCategory: Boolean = false,
        val categories: List<Category> = emptyList(),
        val favorites: List<LibraryItem> = emptyList(),
        val trackingEntries: Map<Long, List<EntryTrackingCollectionTrack>> = emptyMap(),
        val trackingScoreSupportedEntryTypes: Set<EntryType> = emptySet(),
        val hasActiveFilters: Boolean = false,
        val filterAvailability: EntryLibraryFilterAvailability = EntryLibraryFilterAvailability(
            progressSummary = EntryLibraryFilterControlAvailability(emptySet(), emptySet()),
            bookmarking = EntryLibraryFilterControlAvailability(emptySet(), emptySet()),
            outsideReleasePeriod = EntryLibraryFilterControlAvailability(emptySet(), emptySet()),
        ),
    ) {
        val favoritesById by lazy { favorites.associateBy { it.key } }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val searchQuery: String? = null,
        val selection: Set<LibraryItemKey> = setOf(),
        internal val selectionActions: SelectionActions = SelectionActions(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showEntryCount: Boolean = false,
        val showContinueButton: Boolean = false,
        val displaySettings: LibraryDisplaySettings = LibraryDisplaySettings(),
        val dialog: Dialog? = null,
        val libraryData: LibraryData = LibraryData(),
        val grouping: LibraryGrouping = LibraryGrouping.default,
        private val activePageIndex: Int = 0,
        private val groupedFavorites: List<LibraryPage> = emptyList(),
        private val pageItemsById: Map<String, List<LibraryItem>> = emptyMap(),
    ) {
        val displayedPages: List<LibraryPage> = groupedFavorites

        val coercedActivePageIndex = activePageIndex.coerceIn(
            minimumValue = 0,
            maximumValue = displayedPages.lastIndex.coerceAtLeast(0),
        )

        val requestedActivePageIndex = activePageIndex

        val activePage: LibraryPage? = displayedPages.getOrNull(coercedActivePageIndex)

        val activeSortCategory: Category? = activePage?.category

        val isLibraryEmpty = libraryData.favorites.isEmpty()

        val selectionMode = selection.isNotEmpty()

        val selectedLibraryItems by lazy {
            selection
                .mapNotNull { libraryData.favoritesById[it] }
        }

        val selectedEntryTypes by lazy {
            selectedLibraryItems.map { it.entry.type }.toSet()
        }

        val selectionPinTarget: Boolean
            get() = selectedLibraryItems.any { !it.isPinned }

        fun getItemsForPageId(pageId: String?): List<LibraryItem> {
            if (pageId == null) return emptyList()
            return pageItemsById[pageId].orEmpty()
        }

        fun getItemsForPage(page: LibraryPage): List<LibraryItem> {
            return pageItemsById[page.id].orEmpty()
        }

        fun getItemCountForPage(page: LibraryPage): Int? {
            return if (showEntryCount || !searchQuery.isNullOrEmpty()) page.itemIds.size else null
        }

        fun getToolbarTitle(
            defaultTitle: String,
            defaultCategoryTitle: String,
            page: Int,
        ): LibraryToolbarTitle {
            val currentPage = displayedPages.getOrNull(page) ?: return LibraryToolbarTitle(defaultTitle)
            val title = if (showCategoryTabs) defaultTitle else currentPage.displayTitle(defaultCategoryTitle)
            val count = when {
                !showEntryCount -> null
                !showCategoryTabs -> getItemCountForPage(currentPage)
                // Whole library count
                else -> libraryData.favorites.size
            }
            return LibraryToolbarTitle(title, count)
        }
    }
}

internal fun observeGroupedLibraryPages(
    libraryData: Flow<LibraryScreenModel.LibraryData>,
    grouping: Flow<LibraryGrouping>,
    sortingMode: Flow<LibrarySort>,
    randomSortSeed: Flow<Int>,
    applyGrouping: (LibraryScreenModel.LibraryData, LibraryGrouping) -> List<LibraryPage>,
    applySort: (
        pages: List<LibraryPage>,
        data: LibraryScreenModel.LibraryData,
        sortingMode: LibrarySort,
        randomSortSeed: Int,
    ) -> List<LibraryPage>,
): Flow<GroupedLibraryPages> {
    return combine(
        libraryData,
        grouping,
        sortingMode,
        randomSortSeed,
    ) { data, grouping, sortingMode, randomSortSeed ->
        val pages = applySort(applyGrouping(data, grouping), data, sortingMode, randomSortSeed)
            .withTabItemCounts()
        GroupedLibraryPages(
            profileId = checkNotNull(data.profileId),
            grouping = grouping,
            pages = pages,
            itemsByPageId = pages.associate { page ->
                page.id to page.itemIds.mapNotNull(data.favoritesById::get)
            },
        )
    }
}

internal data class GroupedLibraryPages(
    val profileId: Long,
    val grouping: LibraryGrouping,
    val pages: List<LibraryPage>,
    val itemsByPageId: Map<String, List<LibraryItem>>,
)

private data class PagePersistenceRequest(
    val profileId: Long,
    val pageIndex: Int,
    val pageId: String?,
)

private fun List<LibraryPage>.indexOfMatchingPage(
    previousPage: LibraryPage?,
    restoredPageId: String,
    fallbackPageIndex: Int,
    previousGrouping: LibraryGrouping,
    newGrouping: LibraryGrouping,
): Int {
    if (previousPage == null) {
        if (isEmpty()) return fallbackPageIndex
        return indexOfFirst { it.id == restoredPageId }
            .takeIf { it >= 0 }
            ?: fallbackPageIndex.coerceIn(0, lastIndex.coerceAtLeast(0))
    }
    val sharedDimensions = previousGrouping.dimensions.intersect(newGrouping.dimensions.toSet())
    return indexOfFirst { candidate ->
        sharedDimensions.all { dimension ->
            when (dimension) {
                LibraryGroupingDimension.Category ->
                    previousPage.category?.id?.let { candidate.category?.id == it } ?: true
                LibraryGroupingDimension.EntryType ->
                    previousPage.entryType?.let { candidate.entryType == it } ?: true
                LibraryGroupingDimension.Source ->
                    previousPage.sourceId?.let { candidate.sourceId == it } ?: true
            }
        }
    }.takeIf { it >= 0 } ?: 0
}

internal fun availableMoveProfiles(
    profiles: List<Profile>,
    sourceProfileId: Long,
    requiresUnlock: (Long) -> Boolean,
): List<Profile> {
    return profiles
        .filter { it.id != sourceProfileId && !it.isArchived }
        .map { it.copy(requiresAuth = requiresUnlock(it.id)) }
}

internal fun selectedActionEntryIds(selection: List<LibraryItem>): List<Long> {
    return selection
        .flatMap(LibraryItem::memberEntryIds)
        .map(LibraryItemKey::id)
        .distinct()
}

internal fun List<LibraryItem>.downloadSourceIdsFor(entry: Entry): Set<Long> {
    return asSequence()
        .filter { item -> item.memberEntryIds.any { it.id == entry.id } }
        .flatMap { it.sourceIds }
        .toSet()
        .ifEmpty { setOf(entry.source) }
}

internal suspend fun updateLibraryItemCategories(
    items: List<LibraryItem>,
    addCategories: List<Long>,
    removeCategories: List<Long>,
    getCategoryIds: suspend (Long) -> List<Long>,
    setCategoryIds: suspend (Long, List<Long>) -> Unit,
) {
    val removed = removeCategories.toSet()
    selectedActionEntryIds(items).forEach { entryId ->
        val categoryIds = getCategoryIds(entryId)
            .subtract(removed)
            .plus(addCategories)
            .toList()
        setCategoryIds(entryId, categoryIds)
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

private fun LibraryItem.getSourceDisplayName(sourceManager: SourceManager): String {
    return when {
        sourceName.isNotBlank() -> sourceName
        displaySourceId == LibraryItem.MULTI_SOURCE_ID -> ""
        else -> sourceManager.getDisplayInfo(displaySourceId).name
    }
}
