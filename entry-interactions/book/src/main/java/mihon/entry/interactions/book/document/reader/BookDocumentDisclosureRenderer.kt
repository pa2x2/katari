package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Expandable recursive semantic disclosure renderer. */
@Composable
internal fun BookDocumentDisclosureRenderer(
    content: BookDocumentBlockContent.Disclosure,
    block: BookDocumentBlock,
    selectionIdentity: String,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    preserveTerminalSpacing: Boolean,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val textScale = LocalBookDocumentTextScale.current
    val selection = LocalBookDocumentChapterSelection.current
    var expanded by remember(content) { mutableStateOf(content.initiallyExpanded) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                selection?.handleReaderTap { expanded = !expanded } ?: run { expanded = !expanded }
            }
            .padding(vertical = 8.dp),
    ) {
        DisableSelection {
            Text(
                text = if (expanded) "▾ " else "▸ ",
                color = palette.foreground,
            )
        }
        BookDocumentRichTextRenderer(
            value = content.summary,
            identity = "$selectionIdentity:disclosure-summary",
            block = block,
            onAnchorClick = onAnchorClick,
            onExternalLinkClick = onExternalLinkClick,
            separatorAfter = if (expanded) "\n" else "\n\n",
            baseFontSizeSp = 14f,
            modifier = Modifier.weight(1f),
        )
    }
    if (expanded) {
        content.body.blocks.forEachIndexed { index, nested ->
            BookDocumentBlockRenderer(
                block = nested,
                owningContent = content.body,
                sectionKey = sectionKey,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                selectionIdentity = "$selectionIdentity:body:$index:${nested.id.value}",
                preserveTerminalSpacing = index != content.body.blocks.lastIndex || preserveTerminalSpacing,
                modifier = Modifier.padding(start = (12 * textScale).dp),
            )
        }
    }
}
