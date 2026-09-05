package mihon.entry.interactions.book.document.reader.table

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentTableCell
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.bookDocumentFontSize
import mihon.entry.interactions.book.document.reader.bookDocumentTextPresentation
import mihon.entry.interactions.book.document.reader.bookDocumentTextStyle
import mihon.entry.interactions.book.document.reader.toSelectableAnnotatedString

internal data class BookDocumentTableCellText(
    val text: AnnotatedString,
    val style: TextStyle,
    val bottomSpacing: Int,
)

internal fun bookDocumentTableTexts(
    cells: List<BookDocumentTableCell>,
    block: BookDocumentBlock,
    fonts: Map<String, FontFamily>,
    textScale: Float,
    density: Density,
): List<BookDocumentTableCellText> =
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
