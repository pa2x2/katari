package mihon.entry.interactions.book.document.reader

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import mihon.entry.interactions.reader.navigation.EntryReaderNavigationPresentation
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.EntryChapter

internal data class BookDocumentReaderState(
    val entryTitle: String,
    val readingOrder: BookChapterReadingOrder,
    val navigationPresentation: EntryReaderNavigationPresentation = EntryReaderNavigationPresentation(
        chapters = readingOrder.chapters,
        progressLabels = emptyMap(),
    ),
    val currentChapterId: Long,
    val window: EntryChildWindow<EntryChapter>,
    /** Kept here rather than read from [readingOrder], whose chapters don't change while the reader is open. */
    val bookmarkedChapterIds: Set<Long> = emptySet(),
    /** Titles of the groups the chapters belong to, which transitions into another group name. */
    val chapterGroupTitles: Map<ChapterGroupId, String> = emptyMap(),
    val loadedSections: Map<Long, BookDocumentPublicationSections<EntryChapter>>,
    val publicationNavigation: Map<Long, List<BookNavigationItem>> = emptyMap(),
    val navigationLocator: BookLocator? = null,
    val loadStates: Map<Long, BookDocumentChapterLoadState> = emptyMap(),
    /** A chapter the reader asked to open that is still loading or failed to; null once it opens. */
    val chapterSelection: BookDocumentChapterSelectionLoad? = null,
    val chromeVisible: Boolean = false,
    val navigationVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val childWebView: EntryChildWebViewResolution.Available? = null,
    val navigationRequest: BookDocumentNavigationRequest? = null,
    val auxiliarySection: BookDocumentSection<EntryChapter>? = null,
)

internal sealed interface BookDocumentChapterLoadState {
    data object Loading : BookDocumentChapterLoadState

    data class Failed(val message: String) : BookDocumentChapterLoadState
}

internal data class BookDocumentChapterSelectionLoad(
    val chapter: EntryChapter,
    val loadState: BookDocumentChapterLoadState,
)
