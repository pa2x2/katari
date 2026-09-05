package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.table.BookDocumentTableRenderer
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Projects and renders one composed semantic block without retaining chapter-wide Android spans. */
@Composable
internal fun BookDocumentBlockRenderer(
    block: BookDocumentBlock,
    owningContent: BookDocumentContent,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    selectionIdentity: String = block.id.value,
    preserveTerminalSpacing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val textScale = LocalBookDocumentTextScale.current
    val selection = LocalBookDocumentChapterSelection.current
    val blockText = remember(block, owningContent.text) {
        owningContent.text.substring(block.logicalStart, block.logicalEndExclusive)
    }
    val padding = (block.style.paddingEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp
    Column(
        modifier = modifier.padding(
            start = padding,
            top = padding + (block.style.spacingBeforeEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp,
            end = padding,
            bottom = padding + (block.style.spacingAfterEm * BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * textScale).dp,
        ),
    ) {
        when (val content = block.content) {
            is BookDocumentBlockContent.Text -> BookDocumentSelectableText(
                text = blockText,
                links = block.links,
                inlineStyles = block.inlineStyles,
                identity = selectionIdentity,
                block = block,
                separatorAfter = "\n\n",
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                preserveTerminalSpacing = preserveTerminalSpacing,
            )
            is BookDocumentBlockContent.ListBlock -> Column(
                verticalArrangement = Arrangement.spacedBy((4 * textScale).dp),
            ) {
                content.items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.padding(start = (item.depth * 18 * textScale).dp)) {
                        BookDocumentSelectableText(
                            text = item.marker ?: if (content.ordered) "${content.start + index}." else "•",
                            links = emptyList(),
                            inlineStyles = emptyList(),
                            identity = "$selectionIdentity:list-marker:$index",
                            block = block,
                            leadingSelectionText = "  ".repeat(item.depth),
                            separatorAfter = " ",
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            modifier = Modifier.width((32 * textScale).dp),
                        )
                        BookDocumentRichTextRenderer(
                            value = item.content,
                            identity = "$selectionIdentity:list:$index",
                            block = block,
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            separatorAfter = if (index == content.items.lastIndex) "\n\n" else "\n",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            is BookDocumentBlockContent.Figure -> BookDocumentFigureRenderer(
                content = content,
                block = block,
                selectionIdentity = selectionIdentity,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
            )
            is BookDocumentBlockContent.Table -> BookDocumentTableRenderer(
                content = content,
                block = block,
                selectionIdentity = selectionIdentity,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
            )
            is BookDocumentBlockContent.Disclosure -> BookDocumentDisclosureRenderer(
                content = content,
                block = block,
                selectionIdentity = selectionIdentity,
                sectionKey = sectionKey,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                preserveTerminalSpacing = preserveTerminalSpacing,
            )
            BookDocumentBlockContent.ThematicBreak -> HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selection?.handleReaderTap(onReaderTap) ?: onReaderTap()
                    },
                color = palette.outline,
            )
            is BookDocumentBlockContent.Unsupported -> BookDocumentSelectableText(
                text = stringResource(R.string.book_document_unsupported, content.elementType),
                links = emptyList(),
                inlineStyles = emptyList(),
                identity = "$selectionIdentity:unsupported",
                block = block,
                separatorAfter = "\n\n",
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                contentAlpha = 0.72f,
            )
        }
    }
}
