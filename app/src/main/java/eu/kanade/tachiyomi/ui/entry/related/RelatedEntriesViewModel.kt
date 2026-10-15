package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.base.observation.ObservedItemStore
import eu.kanade.tachiyomi.ui.base.observation.collectAsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import mihon.app.di.AppUiScope
import mihon.entry.interactions.child.EntryRelatedEntriesFeature
import mihon.entry.interactions.child.EntryRelatedEntriesLoadResult
import mihon.entry.interactions.child.EntryRelatedEntriesUnavailableReason
import tachiyomi.domain.entry.model.Entry

@AssistedInject
class RelatedEntriesViewModel(
    @Assisted private val entryId: Long,
    private val relatedEntriesFeature: EntryRelatedEntriesFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entryId: Long): RelatedEntriesViewModel
    }

    val state: StateFlow<RelatedEntriesViewModel.State>
        field = MutableStateFlow<RelatedEntriesViewModel.State>(State.Idle)

    private var loadJob: Job? = null
    private var loadGeneration = 0L
    private val entryStore = ObservedItemStore<Long, Entry>(
        scope = viewModelScope + Dispatchers.IO,
        keyOf = Entry::id,
        supersedes = { candidate, current -> candidate.version >= current.version },
        observeItems = relatedEntriesFeature::observeEntries,
    )

    @Composable
    fun getEntryState(initialEntry: Entry): androidx.compose.runtime.State<Entry> {
        return entryStore.collectAsState(initialEntry)
    }

    fun load() {
        if (state.value is State.Loading || state.value is State.Success) return
        load(force = false)
    }

    fun retry() {
        load(force = true, retainSuccess = false)
    }

    fun refresh() {
        val currentState = state.value as? State.Success ?: return
        if (currentState.isRefreshing) return
        load(force = true, retainSuccess = true)
    }

    private fun load(
        force: Boolean,
        retainSuccess: Boolean = false,
    ) {
        if (!force && loadJob?.isActive == true) return

        loadJob?.cancel()
        val generation = ++loadGeneration
        state.update { currentState ->
            if (retainSuccess && currentState is State.Success) {
                currentState.copy(isRefreshing = true)
            } else {
                State.Loading
            }
        }
        loadJob = viewModelScope.launch {
            try {
                val result = relatedEntriesFeature.load(entryId)
                if (generation == loadGeneration) {
                    when (result) {
                        is EntryRelatedEntriesLoadResult.Loaded -> state.update {
                            State.Success(
                                relatedEntries = result.entries.toImmutableList(),
                                sourceItemOrientation = result.orientation,
                                isRefreshing = false,
                            )
                        }
                        is EntryRelatedEntriesLoadResult.Unavailable -> state.update {
                            State.Unavailable(result.reason)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (generation == loadGeneration) {
                    state.update { State.Error(e) }
                }
            }
        }
    }

    override fun onCleared() {
        loadGeneration++
        loadJob?.cancel()
        super.onCleared()
    }

    @Immutable
    sealed interface State {
        data object Idle : State
        data object Loading : State

        data class Unavailable(
            val reason: EntryRelatedEntriesUnavailableReason,
        ) : State

        data class Success(
            val relatedEntries: ImmutableList<Entry>,
            val sourceItemOrientation: EntryItemOrientation,
            val isRefreshing: Boolean = false,
        ) : State

        data class Error(val throwable: Throwable) : State
    }
}
