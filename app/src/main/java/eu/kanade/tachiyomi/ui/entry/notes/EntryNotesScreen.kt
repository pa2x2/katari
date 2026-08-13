package eu.kanade.tachiyomi.ui.entry.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
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

        val screenModel = rememberScreenModel { EntryNotesScreenModel(entry) }
        val state by screenModel.state.collectAsState()

        EntryNotesScreen(
            state = state,
            navigateUp = navigator::pop,
            onUpdate = screenModel::updateNotes,
            onFinish = screenModel::finishEditing,
        )
    }
}
