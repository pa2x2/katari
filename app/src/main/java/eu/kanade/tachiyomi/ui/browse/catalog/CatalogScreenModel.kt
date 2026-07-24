package eu.kanade.tachiyomi.ui.browse.catalog

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.model.CATALOGUE_LATEST_QUERY
import eu.kanade.domain.source.model.CATALOGUE_POPULAR_QUERY
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.applySnapshot
import eu.kanade.domain.source.model.snapshot
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.buildMergeTargetQuery
import eu.kanade.presentation.entry.components.buildMergeTargets
import eu.kanade.presentation.entry.components.rankMergeTargets
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.sourceNotInstalledName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.common.CustomPreferences
import mihon.core.common.browseLongPressActionPriorityForSource
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import mihon.entry.interactions.EntryCatalogueBrowseRequest
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueListing
import mihon.entry.interactions.EntryCatalogueSourceResolution
import mihon.entry.interactions.EntryImmersiveAvailability
import mihon.entry.interactions.EntryImmersiveContext
import mihon.entry.interactions.EntryImmersiveFeature
import mihon.entry.interactions.EntryImmersiveSourceAvailability
import mihon.entry.interactions.EntryLibraryAddRequest
import mihon.entry.interactions.EntryLibraryAddResult
import mihon.entry.interactions.EntryLibraryCategorySelection
import mihon.entry.interactions.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.EntryLibraryMembershipFeature
import mihon.entry.interactions.EntryLibraryRemovalResult
import mihon.entry.interactions.EntryMergeCommitIntent
import mihon.entry.interactions.EntryMergeEditReference
import mihon.entry.interactions.EntryMergeEditorEntry
import mihon.entry.interactions.EntryMergeEditorEntryOrigin
import mihon.entry.interactions.EntryMergeEditorEntryReference
import mihon.entry.interactions.EntryMergeExecutionResult
import mihon.entry.interactions.EntryMergeFeature
import mihon.entry.interactions.EntryMergeMemberPreparationIntent
import mihon.entry.interactions.EntryMergePreparationResult
import mihon.entry.interactions.EntryMergePrepareIntent
import mihon.entry.interactions.EntryPreviewAvailability
import mihon.entry.interactions.EntryPreviewContext
import mihon.entry.interactions.EntryPreviewFeature
import mihon.entry.interactions.EntrySourceHomeFeature
import mihon.entry.interactions.EntrySourceHomeResolution
import mihon.entry.interactions.EntrySourceSettingsFeature
import mihon.entry.interactions.EntrySourceSettingsResolution
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.UUID
import eu.kanade.tachiyomi.source.entry.EntryFilter as SourceModelFilter

private val CatalogListItem.entry: Entry
    get() = (this as CatalogListItem.EntryItem).entry

