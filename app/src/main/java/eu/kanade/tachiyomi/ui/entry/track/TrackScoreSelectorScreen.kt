package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMutation
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackScoreSelectorScreen(
    private val entry: Entry,
    private val service: EntryTrackingServiceDescriptor,
    private val displayScore: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { Model(entry, service, displayScore) }
        val state by viewModel.state.collectAsState()
        TrackScoreSelector(
            selection = state.selection,
            onSelectionChange = viewModel::setSelection,
            selections = service.capabilities.scores,
            onConfirm = {
                viewModel.setScore()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val entry: Entry,
        private val service: EntryTrackingServiceDescriptor,
        displayScore: String,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : ViewModel() {

        val state: StateFlow<Model.State>
            field = MutableStateFlow<Model.State>(State(displayScore))

        fun setSelection(selection: String) {
            state.update { it.copy(selection = selection) }
        }

        fun setScore() {
            val selection = state.value.selection
            viewModelScope.launchNonCancellable {
                trackingFeature.mutate(entry, service.id, EntryTrackingMutation.Score(selection))
                    .logFailure("score update")
            }
        }

        @Immutable
        data class State(val selection: String)
    }
}
