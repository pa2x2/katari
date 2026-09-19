package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentChapterEnd
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import tachiyomi.domain.entry.model.EntryChapter

/** A page contains canonical block ranges, never a persisted page number. */
internal data class BookDocumentPage(val fragments: List<BookDocumentPageFragment>, val scrollable: Boolean = false) {
    val key: String get() = fragments.first().key

    /**
     * The chapter whose reading position this page reports. Content pages report their own
     * section; a transition page leads from its origin chapter, so progress display and
     * chapter-end evidence stay anchored to content without inspecting the transition row.
     */
    val ownerChapter: EntryChapter
        get() = when (val item = fragments.first().item) {
            is BookDocumentViewerItem.Block -> item.section.owner
            is BookDocumentViewerItem.Transition -> item.transition.from
        }

    fun contains(sectionKey: String, position: BookDocumentPosition): Boolean = fragments.any { fragment ->
        val block = fragment.item as? BookDocumentViewerItem.Block ?: return@any false
        block.section.key == sectionKey && block.content.id == position.blockId &&
            position.offsetWithinBlock >= fragment.start &&
            (position.offsetWithinBlock < fragment.end || fragment.end == block.content.logicalLength)
    }
}

/**
 * Whether this settled page is where [end]'s chapter content stream ends: it renders the tail of
 * the chapter's final block. The check is stream-relative — it never inspects the pagination
 * window's extent or the chapter-transition row.
 */
internal fun BookDocumentPage.endsChapterContent(end: BookDocumentChapterEnd): Boolean =
    fragments.any { fragment ->
        val block = fragment.item as? BookDocumentViewerItem.Block ?: return@any false
        block.section.key == end.finalSectionKey && block.content.id == end.finalBlockId &&
            fragment.end == block.content.logicalLength
    }

internal data class BookDocumentPageFragment(
    val item: BookDocumentViewerItem<EntryChapter>,
    val start: Int = 0,
    val end: Int = (item as? BookDocumentViewerItem.Block)?.content?.logicalLength ?: 0,
    val firstOnPage: Boolean = false,
    val lastOnPage: Boolean = false,
) {
    val key: String get() = "${item.key}:$start:$end"
    val section: BookDocumentSection<EntryChapter>? get() = (item as? BookDocumentViewerItem.Block)?.section
    val position: BookDocumentPosition? get() = (item as? BookDocumentViewerItem.Block)?.let {
        BookDocumentPosition(it.content.id, start)
    }
}
