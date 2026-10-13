package eu.kanade.tachiyomi.ui.entry.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.entry.EntryNotesScreen
import eu.kanade.presentation.util.Screen
import tachiyomi.domain.entry.model.Entry

class EntryNotesScreen(
    private val entry: Entry,
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = viewModel { EntryNotesViewModel(entry) }
        val state by viewModel.state.collectAsState()

        EntryNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = viewModel::updateNotes,
            onFinish = viewModel::finishEditing,
        )
    }
}
