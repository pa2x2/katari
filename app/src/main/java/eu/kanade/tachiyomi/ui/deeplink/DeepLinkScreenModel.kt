package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.EntryDeepLinkFeature
import mihon.entry.interactions.EntryDeepLinkResolution
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreenModel(
    query: String = "",
    private val deepLinkFeature: EntryDeepLinkFeature = Injekt.get(),
) : StateScreenModel<DeepLinkScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            val resolution = deepLinkFeature.resolve(query)
            if (resolution is EntryDeepLinkResolution.Failed) {
                logcat(LogPriority.ERROR, resolution.cause) { "Failed to resolve deep link" }
            }
            mutableState.update {
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
