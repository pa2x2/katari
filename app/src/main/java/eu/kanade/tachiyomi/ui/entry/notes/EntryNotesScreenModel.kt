package eu.kanade.tachiyomi.ui.entry.notes

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.entry.EntryNotesScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class EntryNotesScreenModel(
    private val entry: Entry,
    private val entryRepository: EntryRepository = Injekt.get(),
) : StateScreenModel<EntryNotesScreen.State>(EntryNotesScreen.State(entry, entry.notes)) {

    private val pendingNotes = Channel<String>(Channel.CONFLATED)

    init {
        screenModelScope.launchNonCancellable {
            for (notes in pendingNotes) {
                entryRepository.updateNotes(entry.id, entry.profileId, notes)
            }
        }
    }

    fun updateNotes(content: String) {
        if (content == state.value.notes) return

        mutableState.update {
            it.copy(notes = content)
        }
        pendingNotes.trySend(content)
    }

    fun finishEditing(content: String) {
        updateNotes(content)
        pendingNotes.close()
    }
}
