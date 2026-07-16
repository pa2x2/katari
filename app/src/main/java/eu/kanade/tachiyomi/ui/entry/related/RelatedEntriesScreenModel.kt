package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetRelatedEntries
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedEntriesScreenModel(
    private val entryId: Long,
    private val getEntry: GetEntry = Injekt.get(),
    private val getRelatedEntries: GetRelatedEntries = Injekt.get(),
) : StateScreenModel<RelatedEntriesScreenModel.State>(State.Idle) {

    private var loadJob: Job? = null
    private var loadGeneration = 0L

    @Composable
    fun getEntryState(initialEntry: Entry): androidx.compose.runtime.State<Entry> {
        return produceState(initialValue = initialEntry) {
            getEntry.subscribe(initialEntry.url, initialEntry.source, initialEntry.type)
                .filterNotNull()
                .collectLatest { entry -> value = entry }
        }
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
        mutableState.update { currentState ->
            if (retainSuccess && currentState is State.Success) {
                currentState.copy(isRefreshing = true)
            } else {
                State.Loading
            }
        }
        loadJob = screenModelScope.launch {
            try {
                val entry = getEntry.await(entryId)
                    ?: error("Entry $entryId was not found")
                val relatedEntries = getRelatedEntries.await(entry)
                if (generation == loadGeneration) {
                    mutableState.update {
                        State.Success(
                            entry = entry,
                            relatedEntries = relatedEntries.toImmutableList(),
                            isRefreshing = false,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (generation == loadGeneration) {
                    mutableState.update { State.Error(e) }
                }
            }
        }
    }

    override fun onDispose() {
        loadGeneration++
        loadJob?.cancel()
        super.onDispose()
    }

    @Immutable
    sealed interface State {
        data object Idle : State
        data object Loading : State

        data class Success(
            val entry: Entry,
            val relatedEntries: ImmutableList<Entry>,
            val isRefreshing: Boolean = false,
        ) : State

        data class Error(val throwable: Throwable) : State
    }
}
