package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTypography
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.LocalBookDocumentChapterSelection
import mihon.presentation.kit.progress.LoadingIndicator
import tachiyomi.domain.entry.model.EntryChapter

/** Measures rich rows on the UI thread; prose shaping and page assembly run in background preparation. */
@Composable
internal fun BookDocumentPaginationLayout(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    modifier: Modifier = Modifier,
    content: @Composable (List<BookDocumentPage>) -> Unit,
) {
    val textMeasurer = rememberBookDocumentPageTextMeasurer(items)
    val columnWidth = LocalBookDocumentTypography.current.columnWidth(LocalDensity.current)
    SubcomposeLayout(modifier) { constraints ->
        val pageHeight = constraints.maxHeight.coerceAtLeast(1)
        // Pages are measured at the width their text is drawn at: the window's, up to the reading column's
        val pageWidth = constraints.maxWidth.coerceAtMost(columnWidth.roundToPx())
        val rowConstraints = Constraints(minWidth = pageWidth, maxWidth = pageWidth)
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
                                )
                            }
                        }
                    }
                }.single().measure(rowConstraints).height
                put(item.key, height)
            }
        }
        val viewport = subcompose("viewport") {
            val pages = preparedBookDocumentPages(items, pageWidth, pageHeight, richHeights, textMeasurer)
            if (pages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            } else {
                content(pages)
            }
        }.single().measure(constraints)
        layout(constraints.maxWidth, pageHeight) { viewport.place(0, 0) }
    }
}
