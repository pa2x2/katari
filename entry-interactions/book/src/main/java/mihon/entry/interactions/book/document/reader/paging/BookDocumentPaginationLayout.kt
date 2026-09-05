package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.LocalBookDocumentChapterSelection
import tachiyomi.domain.entry.model.EntryChapter

/** Measures the actual renderer before exposing pages, keeping font/style and line wrapping identical. */
@Composable
internal fun BookDocumentPaginationLayout(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    modifier: Modifier = Modifier,
    content: @Composable (List<BookDocumentPage>) -> Unit,
) {
    val probes = remember { mutableMapOf<String, BookDocumentPageMeasurement>() }
    SubcomposeLayout(modifier) { constraints ->
        val pageHeight = constraints.maxHeight.coerceAtLeast(1)
        val rowConstraints = Constraints(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth)
        val measured = mutableMapOf<String, Pair<Int, List<Int>>>()
        fun measure(fragment: BookDocumentPageFragment): Pair<Int, List<Int>> {
            val measurementKey = "${fragment.key}:${fragment.firstOnPage}:${fragment.lastOnPage}"
            return measured.getOrPut(measurementKey) {
                val probe = probes.getOrPut(measurementKey) { BookDocumentPageMeasurement() }
                val placeable = subcompose(measurementKey) {
                    CompositionLocalProvider(
                        LocalBookDocumentChapterSelection provides null,
                        LocalBookDocumentPageMeasurement provides probe,
                    ) {
                        DisableSelection {
                            Box(Modifier.clearAndSetSemantics {}) {
                                BookDocumentPageFragmentContent(fragment, emptyMap(), { _, _ -> }, {}, {}, {})
                            }
                        }
                    }
                }.single().measure(rowConstraints)
                val item = fragment.item as? BookDocumentViewerItem.Block
                val skipped = if (item?.content?.content is BookDocumentBlockContent.Text) {
                    fragment.renderedTextBlock().logicalStart - item.content.logicalStart - fragment.start
                } else {
                    0
                }
                placeable.height to probe.lineEnds.map { it + skipped }
            }
        }
        val pages = assembleBookDocumentPages(items, pageHeight, ::measure)
        probes.keys.retainAll(measured.keys)
        val viewport = subcompose("viewport") { content(pages) }.single().measure(constraints)
        layout(constraints.maxWidth, pageHeight) { viewport.place(0, 0) }
    }
}
