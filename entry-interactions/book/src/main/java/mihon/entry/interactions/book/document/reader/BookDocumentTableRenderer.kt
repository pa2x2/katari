package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.layoutBookDocumentTable
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette

/** Selectable native-text projection of a bounded semantic table. */
@Composable
internal fun BookDocumentTableRenderer(
    content: BookDocumentBlockContent.Table,
    block: BookDocumentBlock,
    selectionIdentity: String,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val grid = remember(content.rows) { requireNotNull(content.rows.layoutBookDocumentTable()) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            content.caption?.let {
                BookDocumentRichTextRenderer(
                    value = it,
                    identity = "$selectionIdentity:table-caption",
                    block = block,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    separatorAfter = "\n",
                )
            }
            BookDocumentTableGrid(grid, viewportWidth, Modifier.background(palette.surfaceVariant)) {
                content.rows.forEachIndexed { rowIndex, row ->
                    row.cells.forEachIndexed { cellIndex, cell ->
                        BookDocumentRichTextRenderer(
                            value = cell.content,
                            identity = "$selectionIdentity:cell:$rowIndex:$cellIndex",
                            block = if (cell.header) block.copy(style = block.style.copy(bold = true)) else block,
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            separatorAfter = when {
                                cellIndex < row.cells.lastIndex -> "\t"
                                rowIndex < content.rows.lastIndex -> "\n"
                                else -> "\n\n"
                            },
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
