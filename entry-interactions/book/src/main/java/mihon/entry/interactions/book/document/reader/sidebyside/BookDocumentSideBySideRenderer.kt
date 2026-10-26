package mihon.entry.interactions.book.document.reader.sidebyside

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBox
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentGridTrack
import mihon.book.api.document.BookDocumentItemAlignment
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentSideBySideItem
import mihon.book.api.document.BookDocumentSideBySideLayout
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.BookDocumentBlockRenderer
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxEdges
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxIndex
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxSlice
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxDecoration
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxes
import mihon.entry.interactions.book.document.reader.box.frame
import mihon.entry.interactions.book.document.reader.box.margins
import mihon.entry.interactions.book.document.reader.box.textBackdrop
import mihon.entry.interactions.book.document.reader.measureBookDocumentText
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentTextBackdrop
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/**
 * Lays the items of a flex row or grid next to each other, each a column of its own blocks.
 *
 * Items hold figures and tables, which can't answer intrinsic size queries, so the row asks none: an item's
 * content width is estimated from its text laid out on one line, each item is measured once at its width, and an
 * item's own box is a separate layer sized afterwards to the line, so the cards of a line share one height.
 */
@Composable
internal fun BookDocumentSideBySideRenderer(
    content: BookDocumentBlockContent.SideBySide,
    selectionIdentity: String,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    val styling = LocalBookDocumentAuthoredStyling.current
    val textScale = LocalBookDocumentTextScale.current
    val emDp = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale
    val layout = content.layout
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    val itemBoxes = remember(content, styling.enabled) {
        content.items.map { item -> if (styling.enabled) item.body.itemBox() else null }
    }
    val contentWidths = remember(content, textScale, itemBoxes) { mutableMapOf<Int, Int>() }
    Layout(
        contents = listOf(
            {
                content.items.forEachIndexed { index, item ->
                    SideBySideItem(
                        item = item,
                        itemBox = itemBoxes[index],
                        selectionIdentity = "$selectionIdentity:item:$index",
                        sectionKey = sectionKey,
                        resourceLoader = resourceLoader,
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        emDp = emDp,
                    )
                }
            },
            {
                itemBoxes.forEach { box ->
                    Box(if (box != null) Modifier.bookDocumentBoxDecoration(box.style, styling) else Modifier)
                }
            },
        ),
    ) { (items, decorations), constraints ->
        val available = constraints.maxWidth
        val emPx = emDp.dp.toPx()
        val columnGap = (layout.columnGapEm * emPx).toInt()
        val rowGap = (layout.rowGapEm * emPx).toInt()
        fun contentWidth(index: Int): Int = contentWidths.getOrPut(index) {
            estimatedContentWidth(content.items[index], itemBoxes[index], textMeasurer, textScale, emDp, available)
        }.coerceAtMost(available)
        val placements = when (layout) {
            is BookDocumentSideBySideLayout.Flex -> placeFlexItems(
                items = content.items.mapIndexed { index, item ->
                    val sizing = item.sizing
                    val basis = sizing.basisEm?.let { (it * emPx).toInt() }
                        ?: sizing.basisFraction?.let { (it * available).toInt() }
                        ?: contentWidth(index)
                    BookDocumentFlexItem(
                        basis = basis.coerceAtMost(available),
                        grow = sizing.grow,
                        shrink = sizing.shrink,
                        minWidth = 0,
                    )
                },
                available = available,
                gap = columnGap,
                wrap = layout.wrap,
                reversed = layout.reversed,
                justify = layout.justify,
            )
            is BookDocumentSideBySideLayout.Grid -> placeGridItems(
                items = content.items.mapIndexed { index, item ->
                    val auto = layout.columns.any { it == BookDocumentGridTrack.Auto }
                    BookDocumentGridItem(
                        column = item.sizing.column,
                        span = item.sizing.columnSpan,
                        contentWidth = if (auto) contentWidth(index) else 0,
                    )
                },
                tracks = layout.columns,
                autoFillMin = layout.autoFillMinEm?.let { (it * emPx).toInt() },
                emPx = emPx,
                available = available,
                gap = columnGap,
            )
        }
        val placeables = placements.mapIndexed { index, placement ->
            items[index].measure(Constraints(minWidth = placement.width, maxWidth = placement.width))
        }
        val lineHeights = IntArray(placements.maxOf { it.line } + 1)
        placements.forEachIndexed { index, placement ->
            lineHeights[placement.line] = maxOf(lineHeights[placement.line], placeables[index].height)
        }
        val lineTops = lineHeights.toList().runningFold(0) { top, height -> top + height + rowGap }
        val stretch = layout.alignItems == BookDocumentItemAlignment.STRETCH
        val boxes = placements.mapIndexed { index, placement ->
            val box = itemBoxes[index] ?: return@mapIndexed null
            val margins = BookDocumentBoxSlice(box.style, opens = true, closes = true).margins(emDp).toPx(this)
            val height = if (stretch) lineHeights[placement.line] else placeables[index].height
            decorations[index].measure(
                Constraints.fixed(
                    (placement.width - margins.left - margins.right).coerceAtLeast(0),
                    (height - margins.top - margins.bottom).coerceAtLeast(0),
                ),
            ) to margins
        }
        layout(available, (lineTops.last() - rowGap).coerceAtLeast(0)) {
            placements.forEachIndexed { index, placement ->
                val free = lineHeights[placement.line] - placeables[index].height
                val offset = when (layout.alignItems) {
                    BookDocumentItemAlignment.CENTER -> free / 2
                    BookDocumentItemAlignment.END -> free
                    BookDocumentItemAlignment.START, BookDocumentItemAlignment.STRETCH -> 0
                }
                val top = lineTops[placement.line] + offset
                boxes[index]?.let { (decoration, margins) ->
                    decoration.placeRelative(placement.x + margins.left, top + margins.top)
                }
                placeables[index].placeRelative(placement.x, top)
            }
        }
    }
}

