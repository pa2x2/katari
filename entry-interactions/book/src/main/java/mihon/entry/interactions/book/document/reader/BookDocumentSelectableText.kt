package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentTextDirection
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette

/** Compose text leaf that keeps document semantics available to the chapter selection owner. */
@Composable
internal fun BookDocumentSelectableText(
    text: String,
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    identity: String,
    block: BookDocumentBlock,
    separatorAfter: String,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    leadingSelectionText: String = "",
    preserveTerminalSpacing: Boolean = true,
    baseFontSizeSp: Float = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP,
    contentAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBookDocumentReaderPalette.current
    val textScale = LocalBookDocumentTextScale.current
    val selection = LocalBookDocumentChapterSelection.current
    val chapterId = requireNotNull(LocalBookDocumentSelectionChapterId.current) {
        "Selectable book text must belong to a chapter"
    }
    val sectionKey = LocalBookDocumentSectionKey.current.orEmpty()
    val terminalLineBreaks = text.length - text.trimEnd('\n').length
    val visibleText = text.dropLast(terminalLineBreaks)
    val token = remember(sectionKey, identity) { "$sectionKey::$identity" }
    val trackSelectionGeometry = selection?.shouldTrackGeometry(token) == true
    val leaf = remember(token, chapterId, visibleText, leadingSelectionText, separatorAfter) {
        BookDocumentSelectableLeaf(
            token = token,
            chapterId = chapterId,
            fullText = visibleText,
            leadingText = leadingSelectionText,
            separatorAfter = separatorAfter,
        )
    }
    DisposableEffect(selection, leaf) {
        selection?.registerText(leaf)
        onDispose { selection?.unregisterText(token) }
    }

    val headingScale = when (block.role.takeIf { it.kind == BookDocumentBlockKind.HEADING }?.level) {
        1 -> 1.5f
        2 -> 1.4f
        3 -> 1.3f
        4 -> 1.2f
        5 -> 1.1f
        else -> 1f
    }
    val fontSize = (baseFontSizeSp * textScale * block.style.fontSizeScale * headingScale).sp
    val annotatedText = remember(
        visibleText,
        links,
        inlineStyles,
        token,
        fontSize,
        palette.accent,
        selection,
        onAnchorClick,
        onExternalLinkClick,
    ) {
        visibleText.toSelectableAnnotatedString(
            links = links,
            inlineStyles = inlineStyles,
            token = token,
            baseFontSize = fontSize.value,
            linkColor = palette.accent,
            onLinkClick = { target ->
                if (selection?.consumeSelectionTap() != true) {
                    when (target) {
                        is BookDocumentLinkTarget.Anchor,
                        is BookDocumentLinkTarget.Resource,
                        is BookDocumentLinkTarget.Reference,
                        -> onAnchorClick(target)
                        is BookDocumentLinkTarget.External -> onExternalLinkClick(target.url)
                    }
                }
            },
        )
    }
    val terminalSpacing = if (preserveTerminalSpacing && terminalLineBreaks > 0) {
        (terminalLineBreaks - 1).coerceAtLeast(0).times(20 * textScale).dp
    } else {
        0.dp
    }
    val quoteModifier = if (block.role.kind == BookDocumentBlockKind.QUOTE) {
        val quoteColor = palette.outline
        Modifier
            .drawBehind { drawRect(quoteColor, size = size.copy(width = 3.dp.toPx())) }
            .padding(start = 12.dp)
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        Text(
            text = annotatedText,
            color = palette.foreground.copy(alpha = contentAlpha),
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize * block.style.lineHeightScale,
                textIndent = TextIndent(firstLine = fontSize * block.style.firstLineIndentEm),
                textDirection = when (block.style.direction) {
                    BookDocumentTextDirection.LEFT_TO_RIGHT -> TextDirection.Ltr
                    BookDocumentTextDirection.RIGHT_TO_LEFT -> TextDirection.Rtl
                    null -> TextDirection.Content
                },
                localeList = block.style.languageTag?.let { language -> LocaleList(Locale(language)) },
                fontWeight = if (block.style.bold || block.role.kind == BookDocumentBlockKind.HEADING) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                fontFamily = block.style.fontFamily.toComposeFontFamily(),
                textAlign = when (block.style.alignment) {
                    BookDocumentAlignment.CENTER -> TextAlign.Center
                    BookDocumentAlignment.END -> TextAlign.End
                    else -> TextAlign.Start
                },
            ),
            onTextLayout = { result -> selection?.updateTextLayout(token, result) },
            modifier = Modifier
                .fillMaxWidth()
                .then(quoteModifier)
                .then(
                    if (trackSelectionGeometry) {
                        Modifier.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) { bounds ->
                            selection.updateTextPosition(token, bounds.positionInWindow)
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(bottom = terminalSpacing),
        )
    }
}

private fun String.toSelectableAnnotatedString(
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    token: String,
    baseFontSize: Float,
    linkColor: Color,
    onLinkClick: (BookDocumentLinkTarget) -> Unit,
): AnnotatedString = AnnotatedString.Builder(this).apply {
    if (length > 0) {
        addStringAnnotation(BOOK_DOCUMENT_SELECTION_TOKEN_TAG, token, 0, length)
    }
    inlineStyles.forEach { range ->
        val start = range.start.coerceIn(0, length)
        val end = range.endExclusive.coerceIn(start, length)
        if (end > start) addStyle(range.style.toComposeSpanStyle(baseFontSize), start, end)
    }
    links.forEachIndexed { index, link ->
        val start = link.start.coerceIn(0, length)
        val end = link.endExclusive.coerceIn(start, length)
        if (end <= start) return@forEachIndexed
        addLink(
            LinkAnnotation.Clickable(
                tag = "book-document-link-$index",
                styles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
                linkInteractionListener = LinkInteractionListener { onLinkClick(link.target) },
            ),
            start,
            end,
        )
    }
}.toAnnotatedString()

private fun BookDocumentInlineStyle.toComposeSpanStyle(baseFontSize: Float): SpanStyle {
    val decorations = buildList {
        if (underline) add(TextDecoration.Underline)
        if (strikethrough) add(TextDecoration.LineThrough)
    }
    val scale = fontSizeScale ?: if (small) SMALL_TEXT_SCALE else 1f
    return SpanStyle(
        fontSize = (baseFontSize * scale).sp,
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        fontFamily = fontFamily.toComposeFontFamily().takeUnless { it == FontFamily.Default }
            ?: if (code) FontFamily.Monospace else null,
        textDecoration = decorations.takeIf(List<TextDecoration>::isNotEmpty)
            ?.let(TextDecoration::combine),
        baselineShift = when {
            subscript -> BaselineShift.Subscript
            superscript -> BaselineShift.Superscript
            else -> null
        },
        localeList = languageTag?.let { language -> LocaleList(Locale(language)) },
    )
}

private fun BookDocumentFontFamily?.toComposeFontFamily(): FontFamily =
    when ((this as? BookDocumentFontFamily.Generic)?.family) {
        BookDocumentFontFamily.GenericFamily.SERIF -> FontFamily.Serif
        BookDocumentFontFamily.GenericFamily.SANS_SERIF -> FontFamily.SansSerif
        BookDocumentFontFamily.GenericFamily.MONOSPACE -> FontFamily.Monospace
        null -> FontFamily.Default
    }

private const val SMALL_TEXT_SCALE = 0.8f
