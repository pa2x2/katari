package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.SortedMap

class SourcesScreenModel(
    private val getEnabledCatalogSources: GetEnabledCatalogSources = Injekt.get(),
    private val getLanguagesWithCatalogSources: GetLanguagesWithCatalogSources = Injekt.get(),
    private val toggleSource: ToggleSource = Injekt.get(),
    private val toggleSourcePin: ToggleSourcePin = Injekt.get(),
    private val toggleLanguage: ToggleLanguage = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val contentTypeFilterController: BrowseContentTypeFilterController =
        BrowseContentTypeFilterController(preferences),
) : StateScreenModel<SourcesScreenModel.State>(State()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            combine(
                getEnabledCatalogSources.subscribe(),
                contentTypeFilterController.changes(),
            ) { sources, contentTypeFilter ->
                sources.filter { contentTypeFilter.matches(it.supportedEntryTypes) } to contentTypeFilter
            }
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(Event.FailedFetchingSources)
                }
                .collectLatest { (sources, contentTypeFilter) ->
                    collectLatestSources(sources, contentTypeFilter)
                }
        }

        screenModelScope.launchIO {
            combine(
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
                .collectLatest { filter ->
                    mutableState.update { it.copy(filter = filter) }
                }
        }
    }

    private fun collectLatestSources(
        sources: List<Source>,
        contentTypeFilter: ContentTypeFilter,
    ) {
        mutableState.update { state ->
            state.copy(
                listState = SourceListUiMapper.map(sources),
                contentTypeFilter = contentTypeFilter,
            )
        }
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

    fun showSourceDialog(source: Source) {
        mutableState.update { it.copy(dialog = Dialog(source)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
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
