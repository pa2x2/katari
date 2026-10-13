package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.source.EntryDeepLinkFeature
import mihon.entry.interactions.source.EntryDeepLinkResolution
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkViewModel(
    query: String = "",
    private val deepLinkFeature: EntryDeepLinkFeature = Injekt.get(),
) : ViewModel() {

    val state: StateFlow<DeepLinkViewModel.State>
        field = MutableStateFlow<DeepLinkViewModel.State>(State.Loading)

    init {
        viewModelScope.launchIO {
            val resolution = deepLinkFeature.resolve(query)
            if (resolution is EntryDeepLinkResolution.Failed) {
                logcat(LogPriority.ERROR, resolution.cause) { "Failed to resolve deep link" }
            }
            state.update {
                when (resolution) {
                    is EntryDeepLinkResolution.Resolved -> State.Result(resolution.entry, resolution.childId)
                    EntryDeepLinkResolution.NoMatch,
                    is EntryDeepLinkResolution.Failed,
                    -> State.NoResults
                }
            }
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class Result(val entry: Entry, val chapterId: Long? = null) : State
    }
}
