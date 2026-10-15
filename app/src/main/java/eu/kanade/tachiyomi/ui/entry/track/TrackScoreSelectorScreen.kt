package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMutation
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry

internal data class TrackScoreSelectorScreen(
    private val entry: Entry,
    private val service: EntryTrackingServiceDescriptor,
    private val displayScore: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(entry, service, displayScore) }
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

    @AssistedInject
    class Model(
        @Assisted private val entry: Entry,
        @Assisted private val service: EntryTrackingServiceDescriptor,
        @Assisted displayScore: String,
        private val trackingFeature: EntryTrackingFeature,
    ) : ViewModel() {

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppUiScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(entry: Entry, service: EntryTrackingServiceDescriptor, displayScore: String): Model
        }

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
