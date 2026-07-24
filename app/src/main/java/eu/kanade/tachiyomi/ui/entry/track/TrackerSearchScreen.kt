package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackerSearch
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingSearchCandidate
import mihon.entry.interactions.EntryTrackingSearchResult
import mihon.entry.interactions.EntryTrackingServiceDescriptor
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class TrackerSearchScreen(
    private val entry: Entry,
    private val initialQuery: String,
    private val currentUrl: String?,
    private val service: EntryTrackingServiceDescriptor,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { Model(entry, currentUrl, initialQuery, service) }
        val state by screenModel.state.collectAsState()
        val textFieldState = rememberTextFieldState(initialQuery)

        TrackerSearch(
            state = textFieldState,
            onDispatchQuery = { screenModel.trackingSearch(textFieldState.text.toString()) },
            queryResult = state.queryResult,
            selected = state.selected,
            onSelectedChange = screenModel::updateSelection,
            onConfirmSelection = { private ->
                state.selected?.let { screenModel.registerTracking(it, private) }
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
            supportsPrivateTracking = service.capabilities.supportsPrivateTracking,
        )
    }

    private class Model(
        private val entry: Entry,
        private val currentUrl: String?,
        initialQuery: String,
        private val service: EntryTrackingServiceDescriptor,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : StateScreenModel<Model.State>(State()) {

        init {
            if (initialQuery.isNotBlank()) trackingSearch(initialQuery)
        }

        fun trackingSearch(query: String) {
            screenModelScope.launch {
                mutableState.update { it.copy(queryResult = null, selected = null) }
                val result = withIOContext { trackingFeature.search(entry, service.id, query) }
                val presentationResult = when (result) {
                    is EntryTrackingSearchResult.Found -> Result.success(result.candidates)
                    is EntryTrackingSearchResult.Failed -> Result.failure(result.cause)
                    is EntryTrackingSearchResult.Unavailable -> Result.failure(
                        IllegalStateException(result.reason.name),
                    )
                }
                mutableState.update { oldState ->
                    oldState.copy(
                        queryResult = presentationResult,
                        selected = presentationResult.getOrNull()?.find { it.remoteUrl == currentUrl },
                    )
                }
            }
        }

        fun registerTracking(candidate: EntryTrackingSearchCandidate, private: Boolean) {
            screenModelScope.launchNonCancellable {
                trackingFeature.register(entry, service.id, candidate, private).logFailure("registration")
            }
        }

        fun updateSelection(selected: EntryTrackingSearchCandidate) {
            mutableState.update { it.copy(selected = selected) }
        }

        @Immutable
        data class State(
            val queryResult: Result<List<EntryTrackingSearchCandidate>>? = null,
            val selected: EntryTrackingSearchCandidate? = null,
        )
    }
}
