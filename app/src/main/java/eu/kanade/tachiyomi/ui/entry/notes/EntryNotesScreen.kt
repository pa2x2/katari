package eu.kanade.tachiyomi.ui.entry.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.entry.EntryNotesScreen
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryNotesScreen(
    private val entry: Entry,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { Model(entry) }
        val state by screenModel.state.collectAsState()

        EntryNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = screenModel::updateNotes,
        )
    }

    private class Model(
        private val entry: Entry,
        private val entryRepository: EntryRepository = Injekt.get(),
    ) : StateScreenModel<EntryNotesScreen.State>(EntryNotesScreen.State(entry, entry.notes)) {

        fun updateNotes(content: String) {
            if (content == state.value.notes) return

            mutableState.update {
                it.copy(notes = content)
            }

            screenModelScope.launchNonCancellable {
                entryRepository.update(entry.copy(notes = content))
            }
        }
    }
}
