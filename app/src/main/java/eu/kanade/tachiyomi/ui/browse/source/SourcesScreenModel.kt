package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.SortedMap
import kotlin.time.Duration.Companion.seconds

class SourcesScreenModel(
    private val getEnabledCatalogSources: GetEnabledCatalogSources = Injekt.get(),
    private val getLanguagesWithCatalogSources: GetLanguagesWithCatalogSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleSourcePin: ToggleSourcePin = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val contentTypeFilterController: BrowseContentTypeFilterController =
        BrowseContentTypeFilterController(preferences),
) : ScreenModel {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val dialog = MutableStateFlow<Dialog?>(null)

    private val sources = combine(
        getEnabledCatalogSources.subscribe(),
        contentTypeFilterController.changes(),
    ) { sources, contentTypeFilter ->
        val listState = SourceListUiMapper.map(
            sources.filter { contentTypeFilter.matches(it.supportedEntryTypes) },
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
        dialog,
    ) { (listState, contentTypeFilter), filter, dialog ->
        State(
            dialog = dialog,
            listState = listState,
            contentTypeFilter = contentTypeFilter,
            filter = filter,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

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

    fun showSourceDialog(source: Source) {
        dialog.update { Dialog(source) }
    }

    fun closeDialog() {
        dialog.update { null }
    }

    sealed interface Event {
        data object FailedFetchingSources : Event
    }

    data class Dialog(val source: Source)

    data class State(
        val dialog: Dialog? = null,
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
