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
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingServiceDescriptor
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
        val screenModel = rememberScreenModel { Model(entry, service, displayScore) }
        val state by screenModel.state.collectAsState()
        TrackScoreSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            selections = service.capabilities.scores,
            onConfirm = {
                screenModel.setScore()
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
    ) : StateScreenModel<Model.State>(State(displayScore)) {

        fun setSelection(selection: String) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setScore() {
            val selection = state.value.selection
            screenModelScope.launchNonCancellable {
                trackingFeature.mutate(entry, service.id, EntryTrackingMutation.Score(selection))
                    .logFailure("score update")
            }
        }

        @Immutable
        data class State(val selection: String)
    }
}
