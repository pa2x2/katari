package mihon.entry.interactions.manga.download.model

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.milliseconds

internal data class MangaDownload(
    val source: UnifiedSource,
    val entry: Entry,
    val chapter: EntryChapter,
) {
    var pages: List<Page>? = null

    val totalProgress: Int
        get() = pages?.sumOf(Page::progress) ?: 0

    val downloadedImages: Int
        get() = pages?.count { it.status == Page.State.Ready } ?: 0

    @Transient
    private val _statusFlow = MutableStateFlow(DownloadState.NOT_DOWNLOADED)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    var status: DownloadState
        get() = _statusFlow.value
        set(status) {
            _statusFlow.value = status
        }

    @Transient
    val progressFlow = flow {
        if (pages == null) {
            emit(0)
            while (pages == null) {
                delay(50.milliseconds)
            }
        }

        val progressFlows = pages!!.map(Page::progressFlow)
        emitAll(combine(progressFlows) { it.average().toInt() })
    }
        .distinctUntilChanged()
        .debounce(50.milliseconds)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    companion object {
        suspend fun fromChapterId(
            chapterId: Long,
            entryChapterRepository: EntryChapterRepository = Injekt.get(),
            getEntry: GetEntry = Injekt.get(),
            sourceManager: SourceManager = Injekt.get(),
        ): MangaDownload? {
            val entryChapter = entryChapterRepository.getChapterById(chapterId) ?: return null
            val entry = getEntry.await(entryChapter.entryId) ?: return null
            val source = sourceManager.get(entry.source) ?: return null

            return MangaDownload(source, entry, entryChapter)
        }
    }
}
