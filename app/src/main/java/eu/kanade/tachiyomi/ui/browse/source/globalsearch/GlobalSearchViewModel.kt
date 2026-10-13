package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.base.observation.ObservedItemStore
import eu.kanade.tachiyomi.ui.base.observation.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSearchRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchResult
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class GlobalSearchViewModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
) : ViewModel() {

    val state: StateFlow<GlobalSearchViewModel.State>
        field = MutableStateFlow<GlobalSearchViewModel.State>(State(searchQuery = initialQuery))

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private var searchJob: Job? = null

    // Bumped by every search. A source answering an earlier one can finish just as the next search starts, after its
    // job was cancelled, so its result is dropped by comparing generations where the state is updated.
    private val searchGeneration = AtomicLong()
    private val itemStore = ObservedItemStore<Long, GlobalSearchItem>(
        scope = viewModelScope + Dispatchers.IO,
        keyOf = GlobalSearchItem::id,
        supersedes = { candidate, current -> candidate.entry.version >= current.entry.version },
    ) { entryIds ->
        getEntry.subscribe(entryIds).map { entries -> entries.map(::GlobalSearchItem) }
    }

    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    private val pinnedSources = sourcePreferences.pinnedSources.get()

    private var lastQuery: String? = null
    private var lastSourceFilter: SourceFilter? = null

    private var extensionFilter: String? = initialExtensionFilter

    private val sortComparator = { map: Map<EntryCatalogueSourceInfo, GlobalSearchItemResult> ->
        compareBy<EntryCatalogueSourceInfo>(
            { (map[it] as? GlobalSearchItemResult.Success)?.isEmpty ?: true },
            { "${it.id}" !in pinnedSources },
            { "${it.name.lowercase()} (${it.language})" },
        )
    }

    init {
        viewModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { onlyShowHasResults ->
                state.update { it.copy(onlyShowHasResults = onlyShowHasResults) }
            }
        }

        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // Use the custom extension filter instead of the pinned/all toggle.
                setSourceFilter(SourceFilter.All)
            } else {
                search()
            }
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        coroutineDispatcher.close()
        super.onCleared()
    }

    @Composable
    fun getItem(initialItem: GlobalSearchItem): androidx.compose.runtime.State<GlobalSearchItem> {
        return itemStore.collectAsState(initialItem)
    }

    private fun getEnabledSources(): List<EntryCatalogueSourceInfo> {
        return catalogueFeature.sources()
            .filter { it.language in enabledLanguages || it.isLocal }
            .filterNot { it.isDisabled(disabledSources) }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.language})" },
                ),
            )
    }

    private suspend fun getSelectedSources(): List<EntryCatalogueSourceInfo> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        val enabledIds = enabledSources.map { it.id }.toSet()
        val extensionSourceIds = extensionManager.getLoadedExtensions()
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .map { it.id }
            .toSet()
        return enabledSources.filter { it.id in extensionSourceIds && it.id in enabledIds }
    }

    fun updateSearchQuery(query: String?) {
        state.update { it.copy(searchQuery = query) }
    }

    fun setSourceFilter(filter: SourceFilter) {
        state.update { it.copy(sourceFilter = filter) }
        search()
    }

    fun toggleFilterResults() {
        preferences.globalSearchFilterState.toggle()
    }

    fun search() {
        val query = state.value.searchQuery
        val sourceFilter = state.value.sourceFilter

        if (query.isNullOrBlank()) return

        val sameQuery = this.lastQuery == query
        if (sameQuery && this.lastSourceFilter == sourceFilter) return

        this.lastQuery = query
        this.lastSourceFilter = sourceFilter

        searchJob?.cancel()
        val generation = searchGeneration.incrementAndGet()

        searchJob = viewModelScope.launchIO {
            val sources = getSelectedSources()
                .let {
                    if (sourceFilter != SourceFilter.PinnedOnly) {
                        it
                    } else {
                        it.filter { source -> source.isPinned(pinnedSources) }
                    }
                }

            if (sameQuery) {
                val existingResults = state.value.items
                updateItems(
                    sources.associateWith { existingResults[it] ?: GlobalSearchItemResult.Loading },
                )
            } else {
                updateItems(
                    sources.associateWith { GlobalSearchItemResult.Loading },
                )
            }

            sources.map { source ->
                async {
                    if (state.value.items[source] !is GlobalSearchItemResult.Loading) {
                        return@async
                    }

                    try {
                        val items = withContext(coroutineDispatcher) {
                            searchSource(source, query)
                        }

                        updateItem(source, GlobalSearchItemResult.Success(items), generation)
                    } catch (e: Exception) {
                        updateItem(source, GlobalSearchItemResult.Error(e), generation)
                    }
                }
            }
                .awaitAll()
        }
    }

    private suspend fun searchSource(source: EntryCatalogueSourceInfo, query: String): List<GlobalSearchItem> {
        return when (val result = catalogueFeature.search(EntryCatalogueSearchRequest(source.id, query))) {
            is EntryCatalogueSearchResult.Success ->
                result.entries
                    .let { networkToLocalEntry(it) }
                    .map(::GlobalSearchItem)
            is EntryCatalogueSearchResult.Unavailable -> emptyList()
            is EntryCatalogueSearchResult.Failed -> throw result.cause
        }
    }

    private fun updateItems(items: Map<EntryCatalogueSourceInfo, GlobalSearchItemResult>) {
        state.update {
            it.copy(
                items = items.toSortedMap(sortComparator(items)),
            )
        }
    }

    // Sources finish concurrently, so each result is merged into the state it updates rather than into a snapshot.
    private fun updateItem(source: EntryCatalogueSourceInfo, result: GlobalSearchItemResult, generation: Long) {
        state.update {
            if (generation != searchGeneration.get()) return@update it
            val items = it.items + (source to result)
            it.copy(items = items.toSortedMap(sortComparator(items)))
        }
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val sourceFilter: SourceFilter = SourceFilter.PinnedOnly,
        val onlyShowHasResults: Boolean = false,
        val items: Map<EntryCatalogueSourceInfo, GlobalSearchItemResult> = mapOf(),
    ) {
        val progress: Int = items.count { it.value !is GlobalSearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }
}

sealed interface GlobalSearchItemResult {
    data object Loading : GlobalSearchItemResult

    data class Error(
        val throwable: Throwable,
    ) : GlobalSearchItemResult

    data class Success(
        val result: List<GlobalSearchItem>,
    ) : GlobalSearchItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}

private val EntryCatalogueSourceInfo.isLocal: Boolean
    get() = id == tachiyomi.source.local.LocalSource.ID

private fun EntryCatalogueSourceInfo.isDisabled(
    disabledSources: Set<String>,
): Boolean {
    return id.toString() in disabledSources
}

private fun EntryCatalogueSourceInfo.isPinned(
    pinnedSources: Set<String>,
): Boolean {
    return id.toString() in pinnedSources
}
