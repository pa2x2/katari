package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentBlockId
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Content-derived evidence that the current chapter has been read to its end.
 *
 * Chapter completion must never depend on the chapter-transition row: the row may render at any
 * size, fail to compose, or be hidden by presentation changes, while persisted progress keeps
 * working. The evidence is therefore the tail of the chapter's final loaded section, whether or
 * not another chapter follows it in the reading window.
 */
internal data class BookDocumentChapterEnd(
    val chapter: EntryChapter,
    val finalSectionKey: String,
    val finalBlockId: BookDocumentBlockId,
)

internal fun bookDocumentChapterEnd(
    window: EntryChildWindow<EntryChapter>,
    loadedSections: Map<Long, BookDocumentPublicationSections<EntryChapter>>,
): BookDocumentChapterEnd? {
    val lastSection = loadedSections[window.current.id]?.sections?.lastOrNull() ?: return null
    return BookDocumentChapterEnd(
        chapter = window.current,
        finalSectionKey = lastSection.key,
        finalBlockId = lastSection.document.blocks.last().id,
    )
}

/**
 * Whether a scrolled viewport shows the bottom edge of the chapter's final block. Scroll extent
 * is no evidence: a following chapter keeps the list scrollable past this chapter's end.
 */
internal fun <T> BookDocumentChapterEnd.isVisibleIn(
    items: List<BookDocumentViewerItem<T>>,
    visibleItems: List<BookDocumentVisibleItemLayout>,
    viewportEndOffset: Int,
): Boolean = visibleItems.any { layout ->
    val block = layout.resolveViewerItem(items) as? BookDocumentViewerItem.Block
    block != null &&
        block.section.key == finalSectionKey &&
        block.content.id == finalBlockId &&
        layout.offset + layout.size <= viewportEndOffset
}
