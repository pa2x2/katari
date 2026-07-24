package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackChapterSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingRecord
import mihon.entry.interactions.EntryTrackingServiceId
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackProgressSelectorScreen(
    private val entry: Entry,
    private val track: EntryTrackingRecord,
    private val serviceId: EntryTrackingServiceId,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { Model(entry, track, serviceId) }
        val state by screenModel.state.collectAsState()
        TrackChapterSelector(
            entryType = entry.type,
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            range = remember(track) { 0..if (track.total > 0) track.total.toInt() else 10000 },
            onConfirm = {
                screenModel.setProgress()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val entry: Entry,
        track: EntryTrackingRecord,
        private val serviceId: EntryTrackingServiceId,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : StateScreenModel<Model.State>(State(track.progress.toInt())) {

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setProgress() {
            val selection = state.value.selection
            screenModelScope.launchNonCancellable {
                trackingFeature.mutate(entry, serviceId, EntryTrackingMutation.Progress(selection))
                    .logFailure("progress update")
            }
        }

        @Immutable
        data class State(val selection: Int)
    }
}
