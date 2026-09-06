package mihon.entry.interactions.book.document.reader.table

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.layoutBookDocumentTable
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.LocalBookDocumentResourceLoader
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.rememberBookDocumentFonts

/** Prepares table geometry when sections arrive, before lazy scrolling needs their cells. */
@Composable
internal fun <T> BookDocumentTablePreparation(
    sections: List<BookDocumentSection<T>>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val textScale = LocalBookDocumentTextScale.current
    val tables = remember(sections) {
        sections.flatMap { section ->
            section.document.blocks.filter { it.content is BookDocumentBlockContent.Table }.map { section to it }
        }
    }
    BoxWithConstraints(modifier) {
        val cache = remember(constraints.maxWidth, density, layoutDirection, fontFamilyResolver, textScale) {
            BookDocumentTableCache()
        }
        LaunchedEffect(cache, tables) { cache.retain(tables.map { it.second }) }
        tables.forEach { (section, block) ->
            key(section.key, block.id) {
                CompositionLocalProvider(LocalBookDocumentResourceLoader provides section.resourceLoader) {
                    val width = with(density) {
                        val authoredPadding = (block.style.paddingEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp
                        (
                            constraints.maxWidth - BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING.roundToPx() * 2 -
                                authoredPadding.roundToPx() * 2
                            ).coerceAtLeast(1)
                    }
                    PrepareTable(block, width, cache)
                }
            }
        }
        CompositionLocalProvider(LocalBookDocumentTableCache provides cache, content = content)
    }
}

@Composable
private fun PrepareTable(block: BookDocumentBlock, width: Int, cache: BookDocumentTableCache) {
    val table = block.content as BookDocumentBlockContent.Table
    val cells = remember(table) { table.rows.flatMap { it.cells } }
    val inlineStyles = remember(cells) { cells.flatMap { it.content.inlineStyles } }
    val fonts = rememberBookDocumentFonts(block.style.fontFamily, inlineStyles)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val textScale = LocalBookDocumentTextScale.current
    LaunchedEffect(cache, block, width, fonts) {
        withContext(cache.dispatcher) {
            val grid = requireNotNull(table.rows.layoutBookDocumentTable())
            val texts = bookDocumentTableTexts(cells, block, fonts, textScale, density)
            val geometry = measureBookDocumentTable(
                grid,
                texts,
                BookDocumentTableTextMeasurer(density, layoutDirection, fontFamilyResolver),
                width,
                with(density) { 8.dp.roundToPx() },
            )
            ensureActive()
            cache.put(block, width, fonts, geometry)
        }
    }
}
