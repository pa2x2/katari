package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.track.TrackStatusSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingRecord
import mihon.entry.interactions.EntryTrackingServiceDescriptor
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
        val screenModel = rememberScreenModel { Model(entry, track, service) }
        val state by screenModel.state.collectAsState()
        TrackStatusSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            selections = screenModel.selections,
            onConfirm = {
                screenModel.setStatus()
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
    ) : StateScreenModel<Model.State>(State(track.status)) {

        val selections: Map<Long, StringResource?> = service.capabilities.statuses.associate { it.value to it.label }

        fun setSelection(selection: Long) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setStatus() {
            val selection = state.value.selection
            screenModelScope.launchNonCancellable {
                trackingFeature.mutate(entry, service.id, EntryTrackingMutation.Status(selection))
                    .logFailure("status update")
            }
        }

        @Immutable
        data class State(val selection: Long)
    }
}
