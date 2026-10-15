package eu.kanade.tachiyomi.ui.deeplink

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.source.EntryDeepLinkFeature
import mihon.entry.interactions.source.EntryDeepLinkResolution
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

@AssistedInject
class DeepLinkViewModel(
    @Assisted query: String,
    private val deepLinkFeature: EntryDeepLinkFeature,
    private val entryOpenFeature: EntryOpenFeature,
    private val chapterRepository: EntryChapterRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(query: String): DeepLinkViewModel
    }

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

    fun canOpen(entry: Entry): Boolean = entryOpenFeature.isApplicable(entry.type)

    suspend fun getChapter(chapterId: Long): EntryChapter? = chapterRepository.getChapterById(chapterId)

    fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entryOpenFeature.open(context, entry, chapter)
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
