package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.common.utils.mutate
import mihon.entry.interactions.EntryMigrationAvailability
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationSelectionResult
import mihon.entry.interactions.EntryMigrationSubject
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateEntriesScreenModel(
    private val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
) : StateScreenModel<MigrateEntriesScreenModel.State>(State()) {

    private val _events: Channel<MigrationEntriesEvent> = Channel()
    val events: Flow<MigrationEntriesEvent> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(source = sourceManager.getOrStub(sourceId))
            }

            entryRepository.getFavoritesBySourceId(sourceId)
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationEntriesEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(entryList = listOf())
                    }
                }
                .map { entries ->
                    entries
                        .filter { migration.availability(it) is EntryMigrationAvailability.Available }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
                .collectLatest { list ->
                    mutableState.update { it.copy(entryList = list) }
                }
        }
    }

    fun toggleSelection(item: Entry) {
        mutableState.update { state ->
            val selection = state.selection.mutate { list ->
                if (!list.remove(item.id)) list.add(item.id)
            }
            state.copy(selection = selection)
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = emptySet()) }
    }

    fun migrationSelection(): List<EntryMigrationSubject> {
        val state = state.value
        val entries = state.entries.filter { it.id in state.selection }
        return (migration.prepareSelection(entries) as? EntryMigrationSelectionResult.Ready)?.subjects.orEmpty()
    }

    @Immutable
    data class State(
        val source: UnifiedSource? = null,
        val selection: Set<Long> = emptySet(),
        private val entryList: List<Entry>? = null,
    ) {

        val entries: List<Entry>
            get() = entryList ?: listOf()

        val isLoading: Boolean
            get() = source == null || entryList == null

        val isEmpty: Boolean
            get() = entries.isEmpty()

        val selectionMode = selection.isNotEmpty()
    }
}

sealed interface MigrationEntriesEvent {
    data object FailedFetchingFavorites : MigrationEntriesEvent
}
