package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.library.components.LibraryDisplaySettings
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.supportedEntryTypes
import eu.kanade.tachiyomi.entry.EntryRemovalCleanupInteraction
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import eu.kanade.tachiyomi.source.sourceItemOrientation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import mihon.core.common.utils.mutate
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.EntryCapabilityInteraction
import mihon.entry.interactions.EntryConsumptionInteraction
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryLibraryFilterInteraction
import mihon.entry.interactions.EntryMergeCapabilityItem
import mihon.feature.profiles.core.EntryProfileMoveConflictResolution
import mihon.feature.profiles.core.EntryProfileMovePreview
import mihon.feature.profiles.core.EntryProfileMoveRequest
import mihon.feature.profiles.core.EntryProfileMoveResult
import mihon.feature.profiles.core.EntryProfileMoveService
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryFavorite
import tachiyomi.domain.entry.interactor.UpdateMergedEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibrarySortKey
import tachiyomi.domain.library.service.librarySortComparator
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracksPerEntry
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.util.applyFilter
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
    private val getTracksPerEntry: GetTracksPerEntry = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getMergedEntry: GetMergedEntry = Injekt.get(),
    private val updateMergedEntry: UpdateMergedEntry = Injekt.get(),
    private val setEntryFavorite: SetEntryFavorite = Injekt.get(),
    private val setEntryCategories: SetEntryCategories = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val entryDownloadInteraction: EntryDownloadInteraction = Injekt.get(),
    private val entryCapabilityInteraction: EntryCapabilityInteraction = Injekt.get(),
    private val entryConsumptionInteraction: EntryConsumptionInteraction = Injekt.get(),
    private val entryLibraryFilterInteraction: EntryLibraryFilterInteraction = Injekt.get(),
    private val entryRemovalCleanupInteraction: EntryRemovalCleanupInteraction = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val profileStore: ProfileAwareStore = Injekt.get(),
    private val profileDatabase: ProfileDatabase = Injekt.get(),
    private val profileManager: ProfileManager = Injekt.get(),
    private val entryProfileMoveService: EntryProfileMoveService = Injekt.get(),
) : StateScreenModel<LibraryScreenModel.State>(State()) {

    val moveEvents = Channel<MoveEvent>(Channel.BUFFERED)
    private var moveInProgress = false

    init {
        mutableState.update { state ->
            state.copy(activePageIndex = libraryPreferences.lastUsedCategory.get())
        }
        profileStore.currentProfileIdFlow
            .drop(1)
            .onEach {
                mutableState.update { state ->
                    state.copy(
                        activePageIndex = libraryPreferences.lastUsedCategory.get(),
                        selection = emptySet(),
                        dialog = null,
                    )
                }
                lastSelectionPageId = null
            }
            .launchIn(screenModelScope)
        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }.distinctUntilChanged().debounce(0.25.seconds),
                getCategories.subscribe(),
                getLibraryItemsFlow(),
                combine(getTracksPerEntry.subscribe(), getTrackingFiltersFlow(), ::Pair),
                getLibraryItemPreferencesFlow(),
            ) { searchQuery, categories, favorites, (tracksMap, trackingFilters), itemPreferences ->
                val showSystemCategory = favorites.any { it.categories.contains(0L) }
                val categoryNamesById = categories.associate { it.id to it.name }
                val filteredFavorites = favorites
                    .applyFilters(tracksMap, trackingFilters, itemPreferences)
                    .let {
                        if (searchQuery ==
                            null
                        ) {
                            it
                        } else {
                            it.filter { item -> item.matches(searchQuery, sourceManager, categoryNamesById) }
                        }
                    }

                LibraryData(
                    isInitialized = true,
                    showSystemCategory = showSystemCategory,
                    categories = categories,
                    favorites = filteredFavorites,
                    tracksMap = tracksMap,
                    loggedInTrackerIds = trackingFilters.keys,
                )
            }
                .distinctUntilChanged()
                .collectLatest { libraryData ->
                    mutableState.update { state ->
                        state.copy(libraryData = libraryData)
                    }
                }
        }

        screenModelScope.launchIO {
            observeGroupedLibraryPages(
                libraryData = state
                    .dropWhile { !it.libraryData.isInitialized }
                    .map { it.libraryData }
                    .distinctUntilChanged(),
                groupType = libraryPreferences.groupType.changes(),
                sortingMode = libraryPreferences.sortingMode.changes(),
                randomSortSeed = libraryPreferences.randomSortSeed.changes(),
                applyGrouping = { data, groupType ->
                    data.favorites.applyGrouping(data.categories, data.showSystemCategory, groupType)
                },
                applySort = { pages, data, groupType, sortingMode, randomSortSeed ->
                    pages.applySort(
                        favoritesById = data.favoritesById,
                        trackMap = data.tracksMap,
                        loggedInTrackerIds = data.loggedInTrackerIds,
                        groupType = groupType,
                        globalSort = sortingMode,
                        randomSortSeed = randomSortSeed,
                    )
                },
            ).collectLatest { (groupType, groupedFavorites) ->
                mutableState.update { state ->
                    state.copy(
                        isLoading = false,
                        groupedFavorites = groupedFavorites,
                        groupType = groupType,
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

        getLibraryItemPreferencesFlow()
            .map(ItemPreferences::toDisplaySettings)
            .distinctUntilChanged()
            .onEach { displaySettings ->
                mutableState.update { state ->
                    state.copy(displaySettings = displaySettings)
                }
            }
            .launchIn(screenModelScope)

        combine(
            getLibraryItemPreferencesFlow(),
            getTrackingFiltersFlow(),
        ) { prefs, trackFilters ->
            listOf(
                prefs.globalFilterDownloaded,
                prefs.filterDownloaded,
                prefs.filterUnread,
                prefs.filterNotStarted,
                prefs.filterBookmarked,
                prefs.filterCompleted,
                prefs.filterIntervalCustom,
                *trackFilters.values.toTypedArray(),
            )
                .any { it != TriState.DISABLED }
        }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun List<LibraryItem>.applyFilters(
        trackMap: Map<Long, List<EntryTrack>>,
        trackingFilter: Map<Long, TriState>,
        preferences: ItemPreferences,
    ): List<LibraryItem> {
        val downloadedOnly = preferences.globalFilterDownloaded
        val skipOutsideReleasePeriod = preferences.skipOutsideReleasePeriod
        val filterDownloaded = if (downloadedOnly) TriState.ENABLED_IS else preferences.filterDownloaded
        val filterUnread = preferences.filterUnread
        val filterNotStarted = preferences.filterNotStarted
        val filterBookmarked = preferences.filterBookmarked
        val filterCompleted = preferences.filterCompleted
        val filterIntervalCustom = preferences.filterIntervalCustom

        val isNotLoggedInAnyTrack = trackingFilter.isEmpty()

        val excludedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_NOT) it.key else null }
        val includedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_IS) it.key else null }
        val trackFiltersIsIgnored = includedTracks.isEmpty() && excludedTracks.isEmpty()

        val filterFnDownloaded: (LibraryItem) -> Boolean = {
            applyFilter(filterDownloaded) { it.isLocal || it.downloadCount > 0 }
        }

        val filterFnUnread: (LibraryItem) -> Boolean = { item ->
            applyFilter(filterUnread) { item.unconsumedCount > 0 }
        }

        val filterFnNotStarted: (LibraryItem) -> Boolean = {
            applyFilter(filterNotStarted) { !it.hasStarted }
        }

        val filterFnBookmarked: (LibraryItem) -> Boolean = { item ->
            applyFilter(filterBookmarked) { item.hasBookmarks }
        }

        val filterFnCompleted: (LibraryItem) -> Boolean = { item ->
            applyFilter(filterCompleted) { item.entry.status == EntryStatus.COMPLETED }
        }

        val filterFnIntervalCustom: (LibraryItem) -> Boolean = { item ->
            if (skipOutsideReleasePeriod &&
                entryLibraryFilterInteraction.supportsOutsideReleasePeriodFilter(item.entry)
            ) {
                applyFilter(filterIntervalCustom) { item.entry.fetchInterval < 0 }
            } else {
                true
            }
        }

        val filterFnTracking: (LibraryItem) -> Boolean = tracking@{ item ->
            if (isNotLoggedInAnyTrack || trackFiltersIsIgnored) return@tracking true

            val entryTracks = trackMap[item.key.id].orEmpty().map { it.trackerId }

            val isExcluded = excludedTracks.isNotEmpty() && entryTracks.fastAny { it in excludedTracks }
            val isIncluded = includedTracks.isEmpty() || entryTracks.fastAny { it in includedTracks }

            !isExcluded && isIncluded
        }

        return fastFilter {
            filterFnDownloaded(it) &&
                filterFnUnread(it) &&
                filterFnNotStarted(it) &&
                filterFnBookmarked(it) &&
                filterFnCompleted(it) &&
                filterFnIntervalCustom(it) &&
                filterFnTracking(it)
        }
    }

    private fun List<LibraryItem>.applyGrouping(
        categories: List<Category>,
        showSystemCategory: Boolean,
        groupType: LibraryGroupType,
    ): List<LibraryPage> {
        val visibleCategories = categories.filter { showSystemCategory || !it.isSystemCategory }
        val categoryTabs = visibleCategories.associate { category ->
            category.id to LibraryPageTab(
                id = "category:${category.id}",
                title = category.name,
                category = category,
            )
        }

        val sourceNames = map { it.displaySourceId to it.sourceName }.toMap()
        val sourceIds = sourceNames.keys.sortedWith { sourceId1, sourceId2 ->
            sourceNames.getValue(sourceId1)
                .compareToWithCollator(sourceNames.getValue(sourceId2))
                .takeIf { it != 0 }
                ?: sourceId1.compareTo(sourceId2)
        }
        val sourceTabs = sourceIds.associateWith { sourceId ->
            LibraryPageTab(
                id = "source:$sourceId",
                title = sourceNames.getValue(sourceId),
            )
        }
        val entryTypes = EntryType.entries.filter { entryType ->
            any { it.entry.type == entryType }
        }
        val typeTabs = entryTypes.associateWith { entryType ->
            LibraryPageTab(
                id = "type:${entryType.name}",
                title = context.stringResource(entryType.entryTypePresentation().displayNameLabel),
            )
        }
        if (
            groupType == LibraryGroupType.Type ||
            groupType == LibraryGroupType.TypeCategory ||
            groupType == LibraryGroupType.CategoryType
        ) {
            return buildTypeLibraryPages(
                items = this,
                visibleCategories = visibleCategories,
                groupType = groupType,
                categoryTabs = categoryTabs,
                entryTypes = entryTypes,
                typeTabs = typeTabs,
            )
        }

        return when (groupType) {
            LibraryGroupType.Category -> {
                visibleCategories.map { category ->
                    LibraryPage(
                        id = "category:${category.id}",
                        primaryTab = categoryTabs.getValue(category.id),
                        category = category,
                        itemIds = fastFilter { category.id in it.categories }
                            .fastMap(LibraryItem::key),
                    )
                }
            }
            LibraryGroupType.Extension -> {
                sourceIds.map { sourceId ->
                    LibraryPage(
                        id = "source:$sourceId",
                        primaryTab = sourceTabs.getValue(sourceId),
                        sourceId = sourceId,
                        itemIds = fastFilter { it.displaySourceId == sourceId }
                            .fastMap(LibraryItem::key),
                    )
                }
            }
            LibraryGroupType.Type,
            LibraryGroupType.TypeCategory,
            LibraryGroupType.CategoryType,
            -> error("Type grouping is handled before source grouping")
            LibraryGroupType.ExtensionCategory -> {
                buildList {
                    sourceIds.forEach { sourceId ->
                        val sourceItems = this@applyGrouping.fastFilter { it.displaySourceId == sourceId }
                        visibleCategories.forEach { category ->
                            val itemIds = sourceItems.fastFilter { category.id in it.categories }
                                .fastMap(LibraryItem::key)
                            if (itemIds.isNotEmpty()) {
                                add(
                                    LibraryPage(
                                        id = "source:$sourceId:category:${category.id}",
                                        primaryTab = sourceTabs.getValue(sourceId),
                                        secondaryTab = categoryTabs.getValue(category.id),
                                        category = category,
                                        sourceId = sourceId,
                                        itemIds = itemIds,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            LibraryGroupType.CategoryExtension -> {
                buildList {
                    visibleCategories.forEach { category ->
                        val categoryItems = this@applyGrouping.fastFilter { category.id in it.categories }
                        if (categoryItems.isEmpty()) {
                            add(
                                LibraryPage(
                                    id = "category:${category.id}",
                                    primaryTab = categoryTabs.getValue(category.id),
                                    category = category,
                                ),
                            )
                        } else {
                            val categorySourceIds = categoryItems.map { it.displaySourceId }
                                .distinct()
                                .sortedWith { sourceId1, sourceId2 ->
                                    sourceNames.getValue(sourceId1)
                                        .compareToWithCollator(sourceNames.getValue(sourceId2))
                                        .takeIf { it != 0 }
                                        ?: sourceId1.compareTo(sourceId2)
                                }
                            categorySourceIds.forEach { sourceId ->
                                add(
                                    LibraryPage(
                                        id = "category:${category.id}:source:$sourceId",
                                        primaryTab = categoryTabs.getValue(category.id),
                                        secondaryTab = sourceTabs.getValue(sourceId),
                                        category = category,
                                        sourceId = sourceId,
                                        itemIds = categoryItems.fastFilter {
                                            it.displaySourceId == sourceId
                                        }
                                            .fastMap(LibraryItem::key),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun List<LibraryPage>.applySort(
        favoritesById: Map<LibraryItemKey, LibraryItem>,
        trackMap: Map<Long, List<EntryTrack>>,
        loggedInTrackerIds: Set<Long>,
        groupType: LibraryGroupType,
        globalSort: LibrarySort,
        randomSortSeed: Int,
    ): List<LibraryPage> {
        val defaultTrackerScoreSortValue = -1.0
        val trackerScores by lazy {
            val trackerMap = trackerManager.getAll(loggedInTrackerIds).associateBy { e -> e.id }
            trackMap.mapValues { entry ->
                when {
                    entry.value.isEmpty() -> null
                    else ->
                        entry.value
                            .mapNotNull { trackerMap[it.trackerId]?.get10PointScore(it) }
                            .average()
                }
            }
        }
        val trackerSupportedEntryTypes by lazy {
            trackerManager.getAll(loggedInTrackerIds).supportedEntryTypes()
        }

        return map { page ->
            val sort = if (groupType == LibraryGroupType.Category) {
                page.category.effectiveLibrarySort(globalSort)
            } else {
                globalSort
            }
            if (sort.type == LibrarySort.Type.Random) {
                return@map page.copy(
                    itemIds = page.itemIds.shuffled(Random(randomSortSeed)),
                )
            }

            val items = page.itemIds.mapNotNull { favoritesById[it] }

            val comparator = Comparator<LibraryItem> { a, b ->
                librarySortComparator(
                    sort = sort,
                    trackerScores = trackerScores,
                    defaultTrackerScore = defaultTrackerScoreSortValue,
                ).compare(
                    a.toLibrarySortKey(trackerSupportedEntryTypes, defaultTrackerScoreSortValue),
                    b.toLibrarySortKey(trackerSupportedEntryTypes, defaultTrackerScoreSortValue),
                )
            }

            page.copy(itemIds = items.sortedWith(comparator).map { it.key })
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

    private fun getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.downloadBadge.changes(),
            libraryPreferences.unreadBadge.changes(),
            libraryPreferences.localBadge.changes(),
            libraryPreferences.languageBadge.changes(),
            libraryPreferences.entryTypeBadge.changes(),
            libraryPreferences.autoUpdateEntryRestrictions.changes(),

            libraryPreferences.downloadedOnly.changes(),
            libraryPreferences.filterDownloaded.changes(),
            libraryPreferences.filterUnread.changes(),
            libraryPreferences.filterNotStarted.changes(),
            libraryPreferences.filterBookmarked.changes(),
            libraryPreferences.filterCompleted.changes(),
            libraryPreferences.filterIntervalCustom.changes(),
        ) {
            ItemPreferences(
                downloadBadge = it[0] as Boolean,
                unreadBadge = it[1] as Boolean,
                localBadge = it[2] as Boolean,
                languageBadge = it[3] as Boolean,
                entryTypeBadge = it[4] as Boolean,
                skipOutsideReleasePeriod = LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in (it[5] as Set<*>),
                globalFilterDownloaded = it[6] as Boolean,
                filterDownloaded = it[7] as TriState,
                filterUnread = it[8] as TriState,
                filterNotStarted = it[9] as TriState,
                filterBookmarked = it[10] as TriState,
                filterCompleted = it[11] as TriState,
                filterIntervalCustom = it[12] as TriState,
            )
        }
    }

    private fun getLibraryItemsFlow(): Flow<List<LibraryItem>> {
        return combine(
            getLibraryEntries.subscribe(),
            entryDownloadInteraction.changes,
        ) { items, _ ->
            items.enrichEntryItems()
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
            val downloadCount = item.calculateDownloadCount(entryDownloadInteraction)

            item.copy(
                sourceName = sourceName,
                sourceLanguage = sourceLanguage,
                sourceItemOrientation = displayUnifiedSource?.sourceItemOrientation() ?: EntryItemOrientation.VERTICAL,
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
        return trackerManager.loggedInTrackersFlow().flatMapLatest { loggedInTrackers ->
            if (loggedInTrackers.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val filterFlows = loggedInTrackers.map { tracker ->
                    libraryPreferences.filterTracking(tracker.id.toInt()).changes().map { tracker.id to it }
                }
                combine(filterFlows) { it.toMap() }
            }
        }
    }

    /**
     * Returns the common categories for the given library items.
     */
    private suspend fun getCommonCategories(items: List<LibraryItem>): Collection<Category> {
        if (items.isEmpty()) return emptyList()
        return items
            .map { getCategoriesForItem(it).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    /**
     * Returns the mix (non-common) categories for the given library items.
     */
    private suspend fun getMixCategories(items: List<LibraryItem>): Collection<Category> {
        if (items.isEmpty()) return emptyList()
        val itemCategories = items.map { getCategoriesForItem(it).toSet() }
        val common = itemCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return itemCategories.flatten().distinct().subtract(common)
    }

    private suspend fun getCategoriesForItem(item: LibraryItem): List<Category> {
        return getCategories.await(item.entry.id)
    }

    /**
     * Queues the amount specified of unread chapters from the list of selected entries.
     */
    fun performDownloadAction(action: DownloadAction) {
        downloadBulkDownloadCandidates(action)
        clearSelection()
    }

    private fun downloadBulkDownloadCandidates(action: DownloadAction) {
        screenModelScope.launchNonCancellable {
            val entries = getSelectedActionEntries()
            entries.forEach { entry ->
                when (
                    val result = entryDownloadInteraction.resolveBulkDownloadCandidates(
                        entry = entry,
                        action = action.toEntryBulkDownloadAction(),
                    )
                ) {
                    is EntryBulkDownloadCandidateResult.Supported -> {
                        if (result.chapters.isNotEmpty()) {
                            entryDownloadInteraction.download(entry, result.chapters)
                        }
                    }
                    EntryBulkDownloadCandidateResult.Unsupported -> Unit
                }
            }
        }
    }

    fun canDownloadSelection(): Boolean {
        val selectedItems = state.value.selectedLibraryItems
        if (selectedItems.isEmpty()) return false
        return selectedItems.all { item ->
            !item.isLocal && entryCapabilityInteraction.supportsBulkDownload(item.entry)
        }
    }

    /**
     * Marks selected entries' chapters/episodes read/watch status.
     */
    fun markReadSelection(read: Boolean) {
        screenModelScope.launchNonCancellable {
            val entries = getSelectedActionEntries()
            entries.forEach { entry ->
                val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                if (chapters.isNotEmpty()) {
                    entryConsumptionInteraction.setConsumed(entry, chapters, read)
                }
            }
        }
        clearSelection()
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
                val removedMergesByTargetId = distinctEntries.mapNotNull { entry ->
                    getMergedEntry.awaitTargetId(entry.id)?.let { targetId -> targetId to entry.id }
                }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                removedMergesByTargetId.forEach { (targetId, entryIds) ->
                    updateMergedEntry.awaitRemoveMembers(targetId, entryIds)
                }

                distinctEntries.forEach { entry ->
                    entryRemovalCleanupInteraction.cleanupAfterLibraryRemoval(entry)
                    setEntryFavorite.await(entry.id, false)
                }
            }

            if (deleteChapters) {
                distinctEntries.forEach { entry ->
                    val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                    if (chapters.isNotEmpty()) {
                        entryDownloadInteraction.delete(entry, chapters)
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
            items.forEach { item ->
                val categoryIds = getCategories.await(item.entry.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                setEntryCategories.await(item.entry.id, categoryIds)
            }
        }
    }

    fun getDisplayMode(): PreferenceMutableState<LibraryDisplayMode> {
        return libraryPreferences.displayMode.asState(screenModelScope)
    }

    fun getColumnsForOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (if (isLandscape) libraryPreferences.landscapeColumns else libraryPreferences.portraitColumns)
            .asState(screenModelScope)
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

    fun updateActivePageIndex(index: Int) {
        val newIndex = mutableState.updateAndGet { state ->
            state.copy(activePageIndex = index)
        }
            .coercedActivePageIndex

        libraryPreferences.lastUsedCategory.set(newIndex)
    }

    fun openChangeCategoryDialog() {
        screenModelScope.launchIO {
            val items = state.value.selection.mapNotNull { state.value.libraryData.favoritesById[it] }

            // Hide the default category because it has a different behavior than the ones from db.
            val categories = state.value.libraryData.categories.filter { it.id != 0L }

            // Get indexes of the common categories to preselect.
            val common = getCommonCategories(items)
            // Get indexes of the mix categories to preselect.
            val mix = getMixCategories(items)
            val preselected = categories
                .map {
                    when (it) {
                        in common -> CheckboxState.State.Checked(it)
                        in mix -> CheckboxState.TriState.Exclude(it)
                        else -> CheckboxState.State.None(it)
                    }
                }

            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(items = items, preselected)) }
        }
    }

    fun openDeleteEntriesDialog() {
        screenModelScope.launchIO {
            val entries = getSelectedActionEntries()
            val containsLocalEntries = entries.any { it.source == LocalSource.ID }
            val containsMergedEntries = state.value.selectedLibraryItems.any { it.isMerged }
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
        moveInProgress = true
        screenModelScope.launchNonCancellable {
            try {
                val selectedIds = state.value.selectedLibraryItems.map { it.entry.id }
                val preview = entryProfileMoveService.preview(
                    EntryProfileMoveRequest(
                        sourceProfileId = profileStore.currentProfileId,
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
            val result = entryProfileMoveService.execute(preview, resolutions)
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

    fun canMergeSelection(): Boolean {
        val items = state.value.selection.mapNotNull { state.value.libraryData.favoritesById[it] }
        return entryCapabilityInteraction.canMergeSelection(items.toEntryMergeCapabilityItems())
    }

    fun canMigrateSelection(): Boolean {
        val items = state.value.selectedLibraryItems
        return entryCapabilityInteraction.canMigrate(items.map { it.entry })
    }

    fun selectedMigrationEntryIds(): List<Long> {
        return entryCapabilityInteraction.migrationEntries(state.value.selectedLibraryItems.map { it.entry })
            .map { it.id }
    }

    fun openMergeDialog() {
        screenModelScope.launchIO {
            val selectedItems = state.value.selectedLibraryItems
            if (!entryCapabilityInteraction.canMergeSelection(selectedItems.toEntryMergeCapabilityItems())) {
                return@launchIO
            }

            val dialog = buildMergeDialog(selectedItems) ?: return@launchIO
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
                    if (dialog.targetLocked || dialog.entries.none { it.id == id }) return@update state
                    state.copy(dialog = dialog.copy(targetId = id))
                }
                else -> state
            }
        }
    }

    fun confirmMergeSelection() {
        when (val dialog = state.value.dialog) {
            is Dialog.MergeEntry -> {
                screenModelScope.launchNonCancellable {
                    val targetId = dialog.targetId.takeIf { targetId -> dialog.entries.any { it.id == targetId } }
                        ?: dialog.entries.firstOrNull()?.id
                        ?: return@launchNonCancellable
                    val mergedIds = orderedMergeIds(dialog.entries)

                    if (mergedIds.size > 1) {
                        updateMergedEntry.awaitMerge(targetId, mergedIds)
                    }
                }
            }
            else -> return
        }
        clearSelection()
        closeDialog()
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    private suspend fun getSelectedActionEntries(): List<Entry> {
        return state.value.selectedLibraryItems
            .flatMap { it.memberEntryIds }
            .map(LibraryItemKey::id)
            .distinct()
            .mapNotNull { getEntry.await(it) }
            .distinctBy { it.id }
    }

    sealed interface Dialog {
        data object SettingsSheet : Dialog
        data class ChangeCategory(
            val items: List<LibraryItem>,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class MergeEntry(
            val entries: ImmutableList<LibraryScreenModel.MergeEntry>,
            val targetId: Long,
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

    @Immutable
    private data class ItemPreferences(
        val downloadBadge: Boolean,
        val unreadBadge: Boolean,
        val localBadge: Boolean,
        val languageBadge: Boolean,
        val entryTypeBadge: Boolean,
        val skipOutsideReleasePeriod: Boolean,

        val globalFilterDownloaded: Boolean,
        val filterDownloaded: TriState,
        val filterUnread: TriState,
        val filterNotStarted: TriState,
        val filterBookmarked: TriState,
        val filterCompleted: TriState,
        val filterIntervalCustom: TriState,
    ) {
        fun toDisplaySettings(): LibraryDisplaySettings {
            return LibraryDisplaySettings(
                downloadBadge = downloadBadge,
                unreadBadge = unreadBadge,
                localBadge = localBadge,
                languageBadge = languageBadge,
                entryTypeBadge = entryTypeBadge,
            )
        }
    }

    @Immutable
    data class LibraryData(
        val isInitialized: Boolean = false,
        val showSystemCategory: Boolean = false,
        val categories: List<Category> = emptyList(),
        val favorites: List<LibraryItem> = emptyList(),
        val tracksMap: Map<Long, List<EntryTrack>> = emptyMap(),
        val loggedInTrackerIds: Set<Long> = emptySet(),
    ) {
        val favoritesById by lazy { favorites.associateBy { it.key } }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val searchQuery: String? = null,
        val selection: Set<LibraryItemKey> = setOf(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showEntryCount: Boolean = false,
        val showContinueButton: Boolean = false,
        val displaySettings: LibraryDisplaySettings = LibraryDisplaySettings(),
        val dialog: Dialog? = null,
        val libraryData: LibraryData = LibraryData(),
        val groupType: LibraryGroupType = LibraryGroupType.Category,
        private val activePageIndex: Int = 0,
        private val groupedFavorites: List<LibraryPage> = emptyList(),
    ) {
        val displayedPages: List<LibraryPage> = groupedFavorites

        val coercedActivePageIndex = activePageIndex.coerceIn(
            minimumValue = 0,
            maximumValue = displayedPages.lastIndex.coerceAtLeast(0),
        )

        val activePage: LibraryPage? = displayedPages.getOrNull(coercedActivePageIndex)

        val activeSortCategory: Category? = activePage?.category
            ?.takeIf { groupType == LibraryGroupType.Category }

        val isLibraryEmpty = libraryData.favorites.isEmpty()

        val selectionMode = selection.isNotEmpty()

        val selectedLibraryItems by lazy {
            selection
                .mapNotNull { libraryData.favoritesById[it] }
        }

        val selectedEntryTypes by lazy {
            selectedLibraryItems.map { it.entry.type }.toSet()
        }

        fun getItemsForPageId(pageId: String?): List<LibraryItem> {
            if (pageId == null) return emptyList()
            val page = displayedPages.find { it.id == pageId } ?: return emptyList()
            return getItemsForPage(page)
        }

        fun getItemsForPage(page: LibraryPage): List<LibraryItem> {
            return page.itemIds.mapNotNull { libraryData.favoritesById[it] }
        }

        fun getItemCountForPage(page: LibraryPage): Int? {
            return if (showEntryCount || !searchQuery.isNullOrEmpty()) page.itemIds.size else null
        }

        fun getItemCountForPrimaryTab(tab: LibraryPageTab): Int? {
            if (!showEntryCount && searchQuery.isNullOrEmpty()) return null
            return displayedPages
                .filter { it.primaryTab.id == tab.id }
                .flatMap(LibraryPage::itemIds)
                .distinct()
                .size
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

internal fun LibraryItem.calculateDownloadCount(entryDownloadInteraction: EntryDownloadInteraction): Int {
    return memberEntries.sumOf(entryDownloadInteraction::getDownloadCount)
}

internal fun buildTypeLibraryPages(
    items: List<LibraryItem>,
    visibleCategories: List<Category>,
    groupType: LibraryGroupType,
    categoryTabs: Map<Long, LibraryPageTab>,
    entryTypes: List<EntryType>,
    typeTabs: Map<EntryType, LibraryPageTab>,
): List<LibraryPage> {
    return when (groupType) {
        LibraryGroupType.Type -> {
            entryTypes.map { entryType ->
                LibraryPage(
                    id = "type:${entryType.name}",
                    primaryTab = typeTabs.getValue(entryType),
                    entryType = entryType,
                    itemIds = items.fastFilter { it.entry.type == entryType }
                        .fastMap(LibraryItem::key),
                )
            }
        }
        LibraryGroupType.TypeCategory -> {
            buildList {
                entryTypes.forEach { entryType ->
                    val typeItems = items.fastFilter { it.entry.type == entryType }
                    visibleCategories.forEach { category ->
                        val itemIds = typeItems.fastFilter { category.id in it.categories }
                            .fastMap(LibraryItem::key)
                        if (itemIds.isNotEmpty()) {
                            add(
                                LibraryPage(
                                    id = "type:${entryType.name}:category:${category.id}",
                                    primaryTab = typeTabs.getValue(entryType),
                                    secondaryTab = categoryTabs.getValue(category.id),
                                    category = category,
                                    entryType = entryType,
                                    itemIds = itemIds,
                                ),
                            )
                        }
                    }
                }
            }
        }
        LibraryGroupType.CategoryType -> {
            buildList {
                visibleCategories.forEach { category ->
                    val categoryItems = items.fastFilter { category.id in it.categories }
                    if (categoryItems.isEmpty()) {
                        add(
                            LibraryPage(
                                id = "category:${category.id}",
                                primaryTab = categoryTabs.getValue(category.id),
                                category = category,
                            ),
                        )
                    } else {
                        entryTypes.forEach { entryType ->
                            val itemIds = categoryItems.fastFilter { it.entry.type == entryType }
                                .fastMap(LibraryItem::key)
                            if (itemIds.isNotEmpty()) {
                                add(
                                    LibraryPage(
                                        id = "category:${category.id}:type:${entryType.name}",
                                        primaryTab = categoryTabs.getValue(category.id),
                                        secondaryTab = typeTabs.getValue(entryType),
                                        category = category,
                                        entryType = entryType,
                                        itemIds = itemIds,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> error("Unsupported type grouping mode: $groupType")
    }
}

internal fun observeGroupedLibraryPages(
    libraryData: Flow<LibraryScreenModel.LibraryData>,
    groupType: Flow<LibraryGroupType>,
    sortingMode: Flow<LibrarySort>,
    randomSortSeed: Flow<Int>,
    applyGrouping: (LibraryScreenModel.LibraryData, LibraryGroupType) -> List<LibraryPage>,
    applySort: (
        pages: List<LibraryPage>,
        data: LibraryScreenModel.LibraryData,
        groupType: LibraryGroupType,
        sortingMode: LibrarySort,
        randomSortSeed: Int,
    ) -> List<LibraryPage>,
): Flow<Pair<LibraryGroupType, List<LibraryPage>>> {
    return combine(
        libraryData,
        groupType,
        sortingMode,
        randomSortSeed,
    ) { data, groupType, sortingMode, randomSortSeed ->
        val pages = applyGrouping(data, groupType)
        groupType to applySort(pages, data, groupType, sortingMode, randomSortSeed)
    }
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

internal fun buildMergeDialog(selection: List<LibraryItem>): LibraryScreenModel.Dialog.MergeEntry? {
    if (selection.size < 2) return null
    if (selection.map { it.entry.type }.distinct().size != 1) return null

    val mergedSelections = selection.filter { it.isMerged }
    if (mergedSelections.size > 1) return null

    val existingMerge = mergedSelections.firstOrNull()
    val newEntries = selection
        .filterNot(LibraryItem::isMerged)
        .flatMap { item -> item.toMergeEntries(isFromExistingMerge = false) }
    val existingEntries = selection
        .filter(LibraryItem::isMerged)
        .flatMap { item -> item.toMergeEntries(isFromExistingMerge = true) }
    val entries = (newEntries + existingEntries)
        .distinctBy(LibraryScreenModel.MergeEntry::id)
        .toImmutableList()

    if (entries.size < 2) return null

    return LibraryScreenModel.Dialog.MergeEntry(
        entries = entries,
        targetId = existingMerge?.entry?.id ?: entries.first().id,
        targetLocked = false,
    )
}

private fun LibraryItem.toMergeEntries(isFromExistingMerge: Boolean): List<LibraryScreenModel.MergeEntry> {
    val memberEntriesByKey = memberEntries.associateBy { LibraryItemKey(it.type, it.id) }
    return memberEntryIds.map { member ->
        LibraryScreenModel.MergeEntry(
            id = member.id,
            entry = memberEntriesByKey[member] ?: entry.copy(id = member.id, type = member.type),
            isFromExistingMerge = isFromExistingMerge,
        )
    }
}

private fun List<LibraryItem>.toEntryMergeCapabilityItems(): List<EntryMergeCapabilityItem> {
    return map { item ->
        EntryMergeCapabilityItem(
            entry = item.entry,
            isMerged = item.isMerged,
        )
    }
}

internal fun orderedMergeIds(entries: List<LibraryScreenModel.MergeEntry>): List<Long> {
    return entries.map(LibraryScreenModel.MergeEntry::id).distinct()
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

private const val LOCAL_SOURCE_ID_ALIAS = "local"
private const val MULTI_SOURCE_ID_ALIAS = "multi"

private fun LibraryItem.matches(
    query: String,
    sourceManager: SourceManager,
    categoryNamesById: Map<Long, String>,
): Boolean {
    if (query.startsWith("id:", true)) {
        return entry.id == query.substringAfter("id:").toLongOrNull()
    }
    if (query.startsWith("src:", true)) {
        val querySource = query.substringAfter("src:")
        return when {
            querySource.equals(LOCAL_SOURCE_ID_ALIAS, ignoreCase = true) ->
                displaySourceId == LocalSource.ID
            querySource.equals(MULTI_SOURCE_ID_ALIAS, ignoreCase = true) ->
                displaySourceId == LibraryItem.MULTI_SOURCE_ID
            else -> querySource.toLongOrNull() in sourceIds
        }
    }

    val sourceDisplayName by lazy { getSourceDisplayName(sourceManager) }
    val categoryNames by lazy { categories.mapNotNull(categoryNamesById::get) }
    return query.split(",").map { it.trim() }.all { subconstraint ->
        checkNegatableConstraint(subconstraint) { constraint ->
            listOfNotNull(
                entry.title,
                entry.displayName,
                entry.author,
                entry.artist,
                entry.description,
                sourceDisplayName,
                *entry.genre.orEmpty().toTypedArray(),
                *categoryNames.toTypedArray(),
            ).any { it.contains(constraint, ignoreCase = true) } ||
                entry.genre.orEmpty().any { genre -> genre.equals(constraint, ignoreCase = true) }
        }
    }
}

private fun LibraryItem.getSourceDisplayName(sourceManager: SourceManager): String {
    return when {
        sourceName.isNotBlank() -> sourceName
        displaySourceId == LibraryItem.MULTI_SOURCE_ID -> ""
        else -> sourceManager.getDisplayInfo(displaySourceId).name
    }
}

private fun checkNegatableConstraint(
    constraint: String,
    predicate: (String) -> Boolean,
): Boolean {
    return if (constraint.startsWith("-")) {
        !predicate(constraint.substringAfter("-").trimStart())
    } else {
        predicate(constraint)
    }
}
