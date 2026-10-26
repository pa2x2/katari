package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.presentation.kit.content.documentSpanStyle

internal fun BookDocumentTextPresentation.toSelectableAnnotatedString(
    fonts: Map<String, FontFamily>,
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    token: String,
    baseFontSize: Float,
    linkColor: Color,
    onLinkClick: (BookDocumentLinkTarget) -> Unit,
    authored: BookDocumentAuthoredText? = null,
): AnnotatedString = AnnotatedString.Builder(text).apply {
    if (length > 0) {
        addStringAnnotation(BOOK_DOCUMENT_SELECTION_TOKEN_TAG, token, 0, length)
    }
    inlineStyles.forEach { range ->
        val start = this@toSelectableAnnotatedString.start(range.start)
        val end = this@toSelectableAnnotatedString.end(range.endExclusive)
        if (end > start) addStyle(range.style.toComposeSpanStyle(baseFontSize, fonts, authored), start, end)
    }
    links.forEachIndexed { index, link ->
        val start = this@toSelectableAnnotatedString.start(link.start)
        val end = this@toSelectableAnnotatedString.end(link.endExclusive)
        if (end <= start) return@forEachIndexed
        addLink(
            LinkAnnotation.Clickable(
                tag = "book-document-link-$index",
                styles = TextLinkStyles(
                    style = documentSpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
                linkInteractionListener = LinkInteractionListener { onLinkClick(link.target) },
            ),
            start,
            end,
        )
    }
}.toAnnotatedString()

private fun BookDocumentInlineStyle.toComposeSpanStyle(
    baseFontSize: Float,
    fonts: Map<String, FontFamily>,
    authored: BookDocumentAuthoredText?,
): SpanStyle {
    val background = authored?.styling?.fill(backgroundArgb) ?: Color.Unspecified
    val color = authored?.styling?.text(foregroundArgb, background.takeOrElse { authored.backdrop })
        ?: Color.Unspecified
    val decorations = buildList {
        if (underline) add(TextDecoration.Underline)
        if (strikethrough) add(TextDecoration.LineThrough)
    }
    val scale = fontSizeScale ?: if (small) SMALL_TEXT_SCALE else 1f
    return documentSpanStyle(
        color = color,
        background = background,
        letterSpacing = letterSpacingEm?.em ?: TextUnit.Unspecified,
        smallCaps = smallCaps,
        shadow = authored?.textShadow(textShadow, color),
        fontSize = (baseFontSize * scale).sp,
        bold = bold,
        italic = italic,
        fontFamily = fontFamily.toComposeFontFamily(fonts).takeUnless { it == FontFamily.Default }
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

private const val SMALL_TEXT_SCALE = 0.8f
