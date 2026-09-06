package mihon.entry.interactions.book.document.reader.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentTableLayout
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.rememberBookDocumentFonts

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
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val layoutDirection = LocalLayoutDirection.current
    val width = with(density) { viewportWidth.roundToPx().coerceAtLeast(1) }
    val cache = LocalBookDocumentTableCache.current
    cache?.get(block, width, fonts)?.let { return it }
    val texts = remember(cells, block.style, block.role, fonts, textScale, density) {
        bookDocumentTableTexts(cells, block, fonts, textScale, density)
    }
    return remember(grid, texts, fontFamilyResolver, layoutDirection, viewportWidth, density) {
        with(density) {
            measureBookDocumentTable(
                grid,
                texts,
                BookDocumentTableTextMeasurer(density, layoutDirection, fontFamilyResolver),
                width,
                8.dp.roundToPx(),
            )
        }
    }
}
