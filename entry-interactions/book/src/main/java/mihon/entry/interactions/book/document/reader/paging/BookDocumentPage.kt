package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import tachiyomi.domain.entry.model.EntryChapter

/** A page contains canonical block ranges, never a persisted page number. */
internal data class BookDocumentPage(val fragments: List<BookDocumentPageFragment>, val scrollable: Boolean = false) {
    val key: String get() = fragments.first().key

    fun contains(sectionKey: String, position: BookDocumentPosition): Boolean = fragments.any { fragment ->
        val block = fragment.item as? BookDocumentViewerItem.Block ?: return@any false
        block.section.key == sectionKey && block.content.id == position.blockId &&
            position.offsetWithinBlock >= fragment.start &&
            (position.offsetWithinBlock < fragment.end || fragment.end == block.content.logicalLength)
    }
}

internal data class BookDocumentPageFragment(
    val item: BookDocumentViewerItem<EntryChapter>,
    val start: Int = 0,
    val end: Int = (item as? BookDocumentViewerItem.Block)?.content?.logicalLength ?: 0,
) {
    val key: String get() = "${item.key}:$start:$end"
    val section: BookDocumentSection<EntryChapter>? get() = (item as? BookDocumentViewerItem.Block)?.section
    val position: BookDocumentPosition? get() = (item as? BookDocumentViewerItem.Block)?.let {
        BookDocumentPosition(it.content.id, start)
    }
}
