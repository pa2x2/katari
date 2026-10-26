package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.position.BookDocumentViewportText
import mihon.entry.interactions.book.document.reader.position.LocalBookDocumentViewportGeometry
import mihon.entry.interactions.book.document.reader.search.LocalBookDocumentContentStart
import mihon.entry.interactions.book.document.reader.search.LocalBookDocumentSearchMarks
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentTextBackdrop
import mihon.presentation.kit.content.ReadingMetrics
import mihon.presentation.kit.theme.KatariTheme

/**
 * Compose text leaf that keeps document semantics available to the chapter selection owner. [standIn] marks text that
 * replaces missing content, such as an unsupported element's notice, and draws it in the page's stand-in colour.
 * [canonicalStart] is where [text] starts within [block]'s canonical text, which places search marks on it; null for
 * text the document doesn't contain, such as a list marker or a notice.
 */
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
    standIn: Boolean = false,
    canonicalStart: Int? = null,
    modifier: Modifier = Modifier,
) {
    val fonts = rememberBookDocumentFonts(block.style.fontFamily, inlineStyles)
    val palette = LocalBookDocumentReaderPalette.current
    val typography = LocalBookDocumentTypography.current
    val textScale = typography.textScale
    val selection = LocalBookDocumentChapterSelection.current
    val chapterId = requireNotNull(LocalBookDocumentSelectionChapterId.current) {
        "Selectable book text must belong to a chapter"
    }
    val sectionKey = LocalBookDocumentSectionKey.current.orEmpty()
    val terminalLineBreaks = text.length - text.trimEnd('\n').length
    val visibleText = text.dropLast(terminalLineBreaks)
    val transform = block.style.textTransform
    val presentation = remember(visibleText, inlineStyles, transform) {
        bookDocumentTextPresentation(visibleText, inlineStyles, transform)
    }
    val styling = LocalBookDocumentAuthoredStyling.current
    val backdrop = LocalBookDocumentTextBackdrop.current
    val density = LocalDensity.current
    val textColor = if (standIn) {
        palette.foregroundVariant
    } else {
        styling.text(block.style.foregroundArgb, backdrop).takeOrElse {
            styling.inheritedText(palette.foreground, backdrop)
        }
    }
    val authored = remember(styling, backdrop, textColor, density) {
        BookDocumentAuthoredText(styling, backdrop, textColor, density)
    }
    val token = remember(sectionKey, identity) { "$sectionKey::$identity" }
    val viewportGeometry = LocalBookDocumentViewportGeometry.current
    val viewportText = remember(viewportGeometry, token, block.logicalStart, presentation) {
        viewportGeometry?.let { BookDocumentViewportText(sectionKey, block.logicalStart, presentation) }
    }
    DisposableEffect(viewportGeometry, viewportText, token) {
        if (viewportText != null) viewportGeometry?.texts?.set(token, viewportText)
        onDispose { viewportGeometry?.texts?.remove(token) }
    }
    val trackSelectionGeometry = selection?.shouldTrackGeometry(token) == true
    if (trackSelectionGeometry) {
        DisposableEffect(selection, token) {
            // Scrolling is unobserved while this leaf has no selection. Its next selection must
            // wait for the reattached position callback instead of publishing the old anchor.
            onDispose { selection.clearTextPosition(token) }
        }
    }
    val languageTag = block.style.languageTag
    val leaf = remember(token, chapterId, presentation, leadingSelectionText, separatorAfter, languageTag) {
        BookDocumentSelectableLeaf(
            token = token,
            chapterId = chapterId,
            fullText = presentation.text,
            insertedBidiOffsets = presentation.insertedOffsets,
            leadingText = leadingSelectionText,
            separatorAfter = separatorAfter,
            languageTag = languageTag,
        )
    }
    DisposableEffect(selection, leaf) {
        selection?.registerText(leaf)
        onDispose { selection?.unregisterText(token) }
    }

    val searchMarks = LocalBookDocumentSearchMarks.current
    val contentStart = LocalBookDocumentContentStart.current
    val searchStart = canonicalStart?.let { contentStart + block.logicalStart + it }
    val marks = remember(searchMarks, sectionKey, searchStart, visibleText.length) {
        if (searchMarks == null || searchStart == null) {
            emptyList()
        } else {
            searchMarks.within(sectionKey, searchStart, visibleText.length)
        }
    }
    val fontSize = bookDocumentFontSize(block, textScale, baseFontSizeSp)
    val annotatedText = remember(
        presentation,
        fonts,
        links,
        inlineStyles,
        token,
        fontSize,
        selection,
        onAnchorClick,
        onExternalLinkClick,
        authored,
        marks,
        palette,
    ) {
        presentation.toSelectableAnnotatedString(
            links = links,
            fonts = fonts,
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
            authored = authored,
            searchMarks = marks,
            searchMatchColor = palette.searchMatch,
            currentSearchMatchColor = palette.currentSearchMatch,
        )
    }
    val terminalSpacing = if (preserveTerminalSpacing) bookDocumentTerminalSpacing(text, textScale) else 0.dp
    val quoteModifier = if (block.role.kind == BookDocumentBlockKind.QUOTE) {
        val quoteColor = palette.outline
        Modifier
            .drawBehind { drawRect(quoteColor, size = size.copy(width = ReadingMetrics.quoteRuleWidth.toPx())) }
            .padding(start = KatariTheme.spacing.m)
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        BasicText(
            text = annotatedText,
            style = bookDocumentTextStyle(
                block,
                fontSize,
                fonts,
                typography,
                authored.textShadow(block.style.textShadow, textColor, backdrop),
            )
                .copy(color = textColor),
            onTextLayout = { result ->
                selection?.updateTextLayout(token, result)
                viewportText?.layout = result
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(quoteModifier)
                .then(if (viewportText != null) Modifier.onGloballyPositioned { viewportText.placed(it) } else Modifier)
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

/** The gap a paragraph's extra terminal line breaks leave below it. */
internal fun bookDocumentTerminalSpacing(text: String, textScale: Float): Dp {
    val breaks = text.length - text.trimEnd('\n').length
    return (breaks - 1).coerceAtLeast(0).times(20 * textScale).dp
}
