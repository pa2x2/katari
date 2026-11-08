package app.katari.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.book.api.document.BookDocumentBlock
import app.katari.book.api.document.BookDocumentLinkTarget
import app.katari.book.api.document.BookDocumentRichText

@Composable
internal fun BookDocumentRichTextRenderer(
    value: BookDocumentRichText,
    identity: String,
    block: BookDocumentBlock,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    separatorAfter: String = "\n\n",
    leadingSelectionText: String = "",
    baseFontSizeSp: Float = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP,
    modifier: Modifier = Modifier,
) {
    BookDocumentSelectableText(
        text = value.text,
        links = value.links,
        inlineStyles = value.inlineStyles,
        identity = identity,
        block = block,
        separatorAfter = separatorAfter,
        onAnchorClick = onAnchorClick,
        onExternalLinkClick = onExternalLinkClick,
        leadingSelectionText = leadingSelectionText,
        baseFontSizeSp = baseFontSizeSp,
        canonicalStart = value.range.start,
        modifier = modifier,
    )
}
