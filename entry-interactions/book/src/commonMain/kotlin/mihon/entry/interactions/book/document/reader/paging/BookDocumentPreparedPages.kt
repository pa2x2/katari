package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import tachiyomi.domain.entry.model.EntryChapter

/** Publishes complete page sets without removing the current viewport during preparation. */
@Composable
internal fun preparedBookDocumentPages(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    width: Int,
    height: Int,
    richHeights: Map<String, Int>,
    textMeasurer: BookDocumentPageTextMeasurer,
): List<BookDocumentPage> {
    val preparation = remember(textMeasurer, width, height) { BookDocumentPagePreparation(textMeasurer) }
    var pages by remember { mutableStateOf(emptyList<BookDocumentPage>()) }
    LaunchedEffect(preparation, items, richHeights) {
        pages = preparation.prepare(items, width, height, richHeights)
    }
    return pages
}
