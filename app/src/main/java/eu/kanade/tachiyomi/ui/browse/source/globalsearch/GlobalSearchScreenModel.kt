package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueSearchRequest
import mihon.entry.interactions.EntryCatalogueSearchResult
import mihon.entry.interactions.EntryCatalogueSourceInfo
import tachiyomi.core.common.preference.toggle
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

class GlobalSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val catalogueFeature: EntryCatalogueFeature = Injekt.get(),
) : StateScreenModel<GlobalSearchScreenModel.State>(State(searchQuery = initialQuery)) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private var searchJob: Job? = null

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
        screenModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { onlyShowHasResults ->
                mutableState.update { it.copy(onlyShowHasResults = onlyShowHasResults) }
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

    @Composable
    fun getItem(initialItem: GlobalSearchItem): androidx.compose.runtime.State<GlobalSearchItem> {
        return produceState(initialValue = initialItem) {
            getEntry.subscribe(initialItem.url, initialItem.source, initialItem.entryType)
                .collectLatest { entry ->
                    if (entry != null) value = GlobalSearchItem(entry)
                }
        }
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

    private fun getSelectedSources(): List<EntryCatalogueSourceInfo> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        val enabledIds = enabledSources.map { it.id }.toSet()
        val extensionSourceIds = extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .map { it.id }
            .toSet()
        return enabledSources.filter { it.id in extensionSourceIds && it.id in enabledIds }
    }

    fun updateSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setSourceFilter(filter: SourceFilter) {
        mutableState.update { it.copy(sourceFilter = filter) }
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

        searchJob = ioCoroutineScope.launch {
            sources.map { source ->
                async {
                    if (state.value.items[source] !is GlobalSearchItemResult.Loading) {
                        return@async
                    }

                    try {
                        val items = withContext(coroutineDispatcher) {
                            searchSource(source, query)
                        }

                        if (isActive) {
                            updateItem(source, GlobalSearchItemResult.Success(items))
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            updateItem(source, GlobalSearchItemResult.Error(e))
                        }
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
        mutableState.update {
            it.copy(
                items = items.toSortedMap(sortComparator(items)),
            )
        }
    }

    private fun updateItem(source: EntryCatalogueSourceInfo, result: GlobalSearchItemResult) {
        updateItems(state.value.items + (source to result))
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
