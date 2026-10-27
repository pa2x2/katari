package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.interactor.GetEnabledCatalogSources
import eu.kanade.domain.source.interactor.GetLanguagesWithCatalogSources
import eu.kanade.domain.source.interactor.SourceListState
import eu.kanade.domain.source.interactor.SourceListUiMapper
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.BrowseContentTypeFilterController
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import logcat.LogPriority
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Source
import java.util.SortedMap
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class SourcesViewModel(
    private val getEnabledCatalogSources: GetEnabledCatalogSources,
    private val getLanguagesWithCatalogSources: GetLanguagesWithCatalogSources,
    private val toggleSource: ToggleSource,
    private val toggleSourcePin: ToggleSourcePin,
    private val toggleLanguage: ToggleLanguage,
    private val preferences: SourcePreferences,
    private val contentTypeFilterController: BrowseContentTypeFilterController,
) : ViewModel() {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    private val sources = combine(
        getEnabledCatalogSources.subscribe(),
        contentTypeFilterController.changes(),
        searchQuery,
    ) { sources, contentTypeFilter, searchQuery ->
        val listState = SourceListUiMapper.map(
            sources.filter { contentTypeFilter.matches(it.supportedEntryTypes) },
            searchQuery,
        )
        listState to contentTypeFilter
    }
        .catch {
            logcat(LogPriority.ERROR, it)
            _events.send(Event.FailedFetchingSources)
        }

    private val filter = combine(
        getLanguagesWithCatalogSources.subscribe(),
        preferences.enabledLanguages.changes(),
        preferences.disabledSources.changes(),
        contentTypeFilterController.changes(),
    ) { items, enabledLanguages, disabledSources, contentTypeFilter ->
        SourcesFilterState(
            items = items,
            enabledLanguages = enabledLanguages,
            disabledSources = disabledSources,
            contentTypeFilter = contentTypeFilter,
        )
    }
        .catch {
            logcat(LogPriority.ERROR, it)
            _events.send(Event.FailedFetchingSources)
        }
        // The source list must not wait for the filter sheet's data
        .onStart { emit(SourcesFilterState()) }

    val state: StateFlow<State> = combine(
        sources,
        filter,
    ) { (listState, contentTypeFilter), filter ->
        State(
            listState = listState,
            contentTypeFilter = contentTypeFilter,
            filter = filter,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    fun search(query: String) {
        searchQuery.value = query
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun togglePin(source: Source) {
        toggleSourcePin.await(source)
    }

    fun toggleLanguage(language: String) {
        toggleLanguage.await(language)
    }

    fun showAllContentTypes() {
        contentTypeFilterController.showAll()
    }

    fun toggleContentType(entryType: EntryType) {
        contentTypeFilterController.toggle(entryType)
    }

    fun toggleUnspecifiedContentType() {
        contentTypeFilterController.toggleUnspecified()
    }

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    data class State(
        val listState: SourceListState = SourceListState(),
        val contentTypeFilter: ContentTypeFilter = ContentTypeFilter(),
        val filter: SourcesFilterState = SourcesFilterState(),
    ) {
        val isLoading get() = listState.isLoading
        val items get() = listState.items
        val isEmpty get() = listState.isEmpty
    }
}

@Immutable
data class SourcesFilterState(
    val items: SortedMap<String, List<Source>> = sortedMapOf(),
    val enabledLanguages: Set<String> = emptySet(),
    val disabledSources: Set<String> = emptySet(),
    val contentTypeFilter: ContentTypeFilter = ContentTypeFilter(),
)
