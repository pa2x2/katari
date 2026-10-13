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
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.track.TrackStatusSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMutation
import mihon.entry.interactions.tracking.EntryTrackingRecord
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackStatusSelectorScreen(
    private val entry: Entry,
    private val track: EntryTrackingRecord,
    private val service: EntryTrackingServiceDescriptor,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { Model(entry, track, service) }
        val state by viewModel.state.collectAsState()
        TrackStatusSelector(
            selection = state.selection,
            onSelectionChange = viewModel::setSelection,
            selections = viewModel.selections,
            onConfirm = {
                viewModel.setStatus()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val entry: Entry,
        track: EntryTrackingRecord,
        private val service: EntryTrackingServiceDescriptor,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : ViewModel() {

        val state: StateFlow<Model.State>
            field = MutableStateFlow<Model.State>(State(track.status))

        val selections: Map<Long, StringResource?> = service.capabilities.statuses.associate { it.value to it.label }

        fun setSelection(selection: Long) {
            state.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            val selection = state.value.selection
            viewModelScope.launchNonCancellable {
                trackingFeature.mutate(entry, service.id, EntryTrackingMutation.Status(selection))
                    .logFailure("status update")
            }
        }

        @Immutable
        data class State(val selection: Long)
    }
}
