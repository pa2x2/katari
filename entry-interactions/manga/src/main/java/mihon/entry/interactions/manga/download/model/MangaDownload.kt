package mihon.entry.interactions.manga.download.model

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.download.EntryDownloadMessage
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.time.Duration.Companion.milliseconds

internal data class MangaDownload(
    val source: UnifiedSource,
    val entry: Entry,
    val chapter: EntryChapter,
) {
    @Transient
    private val pagesFlow = MutableStateFlow<List<Page>?>(null)
    var pages: List<Page>?
        get() = pagesFlow.value
        set(pages) {
            pagesFlow.value = pages
        }
    var failure: EntryDownloadMessage? = null

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
    val progressFlow = pagesFlow
        .flatMapLatest { pages ->
            if (pages == null) {
                flowOf(0)
            } else {
                combine(pages.map(Page::progressFlow)) { it.average().toInt() }
            }
        }
        .distinctUntilChanged()
        .debounce(50.milliseconds)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }
}
