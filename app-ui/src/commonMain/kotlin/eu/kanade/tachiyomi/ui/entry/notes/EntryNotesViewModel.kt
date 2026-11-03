package eu.kanade.tachiyomi.ui.entry.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.presentation.entry.EntryNotesScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

@AssistedInject
internal class EntryNotesViewModel(
    @Assisted private val entry: Entry,
    private val entryRepository: EntryRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entry: Entry): EntryNotesViewModel
    }

    val state: StateFlow<EntryNotesScreen.State>
        field = MutableStateFlow<EntryNotesScreen.State>(EntryNotesScreen.State(entry, entry.notes))

    private val pendingNotes = Channel<String>(Channel.CONFLATED)

    init {
        viewModelScope.launchNonCancellable {
            for (notes in pendingNotes) {
                entryRepository.updateNotes(entry.id, entry.profileId, notes)
            }
        }
    }

    fun updateNotes(content: String) {
        if (content == state.value.notes) return

        state.update {
            it.copy(notes = content)
        }
        pendingNotes.trySend(content)
    }

    fun finishEditing(content: String) {
        updateNotes(content)
        pendingNotes.close()
    }
}
