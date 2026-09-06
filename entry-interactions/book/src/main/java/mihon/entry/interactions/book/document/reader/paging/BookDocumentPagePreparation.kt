package mihon.entry.interactions.book.document.reader.paging

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import tachiyomi.domain.entry.model.EntryChapter

/** Calculates section pages off the UI thread and keeps unchanged neighbours when the window moves. */
internal class BookDocumentPagePreparation(private val textMeasurer: BookDocumentPageTextMeasurer) {
    private val sections = mutableMapOf<String, PreparedSection>()

    suspend fun prepare(
        items: List<BookDocumentViewerItem<EntryChapter>>,
        width: Int,
        height: Int,
        richHeights: Map<String, Int>,
    ): List<BookDocumentPage> = withContext(textMeasurer.dispatcher) {
        textMeasurer.retain(items, width)
        val groups = items.groupBy { it.paginationGroup() }
        sections.keys.retainAll(groups.keys)
        val context = currentCoroutineContext()
        groups.flatMap { (key, group) ->
            context.ensureActive()
            val groupHeights = group.mapNotNull { item -> richHeights[item.key]?.let { item.key to it } }.toMap()
            val cached = sections[key]
            if (cached != null && cached.items.size == group.size &&
                cached.items.indices.all { cached.items[it] === group[it] } && cached.richHeights == groupHeights
            ) {
                cached.pages
            } else {
                val pages = assembleBookDocumentPages(group, height) { fragment ->
                    context.ensureActive()
                    val block = fragment.item as? BookDocumentViewerItem.Block
                    if (block?.content?.content is BookDocumentBlockContent.Text) {
                        textMeasurer.measure(fragment)
                    } else {
                        requireNotNull(richHeights[fragment.item.key]) to emptyList()
                    }
                }
                sections[key] = PreparedSection(group, groupHeights, pages)
                pages
            }
        }
    }

    private class PreparedSection(
        val items: List<BookDocumentViewerItem<EntryChapter>>,
        val richHeights: Map<String, Int>,
        val pages: List<BookDocumentPage>,
    )
}
