package mihon.entry.interactions.book.document.reader.sidebyside

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentItemAlignment
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentSideBySideItem
import mihon.book.api.document.BookDocumentSideBySideLayout
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.BookDocumentBlockRenderer
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxIndex
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxSlice
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxes
import mihon.entry.interactions.book.document.reader.box.textBackdrop
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentTextBackdrop
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Lays the items of a flex row or grid next to each other, each a column of its own blocks. */
@Composable
internal fun BookDocumentSideBySideRenderer(
    content: BookDocumentBlockContent.SideBySide,
    block: BookDocumentBlock,
    selectionIdentity: String,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    val emDp = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * LocalBookDocumentTextScale.current
    val layout = content.layout
    Layout(
        contents = content.items.mapIndexed { index, item ->
            {
                SideBySideItem(
                    item = item,
                    selectionIdentity = "$selectionIdentity:item:$index",
                    sectionKey = sectionKey,
                    resourceLoader = resourceLoader,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    emDp = emDp,
                )
            }
        },
    ) { measurables, constraints ->
        val roots = measurables.map(List<Measurable>::single)
        val available = constraints.maxWidth
        val emPx = emDp.dp.toPx()
        val columnGap = (layout.columnGapEm * emPx).toInt()
        val rowGap = (layout.rowGapEm * emPx).toInt()
        val placements = when (layout) {
            is BookDocumentSideBySideLayout.Flex -> placeFlexItems(
                items = content.items.mapIndexed { index, item ->
                    val sizing = item.sizing
                    val basis = sizing.basisEm?.let { (it * emPx).toInt() }
                        ?: sizing.basisFraction?.let { (it * available).toInt() }
                        ?: roots[index].maxIntrinsicWidth(Constraints.Infinity)
                    BookDocumentFlexItem(
                        basis = basis.coerceAtMost(available),
                        grow = sizing.grow,
                        shrink = sizing.shrink,
                        minWidth = roots[index].minIntrinsicWidth(Constraints.Infinity).coerceAtMost(available),
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
                    BookDocumentGridItem(
                        column = item.sizing.column,
                        span = item.sizing.columnSpan,
                        contentWidth = roots[index].maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(available),
                    )
                },
                tracks = layout.columns,
                autoFillMin = layout.autoFillMinEm?.let { (it * emPx).toInt() },
                emPx = emPx,
                available = available,
                gap = columnGap,
            )
        }
        val lineCount = placements.maxOf { it.line } + 1
        val lineHeights = IntArray(lineCount)
        placements.forEachIndexed { index, placement ->
            val height = roots[index].maxIntrinsicHeight(placement.width)
            lineHeights[placement.line] = maxOf(lineHeights[placement.line], height)
        }
        val lineTops = lineHeights.toList().runningFold(0) { top, height -> top + height + rowGap }
        val placeables = placements.mapIndexed { index, placement ->
            val lineHeight = lineHeights[placement.line]
            roots[index].measure(
                if (layout.alignItems == BookDocumentItemAlignment.STRETCH) {
                    Constraints.fixed(placement.width, lineHeight)
                } else {
                    Constraints(minWidth = placement.width, maxWidth = placement.width, maxHeight = lineHeight)
                },
            )
        }
        val height = (lineTops.last() - rowGap).coerceAtLeast(0)
        layout(available, height) {
            placeables.forEachIndexed { index, placeable ->
                val placement = placements[index]
                val free = lineHeights[placement.line] - placeable.height
                val offset = when (layout.alignItems) {
                    BookDocumentItemAlignment.CENTER -> free / 2
                    BookDocumentItemAlignment.END -> free
                    BookDocumentItemAlignment.START, BookDocumentItemAlignment.STRETCH -> 0
                }
                placeable.placeRelative(placement.x, lineTops[placement.line] + offset)
            }
        }
    }
}

// A box around every block of the item is the item's own and stretches with it; the other boxes stay with their
// blocks.
@Composable
private fun SideBySideItem(
    item: BookDocumentSideBySideItem,
    selectionIdentity: String,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    emDp: Float,
) {
    val styling = LocalBookDocumentAuthoredStyling.current
    val body = item.body
    val itemBox = remember(body) {
        body.boxes.firstOrNull { it.firstBlockIndex == 0 && it.lastBlockIndex == body.blocks.lastIndex }
    }
    val boxIndex =
        remember(body, itemBox) { BookDocumentBoxIndex(body.blocks.size, body.boxes - setOfNotNull(itemBox)) }
    val itemSlices = if (styling.enabled && itemBox != null) {
        listOf(BookDocumentBoxSlice(itemBox.style, opens = true, closes = true))
    } else {
        emptyList()
    }
    Column(Modifier.bookDocumentBoxes(itemSlices, styling, emDp)) {
        body.blocks.forEachIndexed { index, nested ->
            val slices = if (styling.enabled) boxIndex.slices(index) else emptyList()
            CompositionLocalProvider(
                LocalBookDocumentTextBackdrop provides (itemSlices + slices).textBackdrop(styling),
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
