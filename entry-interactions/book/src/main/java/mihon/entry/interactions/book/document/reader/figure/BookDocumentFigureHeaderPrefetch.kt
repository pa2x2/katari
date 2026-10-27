package mihon.entry.interactions.book.document.reader.figure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import mihon.entry.interactions.book.document.preparation.resourceRequirements
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.resource.BookDocumentImageHeaders
import mihon.entry.interactions.book.preparation.BookResourceContentKind

/**
 * Reads the image headers of every loaded section in the background, so a figure scrolled into view from above
 * for the first time, such as after reopening the book below it, already has its height.
 */
@Composable
internal fun PrefetchBookDocumentFigureHeaders(sections: List<BookDocumentSection<*>>) {
    LaunchedEffect(sections) {
        sections.forEach { section ->
            val loader = section.resourceLoader ?: return@forEach
            section.document.document.resourceRequirements()
                .filterValues { it.contentKind == BookResourceContentKind.DOCUMENT_IMAGE }
                .keys
                .forEach { resourceId -> BookDocumentImageHeaders.read(loader, resourceId) }
        }
    }
}
