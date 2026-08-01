package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

internal data class BookDocumentReaderState(
    val entryTitle: String,
    val chapters: List<EntryChapter>,
    val currentChapterId: Long,
    val window: EntryChildWindow<EntryChapter>,
    val loadedSections: Map<Long, BookDocumentSection<EntryChapter>>,
    val loadStates: Map<Long, BookDocumentChapterLoadState> = emptyMap(),
    val chapterProgression: Float = 0f,
    val chromeVisible: Boolean = false,
    val navigationVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val childWebView: EntryChildWebViewResolution.Available? = null,
    val navigationRequest: BookDocumentNavigationRequest? = null,
)

internal sealed interface BookDocumentChapterLoadState {
    data object Loading : BookDocumentChapterLoadState

    data class Failed(val message: String) : BookDocumentChapterLoadState
}
