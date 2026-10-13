package eu.kanade.tachiyomi.ui.entry.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.presentation.entry.EntryNotesScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class EntryNotesViewModel(
    private val entry: Entry,
    private val entryRepository: EntryRepository = Injekt.get(),
) : ViewModel() {

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
