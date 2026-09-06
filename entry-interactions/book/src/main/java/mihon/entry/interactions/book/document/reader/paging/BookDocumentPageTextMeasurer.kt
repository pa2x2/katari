package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import mihon.book.api.document.BookDocumentBlockKind
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.bookDocumentFontSize
import mihon.entry.interactions.book.document.reader.bookDocumentTextPresentation
import mihon.entry.interactions.book.document.reader.bookDocumentTextStyle
import mihon.entry.interactions.book.document.reader.toSelectableAnnotatedString
import tachiyomi.domain.entry.model.EntryChapter

/** Retains only geometry, without composing hidden selectable text for every candidate page break. */
internal class BookDocumentPageTextMeasurer(
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val textScale: Float,
    private val sectionFonts: Map<String, Map<String, FontFamily>>,
) {
    val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private var measuredWidth = -1
    private val measurements = mutableMapOf<String, Measurement>()

    fun retain(items: List<BookDocumentViewerItem<EntryChapter>>, width: Int) {
        if (measuredWidth != width) measurements.clear()
        measuredWidth = width
        val retained = items.associateBy { it.key }
        measurements.values.removeAll { retained[it.item.key] !== it.item }
    }

    fun measure(fragment: BookDocumentPageFragment): Pair<Int, List<Int>> {
        // Fragment/section data-class hashes traverse the entire publication. Geometry is owned
        // by the retained row identity and its source range, not a deep document hash.
        val key = "${fragment.key}:${fragment.firstOnPage}:${fragment.lastOnPage}"
        measurements[key]?.takeIf { it.item === fragment.item }?.let { return it.geometry }
        val geometry = measureText(fragment)
        measurements[key] = Measurement(fragment.item, geometry)
        return geometry
    }

    private fun measureText(fragment: BookDocumentPageFragment): Pair<Int, List<Int>> {
        val item = fragment.item as BookDocumentViewerItem.Block
        val block = fragment.renderedTextBlock()
        val source = item.section.document.document.content.text
        val text = source.substring(block.logicalStart, block.logicalEndExclusive)
        val trailingBreaks = text.length - text.trimEnd('\n').length
        val presentation = bookDocumentTextPresentation(text.dropLast(trailingBreaks), block.inlineStyles)
        val fonts = sectionFonts[item.section.key].orEmpty()
        val fontSize = bookDocumentFontSize(block, textScale, BOOK_DOCUMENT_BASE_TEXT_SIZE_SP)
        val annotated = presentation.toSelectableAnnotatedString(
            fonts,
            block.links,
            block.inlineStyles,
            "",
            fontSize.value,
            Color.Unspecified,
            {},
        )
        return with(density) {
            val authoredPadding = (block.style.paddingEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp
            val quotePadding = if (block.role.kind == BookDocumentBlockKind.QUOTE) 12.dp.roundToPx() else 0
            val width = (
                measuredWidth - BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING.roundToPx() * 2 -
                    authoredPadding.roundToPx() * 2 - quotePadding
                ).coerceAtLeast(0)
            val layout = textMeasurer.measure(
                text = annotated,
                style = bookDocumentTextStyle(block, fontSize, fonts),
                constraints = Constraints(maxWidth = width),
            )
            val preserveSpacing = !fragment.lastOnPage && item.content.id != item.section.document.blocks.last().id
            val terminalSpacing = if (preserveSpacing) {
                ((trailingBreaks - 1).coerceAtLeast(0) * 20 * textScale).dp.roundToPx()
            } else {
                0
            }
            val height = layout.size.height + 6.dp.roundToPx() * 2 + terminalSpacing +
                (authoredPadding + (block.style.spacingBeforeEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp)
                    .roundToPx() +
                (authoredPadding + (block.style.spacingAfterEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp)
                    .roundToPx()
            val skipped = block.logicalStart - item.content.logicalStart - fragment.start
            val ends = (0 until layout.lineCount).map { line ->
                val end = layout.getLineEnd(line)
                end - presentation.insertedOffsets.count { it < end }
            }.filter { it > 0 }.distinct().map { it + skipped }
            height to ends
        }
    }

    private class Measurement(
        val item: BookDocumentViewerItem<EntryChapter>,
        val geometry: Pair<Int, List<Int>>,
    )
}
