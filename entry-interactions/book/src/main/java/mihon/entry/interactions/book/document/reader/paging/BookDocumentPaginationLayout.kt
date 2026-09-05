package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.LocalBookDocumentChapterSelection
import tachiyomi.domain.entry.model.EntryChapter

/** Measures rich rows on the UI thread; prose shaping and page assembly run in background preparation. */
@Composable
internal fun BookDocumentPaginationLayout(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    modifier: Modifier = Modifier,
    content: @Composable (List<BookDocumentPage>) -> Unit,
) {
    val textMeasurer = rememberBookDocumentPageTextMeasurer(items)
    SubcomposeLayout(modifier) { constraints ->
        val pageHeight = constraints.maxHeight.coerceAtLeast(1)
        val rowConstraints = Constraints(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth)
        val richHeights = buildMap {
            items.filterIsInstance<BookDocumentViewerItem.Block<EntryChapter>>().forEach { item ->
                when (item.content.content) {
                    is BookDocumentBlockContent.Text,
                    is BookDocumentBlockContent.Disclosure,
                    is BookDocumentBlockContent.Figure,
                    is BookDocumentBlockContent.Table,
                    -> return@forEach
                    else -> Unit
                }
                val height = subcompose(item.key) {
                    CompositionLocalProvider(LocalBookDocumentChapterSelection provides null) {
                        DisableSelection {
                            Box(Modifier.clearAndSetSemantics {}) {
                                BookDocumentPageFragmentContent(
                                    BookDocumentPageFragment(item),
                                    emptyMap(),
                                    { _, _ -> },
                                    {},
                                    {},
                                    {},
                                )
                            }
                        }
                    }
                }.single().measure(rowConstraints).height
                put(item.key, height)
            }
        }
        val viewport = subcompose("viewport") {
            val pages = preparedBookDocumentPages(items, constraints.maxWidth, pageHeight, richHeights, textMeasurer)
            if (pages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                content(pages)
            }
        }.single().measure(constraints)
        layout(constraints.maxWidth, pageHeight) { viewport.place(0, 0) }
    }
}
