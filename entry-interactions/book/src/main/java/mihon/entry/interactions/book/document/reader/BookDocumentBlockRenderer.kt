package mihon.entry.interactions.book.document.reader

import android.graphics.Typeface
import android.text.Layout
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentRichText
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.render.toBookDocumentSpanned
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Projects and renders one composed semantic block without retaining chapter-wide Android spans. */
@Composable
internal fun BookDocumentBlockRenderer(
    block: BookDocumentBlock,
    owningContent: BookDocumentContent,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    preserveTerminalSpacing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val blockText = remember(block, owningContent.text) {
        owningContent.text.substring(block.logicalStart, block.logicalEndExclusive)
    }
    val padding = (block.style.paddingEm * 16).dp
    Column(modifier = modifier.padding(padding)) {
        when (val content = block.content) {
            is BookDocumentBlockContent.Text -> DocumentText(
                text = remember(block, blockText) { blockText.toBookDocumentSpanned(block) },
                identity = block.id.value,
                block = block,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                preserveTerminalSpacing = preserveTerminalSpacing,
            )
            is BookDocumentBlockContent.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                content.items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.padding(start = (item.depth * 18).dp)) {
                        Text(
                            text = item.marker ?: if (content.ordered) "${content.start + index}." else "•",
                            modifier = Modifier.width(32.dp),
                            color = palette.foreground,
                        )
                        BookDocumentRichTextRenderer(
                            value = item.content,
                            identity = "${block.id.value}:list:$index",
                            block = block,
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            onReaderTap = onReaderTap,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            is BookDocumentBlockContent.Figure -> BookDocumentFigureRenderer(
                content = content,
                block = block,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
            )
            is BookDocumentBlockContent.Table -> BookDocumentTableRenderer(
                content = content,
                block = block,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
            )
            is BookDocumentBlockContent.Disclosure -> BookDocumentDisclosureRenderer(
                content = content,
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
                    .clickable(onClick = onReaderTap),
                color = palette.outline,
            )
            is BookDocumentBlockContent.Unsupported -> Text(
                text = stringResource(R.string.book_document_unsupported, content.elementType),
                modifier = Modifier.clickable(onClick = onReaderTap),
                color = palette.foreground.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun DocumentText(
    text: android.text.Spanned,
    identity: String,
    block: BookDocumentBlock,
    onAnchorClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    preserveTerminalSpacing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val color = palette.foreground.toArgb()
    val typeface = when ((block.style.fontFamily as? BookDocumentFontFamily.Generic)?.family) {
        BookDocumentFontFamily.GenericFamily.SERIF -> Typeface.SERIF
        BookDocumentFontFamily.GenericFamily.MONOSPACE -> Typeface.MONOSPACE
        else -> Typeface.DEFAULT
    }
    BookDocumentText(
        text = text,
        documentTextIdentity = identity,
        textColor = color,
        linkTextColor = palette.accent.toArgb(),
        textSizeSp = 16f * block.style.fontSizeScale,
        typeface = typeface,
        lineSpacingMultiplier = 1.25f,
        textAlignment = when (block.style.alignment) {
            BookDocumentAlignment.CENTER -> View.TEXT_ALIGNMENT_CENTER
            BookDocumentAlignment.END -> View.TEXT_ALIGNMENT_VIEW_END
            else -> View.TEXT_ALIGNMENT_VIEW_START
        },
        justificationMode = Layout.JUSTIFICATION_MODE_NONE,
        trimTerminalLine = true,
        preserveTerminalSpacing = preserveTerminalSpacing,
        onAnchorClick = { anchor, _ -> onAnchorClick(anchor) },
        onExternalLinkClick = onExternalLinkClick,
        onNonLinkClick = onReaderTap,
        onViewChanged = {},
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun BookDocumentRichTextRenderer(
    value: BookDocumentRichText,
    identity: String,
    block: BookDocumentBlock,
    onAnchorClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spanned = remember(value) { value.toBookDocumentSpanned() }
    DocumentText(
        text = spanned,
        identity = identity,
        block = block,
        onAnchorClick = onAnchorClick,
        onExternalLinkClick = onExternalLinkClick,
        onReaderTap = onReaderTap,
        modifier = modifier,
    )
}