/** The box around every block of an item: the item's own, drawn by the row at the height of the line. */
private fun BookDocumentContent.itemBox(): BookDocumentBox? =
    boxes.firstOrNull { it.firstBlockIndex == 0 && it.lastBlockIndex == blocks.lastIndex }

@Composable
private fun SideBySideItem(
    item: BookDocumentSideBySideItem,
    itemBox: BookDocumentBox?,
    selectionIdentity: String,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    emDp: Float,
) {
    val styling = LocalBookDocumentAuthoredStyling.current
    val body = item.body
    val boxIndex =
        remember(body, itemBox) { BookDocumentBoxIndex(body.blocks.size, body.boxes - setOfNotNull(itemBox)) }
    val itemSlice = itemBox?.let { BookDocumentBoxSlice(it.style, opens = true, closes = true) }
    val inset = itemSlice?.let { slice -> slice.margins(emDp) + slice.frame(emDp) }
    Column(if (inset != null) Modifier.padding(inset.left, inset.top, inset.right, inset.bottom) else Modifier) {
        body.blocks.forEachIndexed { index, nested ->
            val slices = if (styling.enabled) boxIndex.slices(index) else emptyList()
            CompositionLocalProvider(
                LocalBookDocumentTextBackdrop provides (listOfNotNull(itemSlice) + slices).textBackdrop(styling),
            ) {
                BookDocumentBlockRenderer(
                    block = nested,
                    owningContent = body,
                    sectionKey = sectionKey,
                    resourceLoader = resourceLoader,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    selectionIdentity = "$selectionIdentity:$index:${nested.id.value}",
                    preserveTerminalSpacing = index != body.blocks.lastIndex,
                    modifier = Modifier.bookDocumentBoxes(slices, styling, emDp),
                )
            }
        }
    }
}

// The width the item's text takes on one line, or an image's own width, plus the item's box; a block whose width
// can't be told, such as a table, wants the whole row.
private fun Density.estimatedContentWidth(
    item: BookDocumentSideBySideItem,
    itemBox: BookDocumentBox?,
    textMeasurer: TextMeasurer,
    textScale: Float,
    emDp: Float,
    available: Int,
): Int {
    val body = item.body
    val widest = body.blocks.maxOf { block ->
        when (val blockContent = block.content) {
            is BookDocumentBlockContent.Text -> textMeasurer.measureBookDocumentText(
                block = block,
                blockText = body.text.substring(block.logicalStart, block.logicalEndExclusive),
                fonts = emptyMap(),
                textScale = textScale,
                width = Constraints.Infinity,
            ).layout.size.width
            is BookDocumentBlockContent.Figure -> blockContent.image.width?.dp?.roundToPx() ?: available
            else -> available
        }
    }
    val inset = itemBox?.let { box ->
        val slice = BookDocumentBoxSlice(box.style, opens = true, closes = true)
        (slice.margins(emDp) + slice.frame(emDp)).let { it.left.roundToPx() + it.right.roundToPx() }
    } ?: 0
    // Rounding in the estimate must not wrap the last word of a line that fits.
    return widest + inset + 1
}

private operator fun BookDocumentBoxEdges.plus(other: BookDocumentBoxEdges) =
    BookDocumentBoxEdges(left + other.left, top + other.top, right + other.right, bottom + other.bottom)

private class PxEdges(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun BookDocumentBoxEdges.toPx(density: Density) = with(density) {
    PxEdges(left.roundToPx(), top.roundToPx(), right.roundToPx(), bottom.roundToPx())
}
