package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.EntryUriType
import eu.kanade.tachiyomi.source.entry.ResolvableSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreenModel(
    query: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val syncEntryWithSource: SyncEntryWithSource = Injekt.get(),
) : StateScreenModel<DeepLinkScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            val source = sourceManager.getAll()
                .filterIsInstance<ResolvableSource>()
                .firstOrNull { it.getUriType(query) != EntryUriType.Unknown }

            val entry = source?.getEntry(query)?.let {
                networkToLocalEntry(it.toEntry(source.id))
            }

            val chapter = if (source?.getUriType(query) == EntryUriType.Chapter && entry != null) {
                source.getChapter(query)?.let { getChapterFromSEntryChapter(it, entry) }
            } else {
                null
            }

            mutableState.update {
                if (entry == null) {
                    State.NoResults
                } else {
                    if (chapter == null) {
                        State.Result(entry)
                    } else {
                        State.Result(entry, chapter.id)
                    }
                }
            }
        }
    }

    private suspend fun getChapterFromSEntryChapter(
        sChapter: SEntryChapter,
        entry: Entry,
    ): EntryChapter? {
        val localChapter = entryChapterRepository.getChapterByUrlAndEntryId(sChapter.url, entry.id)

        return localChapter
            ?: syncEntryWithSource(entry)
                .insertedChapters
                .find { it.url == sChapter.url }
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
