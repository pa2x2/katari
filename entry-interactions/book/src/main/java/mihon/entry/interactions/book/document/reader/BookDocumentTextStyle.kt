package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentTextDirection

/** Shared typography for selectable text and table geometry measurement. */
internal fun bookDocumentFontSize(block: BookDocumentBlock, textScale: Float, baseFontSizeSp: Float): TextUnit {
    val headingScale = when (block.role.takeIf { it.kind == BookDocumentBlockKind.HEADING }?.level) {
        1 -> 1.5f
        2 -> 1.4f
        3 -> 1.3f
        4 -> 1.2f
        5 -> 1.1f
        else -> 1f
    }
    return (baseFontSizeSp * textScale * block.style.fontSizeScale * headingScale).sp
}

internal fun bookDocumentTextStyle(
    block: BookDocumentBlock,
    fontSize: TextUnit,
    fonts: Map<String, FontFamily>,
): TextStyle = TextStyle(
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
    fontFamily = block.style.fontFamily.toComposeFontFamily(fonts),
    textAlign = when (block.style.alignment) {
        BookDocumentAlignment.CENTER -> TextAlign.Center
        BookDocumentAlignment.END -> TextAlign.End
        else -> TextAlign.Start
    },
)
