package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
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
    val textScale = LocalBookDocumentTextScale.current
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
        content.rows.forEachIndexed { rowIndex, row ->
            Row {
                row.cells.forEachIndexed { cellIndex, cell ->
                    BookDocumentRichTextRenderer(
                        value = cell.content,
                        identity = "$selectionIdentity:cell:$rowIndex:$cellIndex",
                        block = block,
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        separatorAfter = when {
                            cellIndex < row.cells.lastIndex -> "\t"
                            rowIndex < content.rows.lastIndex -> "\n"
                            else -> "\n\n"
                        },
                        modifier = Modifier
                            .width((120 * cell.columnSpan * textScale).dp)
                            .background(palette.surfaceVariant)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
