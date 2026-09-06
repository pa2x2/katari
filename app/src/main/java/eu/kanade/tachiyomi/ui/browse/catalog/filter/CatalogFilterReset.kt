package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Reload source defaults into the editor while keeping the applied search and preset intact. */
internal class CatalogFilterReset(
    private val state: MutableStateFlow<CatalogScreenModel.State>,
    private val loadFilters: suspend () -> EntryFilterList,
    private val discardEdits: () -> Unit,
    private val retainSessions: (EntryFilterList) -> Unit,
) {
    suspend fun reset() {
        discardEdits()
        state.update { it.copy(filterState = FilterUiState.Loading, filterResetPending = true) }
        try {
            val filters = loadFilters()
            currentCoroutineContext().ensureActive()
            val defaults = filters.detachedCopy()
            retainSessions(filters)
            state.update {
                it.copy(
                    filters = filters,
                    defaultFilters = defaults,
                    repairIssues = emptyList(),
                    filterState = FilterUiState.Ready,
                    filterResetPending = false,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            state.update { it.copy(filterState = FilterUiState.Error(error)) }
        }
    }
}
