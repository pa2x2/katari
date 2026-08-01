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
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette

/** Selectable native-text projection of a bounded semantic table. */
@Composable
internal fun BookDocumentTableRenderer(
    content: BookDocumentBlockContent.Table,
    block: BookDocumentBlock,
    onAnchorClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
) {
    val palette = LocalBookDocumentReaderPalette.current
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        content.caption?.let {
            BookDocumentRichTextRenderer(
                value = it,
                identity = "${block.id.value}:table-caption",
                block = block,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
            )
        }
        content.rows.forEachIndexed { rowIndex, row ->
            Row {
                row.cells.forEachIndexed { cellIndex, cell ->
                    BookDocumentRichTextRenderer(
                        value = cell.content,
                        identity = "${block.id.value}:cell:$rowIndex:$cellIndex",
                        block = block,
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onReaderTap = onReaderTap,
                        modifier = Modifier
                            .width((120 * cell.columnSpan).dp)
                            .background(palette.surfaceVariant)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
