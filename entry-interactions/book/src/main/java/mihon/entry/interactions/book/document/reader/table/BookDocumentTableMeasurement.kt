package mihon.entry.interactions.book.document.reader.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentTableLayout
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.bookDocumentFontSize
import mihon.entry.interactions.book.document.reader.bookDocumentTextPresentation
import mihon.entry.interactions.book.document.reader.bookDocumentTextStyle
import mihon.entry.interactions.book.document.reader.rememberBookDocumentFonts
import mihon.entry.interactions.book.document.reader.toSelectableAnnotatedString

/** Resolves cell typography and invalidates geometry when reading width or fonts change. */
@Composable
internal fun rememberBookDocumentTableGeometry(
    grid: BookDocumentTableLayout,
    block: BookDocumentBlock,
    viewportWidth: Dp,
): BookDocumentTableGeometry {
    val density = LocalDensity.current
    val textScale = LocalBookDocumentTextScale.current
    val cells = remember(grid) { grid.rows.flatMap { it.placements.map { placement -> placement.cell } } }
    val inlineStyles = remember(cells) { cells.flatMap { it.content.inlineStyles } }
    val fonts = rememberBookDocumentFonts(block.style.fontFamily, inlineStyles)
    val measurer = rememberTextMeasurer(cacheSize = 128)
    val texts = remember(cells, block.style, block.role, fonts, textScale, density) {
        cells.map { cell ->
            val cellBlock = if (cell.header) block.copy(style = block.style.copy(bold = true)) else block
            val fontSize = bookDocumentFontSize(cellBlock, textScale, BOOK_DOCUMENT_BASE_TEXT_SIZE_SP)
            val text = cell.content.text.trimEnd('\n')
            val terminalBreaks = cell.content.text.length - text.length
            BookDocumentTableCellText(
                text = bookDocumentTextPresentation(text, cell.content.inlineStyles).toSelectableAnnotatedString(
                    fonts = fonts,
                    links = emptyList(),
                    inlineStyles = cell.content.inlineStyles,
                    token = "",
                    baseFontSize = fontSize.value,
                    linkColor = Color.Unspecified,
                    onLinkClick = {},
                ),
                style = bookDocumentTextStyle(cellBlock, fontSize, fonts),
                bottomSpacing = with(density) {
                    ((terminalBreaks - 1).coerceAtLeast(0) * 20 * textScale).dp.roundToPx()
                },
            )
        }
    }
    return remember(grid, texts, measurer, viewportWidth, density) {
        with(density) {
            measureBookDocumentTable(
                grid,
                texts,
                measurer,
                viewportWidth.roundToPx().coerceAtLeast(1),
                8.dp.roundToPx(),
            )
        }
    }
}
