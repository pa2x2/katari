package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.saveable.listSaver
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentPosition
import tachiyomi.domain.entry.model.EntryChapter

/** Saves the first visible passage for mode changes and recreation, independently of reading progress. */
internal class BookDocumentViewportAnchor(private var restored: List<Any> = emptyList()) {
    var location: BookDocumentViewerLocation<EntryChapter>? = null

    fun resolve(sections: Map<Long, BookDocumentPublicationSections<EntryChapter>>) {
        if (location != null || restored.isEmpty()) return
        val section = sections[restored[0] as Long]?.sections?.firstOrNull { it.key == restored[1] } ?: return
        val position = BookDocumentPosition(BookDocumentBlockId(restored[2] as String), restored[3] as Int)
        if (section.document.document.contains(position)) {
            location = BookDocumentViewerLocation(section, position, section.document.document.progressionAt(position))
        }
        restored = emptyList()
    }

    companion object {
        val Saver = listSaver<BookDocumentViewportAnchor, Any>(
            save = { anchor ->
                anchor.location?.let { location ->
                    listOf(
                        location.section.owner.id,
                        location.section.key,
                        location.position.blockId.value,
                        location.position.offsetWithinBlock,
                    )
                } ?: anchor.restored
            },
            restore = { BookDocumentViewportAnchor(it) },
        )
    }
}