class CatalogScreenModel(
    private val sourceId: Long,
    listingQuery: String?,
    private val migrationEntryType: EntryType? = null,
    private val initialFilterSnapshot: List<FilterStateNode> = emptyList(),
    private val sourceManager: SourceManager = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    customPreferences: CustomPreferences = Injekt.get(),
    private val browseFeedService: BrowseFeedService = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature = Injekt.get(),
    private val entryMergeFeature: EntryMergeFeature = Injekt.get(),
    private val categoryRepository: CategoryRepository = Injekt.get(),
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val duplicatePreferences: DuplicatePreferences = Injekt.get(),
    private val entryPreviewFeature: EntryPreviewFeature = Injekt.get(),
    private val entryImmersiveFeature: EntryImmersiveFeature = Injekt.get(),
    private val entryCatalogueFeature: EntryCatalogueFeature = Injekt.get(),
    private val entrySourceHomeFeature: EntrySourceHomeFeature = Injekt.get(),
    private val entrySourceSettingsFeature: EntrySourceSettingsFeature = Injekt.get(),
    private val getIncognitoState: GetIncognitoState = Injekt.get(),
    private val application: Application = Injekt.get(),
) : StateScreenModel<CatalogScreenModel.State>(
    initialCatalogState(listingQuery),
) {

    var displayMode by sourcePreferences.sourceDisplayMode(sourceId).asState(screenModelScope)
    var feedsEnabled by customPreferences.enableFeeds.asState(screenModelScope)
    private val defaultBrowseLongPressActionPriority by
        customPreferences.browseLongPressActionPriority.asState(screenModelScope)
    private val browseLongPressActionOverrides by
        customPreferences.browseLongPressActionOverrides.asState(screenModelScope)
    val browseLongPressActionPriority: List<CustomPreferences.BrowseLongPressAction>
        get() = browseLongPressActionPriorityForSource(
            sourceId = sourceId,
            defaultPriority = defaultBrowseLongPressActionPriority,
            overrides = browseLongPressActionOverrides,
        )

    private val filterLoader = CatalogFilterLoader(entryCatalogueFeature)
    private val presetHelper = CatalogPresetHelper(sourceId, browseFeedService, entryCatalogueFeature)

    val catalogSource =
        (entryCatalogueFeature.source(sourceId) as? EntryCatalogueSourceResolution.Available)?.source
    val isImmersiveSourceAvailable: Boolean
        get() = entryImmersiveFeature.sourceAvailability(sourceManager.get(sourceId)) is
            EntryImmersiveSourceAvailability.Available

    init {
        if (catalogSource == null) {
            mutableState.update { it.copy(filterState = FilterUiState.Unavailable) }
        } else {
            if (filterLoader.usesAsyncFilters(sourceId) && state.value.listing is Listing.Search) {
                mutableState.update {
                    it.copy(
                        filterState = FilterUiState.Loading,
                        isWaitingForInitialFilterLoad = true,
                    )
                }
            }

            screenModelScope.launchIO {
                loadFilters(initialFilterSnapshot = initialFilterSnapshot)
            }

            if (!getIncognitoState.await(sourceId)) {
                sourcePreferences.lastUsedSource.set(sourceId)
            }
        }
    }

    private val hideInLibraryItems = sourcePreferences.hideInLibraryItems.get()

    val catalogPagerFlowFlow = state.map { it.listing to it.isWaitingForInitialFilterLoad }
        .distinctUntilChanged()
        .map { (listing, isWaitingForInitialFilterLoad) ->
            if (isWaitingForInitialFilterLoad || catalogSource == null) {
                emptyFlow()
            } else {
                Pager(PagingConfig(pageSize = 25)) {
                    entryCatalogueFeature.paging(
                        EntryCatalogueBrowseRequest(sourceId, listing.toFeatureListing()),
                    )
                }.flow.map { pagingData ->
                    pagingData.map { item ->
                        val entryItem = item as CatalogListItem.EntryItem
                        getEntry.subscribe(
                            entryItem.entry.url,
                            entryItem.entry.source,
                            entryItem.entry.type,
                        )
                            .map {
                                CatalogListItem.EntryItem(
                                    it ?: entryItem.entry,
                                    entryItem.sourceItemOrientation,
                                )
                            }
                            .stateIn(ioCoroutineScope)
                    }
                        .filter { migrationEntryType == null || it.value.entry.type == migrationEntryType }
                        .filter { !hideInLibraryItems || !it.value.favorite }
                }
                    .cachedIn(ioCoroutineScope)
            }
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyFlow())

    val sourceName: String
        get() = catalogSource?.name ?: ""

    val sourceDisplayInfo: SourceDisplayInfo
        get() = sourceManager.getDisplayInfo(sourceId)

    val supportsLatest: Boolean
        get() = presetHelper.supportsLatest

    val hasFilterCapability: Boolean
        get() = state.value.filterState !is FilterUiState.Unavailable

    val sourceItemOrientation: EntryItemOrientation
        get() = catalogSource?.itemOrientation ?: EntryItemOrientation.VERTICAL

    val homeUrl: String?
        get() = (entrySourceHomeFeature.resolve(sourceId) as? EntrySourceHomeResolution.Available)?.url

    val isConfigurable: Boolean
        get() = entrySourceSettingsFeature.resolve(sourceId) is EntrySourceSettingsResolution.Available

    fun getColumnsPreference(
        orientation: Int,
        sourceItemOrientation: EntryItemOrientation = EntryItemOrientation.VERTICAL,
    ): GridCells {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = if (isLandscape) {
            libraryPreferences.landscapeColumns
        } else {
            libraryPreferences.portraitColumns
        }.get()
        val isHorizontal = sourceItemOrientation == EntryItemOrientation.HORIZONTAL
        return if (columns == 0) {
            GridCells.Adaptive(if (isHorizontal) 180.dp else 128.dp)
        } else {
            GridCells.Fixed(if (isHorizontal) (columns - 1).coerceAtLeast(1) else columns)
        }
    }

    fun resetFilters() {
        if (catalogSource == null) return
        screenModelScope.launchIO {
            loadFilters()
        }
    }

    fun setListing(listing: Listing) {
        mutableState.update { it.copy(listing = listing, toolbarQuery = null) }
    }

    fun setFilters(filters: EntryFilterList) {
        mutableState.update { it.copy(filters = filters) }
    }

    fun search(query: String? = null, filters: EntryFilterList? = null) {
        val input = state.value.listing as? Listing.Search
            ?: Listing.Search(query = null, filters = state.value.filters)

        mutableState.update {
            it.copy(
                listing = input.copy(
                    query = query ?: input.query,
                    filters = filters ?: input.filters,
                ),
                toolbarQuery = query ?: input.query,
            )
        }
    }

    fun searchGenre(genreName: String) {
        if (catalogSource == null) return

        screenModelScope.launchIO {
            val defaultFilters = freshResolvedFilters() ?: return@launchIO
            var genreExists = false

            filter@ for (sourceFilter in defaultFilters) {
                if (sourceFilter is SourceModelFilter.Group<*>) {
                    for (filter in sourceFilter.state) {
                        if (filter is SourceModelFilter<*> && filter.name.equals(genreName, true)) {
                            when (filter) {
                                is SourceModelFilter.TriState -> filter.state = 1
                                is SourceModelFilter.CheckBox -> filter.state = true
                                else -> {}
                            }
                            genreExists = true
                            break@filter
                        }
                    }
                } else if (sourceFilter is SourceModelFilter.Select<*>) {
                    val index = sourceFilter.values.filterIsInstance<String>()
                        .indexOfFirst { it.equals(genreName, true) }

                    if (index != -1) {
                        sourceFilter.state = index
                        genreExists = true
                        break
                    }
                }
            }

            mutableState.update {
                val listing = if (genreExists) {
                    Listing.Search(query = null, filters = defaultFilters)
                } else {
                    Listing.Search(query = genreName, filters = defaultFilters)
                }
                it.copy(
                    filters = defaultFilters,
                    listing = listing,
                    toolbarQuery = listing.query,
                    filterState = FilterUiState.Ready,
                    isWaitingForInitialFilterLoad = false,
                )
            }
        }
    }

    fun openFilterSheet() {
        if (catalogSource == null) return
        mutableState.update { it.copy(dialog = Dialog.Filter) }
        if (state.value.filterState is FilterUiState.Uninitialized) {
            screenModelScope.launchIO {
                loadFilters()
            }
        }
    }

    fun retryFilterLoad() {
        if (catalogSource == null) return
        screenModelScope.launchIO {
            loadFilters(
                initialFilterSnapshot = if (state.value.isWaitingForInitialFilterLoad) {
                    initialFilterSnapshot
                } else {
                    emptyList()
                },
            )
        }
    }

    private suspend fun loadFilters(initialFilterSnapshot: List<FilterStateNode> = emptyList()) {
        val keepWaitingOnFailure = state.value.isWaitingForInitialFilterLoad

        mutableState.update { it.copy(filterState = FilterUiState.Loading) }

        runCatching {
            filterLoader.load(sourceId)
        }.onSuccess { sourceFilters ->
            mutableState.update {
                it.initializeForSource(
                    sourceFilters = sourceFilters,
                    initialFilterSnapshot = initialFilterSnapshot,
                ).copy(
                    filterState = FilterUiState.Ready,
                    isWaitingForInitialFilterLoad = false,
                )
            }
        }.onFailure { throwable ->
            mutableState.update {
                it.copy(
                    filterState = FilterUiState.Error(throwable),
                    isWaitingForInitialFilterLoad = keepWaitingOnFailure,
                )
            }
        }
    }

    private suspend fun freshResolvedFilters(): EntryFilterList? {
        return runCatching {
            filterLoader.load(sourceId)
        }.onFailure { throwable ->
            mutableState.update {
                it.copy(
                    filterState = FilterUiState.Error(throwable),
                    isWaitingForInitialFilterLoad = it.isWaitingForInitialFilterLoad,
                )
            }
        }.getOrNull()
    }

    // region Library actions

    fun changeFavorite(item: CatalogListItem) {
        changeFavorite(item.entry)
    }

    fun addFavorite(item: CatalogListItem) {
        addFavorite(item.entry)
    }

    fun addFavorite(entry: Entry) {
        screenModelScope.launchIO {
            applyLibraryAdd(
                EntryLibraryAddRequest(entry, duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW),
            )
        }
    }

    fun addFavorite(entry: Entry, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            applyLibraryAdd(
                EntryLibraryAddRequest(
                    entry = entry,
                    duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW,
                    categorySelection = EntryLibraryCategorySelection.Selected(categoryIds),
                ),
            )
        }
    }

    fun confirmBrowseLibraryAction(item: CatalogListItem) {
        confirmBrowseLibraryAction(item.entry)
    }

    fun confirmBrowseLibraryAction(entry: Entry) {
        screenModelScope.launchIO {
            handleLibraryAction(entry)
        }
    }

    internal suspend fun onItemLongClick(
        item: CatalogListItem,
    ): BrowseLongPressOutcome {
        val immersiveAvailable = entryImmersiveFeature.availability(
            EntryImmersiveContext(
                entry = item.entry,
                source = sourceManager.get(item.entry.source),
            ),
        ) is EntryImmersiveAvailability.Available
        return when (
            resolveBrowseLongPressAction(
                priority = browseLongPressActionPriority,
                immersiveAvailable = immersiveAvailable,
                previewEnabled = entryPreviewFeature.availability(
                    EntryPreviewContext(
                        entry = item.entry,
                        source = sourceManager.getOrStub(item.entry.source),
                    ),
                ) is EntryPreviewAvailability.Available,
            )
        ) {
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION -> {
                showLibraryActionChooserOrHandle(item.entry)
                BrowseLongPressOutcome.Handled
            }
            CustomPreferences.BrowseLongPressAction.PREVIEW -> {
                setDialog(Dialog.EntryPreview(item.entry.id))
                BrowseLongPressOutcome.Handled
            }
            CustomPreferences.BrowseLongPressAction.IMMERSIVE -> BrowseLongPressOutcome.StartImmersive
        }
    }

    private suspend fun handleLibraryAction(entry: Entry) {
        if (entry.favorite) {
            setDialog(Dialog.RemoveEntry(entry))
            return
        }

        setDialog(Dialog.CheckingDuplicates)
        applyLibraryAdd(EntryLibraryAddRequest(entry))
    }

    fun changeFavorite(entry: Entry) {
        screenModelScope.launchIO {
            if (entry.favorite) {
                when (val result = entryLibraryMembershipFeature.remove(listOf(entry))) {
                    is EntryLibraryRemovalResult.Failed -> logcat(LogPriority.ERROR, result.cause)
                    is EntryLibraryRemovalResult.Removed,
                    EntryLibraryRemovalResult.NoChange,
                    -> Unit
                }
            } else {
                applyLibraryAdd(
                    EntryLibraryAddRequest(entry, duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW),
                )
            }
        }
    }

    private suspend fun applyLibraryAdd(request: EntryLibraryAddRequest) {
        when (val result = entryLibraryMembershipFeature.add(request)) {
            is EntryLibraryAddResult.DuplicateCandidates -> setDialog(
                Dialog.DuplicateEntry(result.entry, result.candidates),
            )
            is EntryLibraryAddResult.CategorySelectionRequired -> setDialog(
                Dialog.ChangeEntryCategory(
                    result.entry,
                    result.categories.mapAsCheckboxState { it.id in result.selectedCategoryIds },
                ),
            )
            is EntryLibraryAddResult.Failed -> {
                setDialog(null)
                logcat(LogPriority.ERROR, result.cause)
            }
            is EntryLibraryAddResult.Added,
            is EntryLibraryAddResult.AlreadyInLibrary,
            -> setDialog(null)
        }
    }

    fun showMigrateEntryDialog(current: Entry, target: Entry) {
        setDialog(Dialog.MigrateEntry(current = current, target = target))
    }

    // endregion

    // region Merge

    fun showMergeTargetPicker(item: CatalogListItem) {
        showEntryMergeTargetPicker(item.entry)
    }

    fun showMergeTargetPicker(entry: Entry) {
        showEntryMergeTargetPicker(entry)
    }

    private fun showEntryMergeTargetPicker(entry: Entry) {
        screenModelScope.launchIO {
            val targets = buildMergeTargets(
                libraryItems = getLibraryEntries.await(),
                sourceManager = sourceManager,
                entryType = entry.type,
            )
            if (targets.isEmpty()) return@launchIO
            val query = buildMergeTargetQuery(entry.displayTitle, duplicatePreferences)
            val visibleTargets = rankMergeTargets(targets, query).toImmutableList()
            setDialog(
                Dialog.SelectEntryMergeTarget(
                    entry = entry,
                    query = query,
                    targets = targets,
                    visibleTargets = visibleTargets,
                ),
            )
        }
    }

    fun updateMergeTargetQuery(query: String) {
        when (val dialog = state.value.dialog) {
            is Dialog.SelectEntryMergeTarget -> {
                val visibleTargets = rankMergeTargets(dialog.targets, query).toImmutableList()
                setDialog(dialog.copy(query = query, visibleTargets = visibleTargets))
            }
            else -> {}
        }
    }

    fun openMergeEditor(targetId: Long) {
        when (val dialog = state.value.dialog) {
            is Dialog.SelectEntryMergeTarget -> {
                screenModelScope.launchIO {
                    val target = dialog.targets.firstOrNull { it.id == targetId } ?: return@launchIO
                    val editor = createEntryMergeEditorDialog(dialog.entry, target) ?: return@launchIO
                    setDialog(editor)
                }
            }
            else -> {}
        }
    }

    fun moveMergeEntry(fromIndex: Int, toIndex: Int) {
        mutableState.update { currentState ->
            when (val dialog = currentState.dialog) {
                is Dialog.EditEntryMerge -> {
                    val entries = dialog.entries.toMutableList()
                    if (fromIndex !in entries.indices || toIndex !in entries.indices) return@update currentState
                    val entry = entries.removeAt(fromIndex)
                    entries.add(toIndex, entry)
                    currentState.copy(dialog = dialog.copy(entries = entries.toImmutableList()))
                }
                else -> currentState
            }
        }
    }

    fun setMergeTarget(itemId: Long) {
        mutableState.update { currentState ->
            when (val dialog = currentState.dialog) {
                is Dialog.EditEntryMerge -> {
                    val entry = dialog.entries.firstOrNull { it.id == itemId } ?: return@update currentState
                    val reference = dialog.referencesById[itemId] ?: return@update currentState
                    if (dialog.targetLocked) return@update currentState
                    currentState.copy(
                        dialog = dialog.copy(
                            targetId = itemId,
                            targetReference = reference,
                            removedIds = dialog.removedIds - itemId,
                            libraryRemovalIds = dialog.libraryRemovalIds - itemId,
                        ),
                    )
                }
                else -> currentState
            }
        }
    }

    fun toggleMergeEntryRemoval(itemId: Long) {
        mutableState.update { currentState ->
            when (val dialog = currentState.dialog) {
                is Dialog.EditEntryMerge -> {
                    val entry = dialog.entries.firstOrNull { it.id == itemId } ?: return@update currentState
                    if (!entry.isRemovable || itemId == dialog.targetId) return@update currentState
                    val removedIds = dialog.removedIds.toMutableSet().apply {
                        if (!add(itemId)) remove(itemId)
                    }
                    currentState.copy(dialog = dialog.copy(removedIds = removedIds))
                }
                else -> currentState
            }
        }
    }

    fun toggleMergeEntryLibraryRemoval(itemId: Long) {
        mutableState.update { currentState ->
            when (val dialog = currentState.dialog) {
                is Dialog.EditEntryMerge -> {
                    val entry = dialog.entries.firstOrNull { it.id == itemId } ?: return@update currentState
                    if (!entry.isRemovable || itemId == dialog.targetId) return@update currentState
                    val libraryRemovalIds = dialog.libraryRemovalIds.toMutableSet().apply {
                        if (!add(itemId)) remove(itemId)
                    }
                    currentState.copy(dialog = dialog.copy(libraryRemovalIds = libraryRemovalIds))
                }
                else -> currentState
            }
        }
    }

    fun confirmBrowseMerge() {
        when (val dialog = state.value.dialog) {
            is Dialog.EditEntryMerge -> {
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
            else -> {}
        }
    }

    private suspend fun createEntryMergeEditorDialog(
        entry: Entry,
        target: MergeTarget,
    ): Dialog.EditEntryMerge? {
        val targetEntry = target.memberEntries.firstOrNull { it.id == target.id }
            ?: target.memberEntries.firstOrNull()
            ?: return null
        val categoryIds = target.categoryIds.ifEmpty {
            categoryRepository.getCategoriesByEntryId(targetEntry.id).map(Category::id)
        }
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
        return Dialog.EditEntryMerge(
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

    private fun toMergeEditorEntry(item: EntryMergeEditorEntry): MergeEditorEntry {
        val existing = item.origin == EntryMergeEditorEntryOrigin.EXISTING_MEMBER
        return MergeEditorEntry(
            id = item.entry.id,
            entry = item.entry,
            subtitle = getEntrySourceSubtitle(item.entry) + if (existing) "" else " • New",
            isRemovable = item.removable,
            isMember = existing,
        )
    }

    private fun getEntrySourceSubtitle(entry: Entry): String {
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

    private fun getSourceNameForId(sourceId: Long): String {
        val sourceInfo = sourceManager.getDisplayInfo(sourceId)
        return if (sourceInfo.isMissing) {
            application.stringResource(MR.strings.source_not_installed, sourceInfo.sourceNotInstalledName())
        } else {
            sourceInfo.name
        }
    }

    // endregion

    // region Presets

    fun showSavePresetDialog() {
        if (!feedsEnabled) return
        setDialog(
            Dialog.SavePreset(
                mode = Dialog.SavePreset.Mode.Create,
                name = "",
                chronological = state.value.listing != Listing.Popular,
            ),
        )
    }

    fun showUpdateCurrentPresetDialog() {
        if (!feedsEnabled) return

        val preset = appliedCustomPreset() ?: return

        setDialog(
            Dialog.SavePreset(
                mode = Dialog.SavePreset.Mode.UpdateFromCurrentState,
                presetId = preset.id,
                name = preset.name,
                chronological = preset.chronological,
            ),
        )
    }

    fun showEditPresetDialog(presetId: String) {
        if (!feedsEnabled) return

        val preset = presetHelper.customPreset(presetId) ?: return

        setDialog(
            Dialog.SavePreset(
                mode = Dialog.SavePreset.Mode.EditMetadata,
                presetId = preset.id,
                name = preset.name,
                chronological = preset.chronological,
            ),
        )
    }

    fun appliedCustomPreset(): SourceFeedPreset? {
        if (!feedsEnabled) return null
        return presetHelper.customPreset(state.value.appliedCustomPresetId)
    }

    fun feedPresets(): List<SourceFeedPreset> {
        if (!feedsEnabled) return emptyList()
        return presetHelper.feedPresets()
    }

    fun applyPreset(presetId: String) {
        if (!feedsEnabled) return
        if (catalogSource == null) return

        val preset = feedPresets().firstOrNull { it.id == presetId } ?: return
        when (preset.listingMode) {
            FeedListingMode.Popular -> {
                resetFilters()
                setListing(Listing.Popular)
            }
            FeedListingMode.Latest -> {
                resetFilters()
                setListing(Listing.Latest)
            }
            FeedListingMode.Search -> {
                screenModelScope.launchIO {
                    val filters = freshResolvedFilters()?.applySnapshot(preset.filters) ?: return@launchIO
                    mutableState.update {
                        it.copy(
                            filters = filters,
                            listing = Listing.Search(query = preset.query, filters = filters),
                            toolbarQuery = preset.query,
                            appliedCustomPresetId = preset.id.takeIf(presetHelper::canDeletePreset),
                            filterState = FilterUiState.Ready,
                        )
                    }
                }
                return
            }
        }

        mutableState.update {
            it.copy(appliedCustomPresetId = preset.id.takeIf(presetHelper::canDeletePreset))
        }
    }

    fun canDeletePreset(presetId: String): Boolean {
        return presetHelper.canDeletePreset(presetId)
    }

    fun removePreset(presetId: String) {
        if (!feedsEnabled) return
        if (!presetHelper.canDeletePreset(presetId)) return

        presetHelper.removePreset(presetId)
        mutableState.update {
            if (it.appliedCustomPresetId == presetId) {
                it.copy(appliedCustomPresetId = null)
            } else {
                it
            }
        }
    }

    fun hasPresetName(name: String, excludingPresetId: String? = null): Boolean {
        if (!feedsEnabled) return false
        return presetHelper.hasPresetName(name, excludingPresetId)
    }

    fun savePreset(name: String, chronological: Boolean) {
        if (!feedsEnabled) return

        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val dialog = state.value.dialog as? Dialog.SavePreset ?: return
        when (dialog.mode) {
            Dialog.SavePreset.Mode.Create -> {
                if (catalogSource == null) return

                screenModelScope.launchIO {
                    val defaultFilters = freshResolvedFilters() ?: return@launchIO
                    val presetState = state.value.toSavedPresetState(defaultFilters = defaultFilters)
                    val preset = SourceFeedPreset(
                        id = UUID.randomUUID().toString(),
                        sourceId = sourceId,
                        name = trimmed,
                        listingMode = presetState.listingMode,
                        chronological = chronological,
                        query = presetState.query,
                        filters = presetState.filters,
                    )
                    presetHelper.savePreset(preset)
                    mutableState.update {
                        it.copy(
                            appliedCustomPresetId = preset.id,
                            dialog = null,
                            filterState = FilterUiState.Ready,
                        )
                    }
                }
                return
            }
            Dialog.SavePreset.Mode.EditMetadata -> {
                val preset = presetHelper.customPreset(dialog.presetId) ?: return
                presetHelper.savePreset(
                    preset.copy(
                        name = trimmed,
                        chronological = chronological,
                    ),
                )
                setDialog(null)
            }
            Dialog.SavePreset.Mode.UpdateFromCurrentState -> {
                if (catalogSource == null) return

                val preset = presetHelper.customPreset(dialog.presetId) ?: return
                screenModelScope.launchIO {
                    val defaultFilters = freshResolvedFilters() ?: return@launchIO
                    val presetState = state.value.toSavedPresetState(defaultFilters = defaultFilters)
                    presetHelper.savePreset(
                        preset.copy(
                            name = trimmed,
                            chronological = chronological,
                            listingMode = presetState.listingMode,
                            query = presetState.query,
                            filters = presetState.filters,
                        ),
                    )
                    mutableState.update {
                        it.copy(
                            appliedCustomPresetId = preset.id,
                            dialog = null,
                            filterState = FilterUiState.Ready,
                        )
                    }
                }
                return
            }
        }
    }

    // endregion

    fun dismissDialog() {
        setDialog(null)
    }

    fun setToolbarQuery(query: String?) {
        mutableState.update { it.copy(toolbarQuery = query) }
    }

    private fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    private suspend fun showLibraryActionChooserOrHandle(entry: Entry) {
        if (getLibraryEntries.await().isEmpty()) {
            handleLibraryAction(entry)
        } else {
            setDialog(Dialog.LibraryActionChooser(entry))
        }
    }

    sealed class Listing(open val query: String?, open val filters: EntryFilterList) {
        data object Popular : Listing(query = CATALOGUE_POPULAR_QUERY, filters = EntryFilterList())
        data object Latest : Listing(query = CATALOGUE_LATEST_QUERY, filters = EntryFilterList())
        data class Search(
            override val query: String?,
            override val filters: EntryFilterList,
        ) : Listing(query = query, filters = filters)

        companion object {
            fun valueOf(query: String?): Listing {
                return when (query) {
                    CATALOGUE_POPULAR_QUERY -> Popular
                    CATALOGUE_LATEST_QUERY -> Latest
                    else -> Search(query = query, filters = EntryFilterList())
                }
            }
        }
    }

    sealed interface Dialog {
        data object Filter : Dialog

        data class SavePreset(
            val mode: Mode,
            val presetId: String? = null,
            val name: String = "",
            val chronological: Boolean,
        ) : Dialog {
            enum class Mode {
                Create,
                EditMetadata,
                UpdateFromCurrentState,
            }
        }

        data class EntryPreview(val entryId: Long) : Dialog

        data class LibraryActionChooser(val entry: Entry) : Dialog

        data object CheckingDuplicates : Dialog

        data class RemoveEntry(val entry: Entry) : Dialog

        data class DuplicateEntry(
            val entry: Entry,
            val duplicates: List<DuplicateEntryCandidate>,
        ) : Dialog

        data class MigrateEntry(val target: Entry, val current: Entry) : Dialog

        data class SelectEntryMergeTarget(
            val entry: Entry,
            val query: String = "",
            val targets: ImmutableList<MergeTarget>,
            val visibleTargets: ImmutableList<MergeTarget>,
        ) : Dialog

        data class EditEntryMerge(
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

        data class ChangeEntryCategory(
            val entry: Entry,
            val initialSelection: List<CheckboxState.State<Category>>,
        ) : Dialog
    }

    @Immutable
    data class State(
        val listing: Listing,
        val filters: EntryFilterList = EntryFilterList(),
        val filterState: FilterUiState = FilterUiState.Uninitialized,
        val isWaitingForInitialFilterLoad: Boolean = false,
        val toolbarQuery: String? = null,
        val appliedCustomPresetId: String? = null,
        val dialog: Dialog? = null,
    ) {
        val isUserQuery get() = listing is Listing.Search && !listing.query.isNullOrEmpty()
        val hasFilterCapability get() = filterState !is FilterUiState.Unavailable
    }
}

private fun CatalogScreenModel.Listing.toFeatureListing(): EntryCatalogueListing {
    return when (this) {
        CatalogScreenModel.Listing.Popular -> EntryCatalogueListing.Popular
        CatalogScreenModel.Listing.Latest -> EntryCatalogueListing.Latest
        is CatalogScreenModel.Listing.Search -> EntryCatalogueListing.Search(query.orEmpty(), filters)
    }
}

private fun Map<Long, EntryMergeEditorEntryReference>.referencesFor(
    entryIds: Collection<Long>,
): Set<EntryMergeEditorEntryReference> {
    val ids = entryIds.toSet()
    return filterKeys { it in ids }.values.toSet()
}

sealed interface FilterUiState {
    data object Uninitialized : FilterUiState
    data object Loading : FilterUiState
    data object Ready : FilterUiState
    data class Error(val throwable: Throwable) : FilterUiState
    data object Unavailable : FilterUiState
}

internal fun CatalogScreenModel.State.initializeForSource(
    sourceFilters: EntryFilterList,
    initialFilterSnapshot: List<FilterStateNode> = emptyList(),
): CatalogScreenModel.State {
    val filters = sourceFilters.applySnapshot(initialFilterSnapshot)
    val query = (listing as? CatalogScreenModel.Listing.Search)?.query
    val updatedListing = when (listing) {
        is CatalogScreenModel.Listing.Search -> CatalogScreenModel.Listing.Search(query, filters)
        else -> listing
    }

    return copy(
        listing = updatedListing,
        filters = filters,
        toolbarQuery = query,
    )
}

internal fun initialCatalogState(listingQuery: String?): CatalogScreenModel.State {
    return CatalogScreenModel.State(
        listing = CatalogScreenModel.Listing.valueOf(listingQuery),
    )
}

internal enum class BrowseLongPressOutcome {
    Handled,
    StartImmersive,
}

internal fun resolveBrowseLongPressAction(
    priority: Collection<CustomPreferences.BrowseLongPressAction>,
    immersiveAvailable: Boolean,
    previewEnabled: Boolean,
): CustomPreferences.BrowseLongPressAction {
    return sanitizeBrowseLongPressActionPriority(priority).first { action ->
        when (action) {
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION -> true
            CustomPreferences.BrowseLongPressAction.PREVIEW -> previewEnabled
            CustomPreferences.BrowseLongPressAction.IMMERSIVE -> immersiveAvailable
        }
    }
}

internal data class SavedPresetState(
    val listingMode: FeedListingMode,
    val query: String?,
    val filters: List<FilterStateNode>,
)

internal fun CatalogScreenModel.State.toSavedPresetState(defaultFilters: EntryFilterList): SavedPresetState {
    val filterSnapshot = filters.snapshot()
    val hasEditedFilters = filterSnapshot != defaultFilters.snapshot()
    val listingMode = when {
        listing is CatalogScreenModel.Listing.Search || hasEditedFilters -> FeedListingMode.Search
        listing == CatalogScreenModel.Listing.Popular -> FeedListingMode.Popular
        else -> FeedListingMode.Latest
    }
    val query = (listing as? CatalogScreenModel.Listing.Search)
        ?.query
        ?.trim()
        ?.takeIf { listingMode == FeedListingMode.Search && it.isNotEmpty() }

    return SavedPresetState(
        listingMode = listingMode,
        query = query,
        filters = filterSnapshot,
    )
}
