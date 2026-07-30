package mihon.entry.interactions.book.prose

import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBorder
import mihon.entry.interactions.book.document.model.BookDocumentBorderStyle
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyle
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import org.jsoup.nodes.Element

internal fun Element.documentStyle(): BookDocumentStyle {
    val border = attr("data-katari-border")
        .split('|')
        .takeIf { it.size >= 2 }
        ?.let { parts ->
            val width = parts[0].toFloatOrNull() ?: return@let null
            val borderStyle = when (parts[1]) {
                "dashed" -> BookDocumentBorderStyle.DASHED
                "dotted" -> BookDocumentBorderStyle.DOTTED
                else -> BookDocumentBorderStyle.SOLID
            }
            BookDocumentBorder(
                widthDp = width.coerceIn(0.5f, 8f),
                colorArgb = parts.getOrNull(2)?.toArgbLong(),
                style = borderStyle,
            )
        }
    val fontFamily = when {
        hasAttr("data-katari-font-resource") ->
            BookDocumentFontFamily.Resource(attr("data-katari-font-resource"))
        attr("data-katari-font-generic") == "sans-serif" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SANS_SERIF)
        attr("data-katari-font-generic") == "monospace" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.MONOSPACE)
        attr("data-katari-font-generic") == "serif" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SERIF)
        else -> null
    }
    return BookDocumentStyle(
        alignment = when (attr("data-katari-align")) {
            "center" -> BookDocumentAlignment.CENTER
            "right", "end" -> BookDocumentAlignment.END
            "left", "start" -> BookDocumentAlignment.START
            else -> null
        },
        whiteSpace = when (attr("data-katari-white-space")) {
            "pre" -> BookDocumentWhiteSpace.PRE
            "pre-wrap" -> BookDocumentWhiteSpace.PRE_WRAP
            else -> BookDocumentWhiteSpace.NORMAL
        },
        foregroundArgb = attr("data-katari-color").toArgbLong(),
        backgroundArgb = attr("data-katari-background").toArgbLong(),
        border = border,
        paddingEm = attr("data-katari-padding-em").toFloatOrNull()?.coerceIn(0f, 4f) ?: 0f,
        fontFamily = fontFamily,
        fontSizeScale = attr("data-katari-font-scale").toFloatOrNull()?.coerceIn(0.75f, 1.5f) ?: 1f,
        bold = attr("data-katari-bold") == "true",
    )
}

internal fun Element.documentInlineStyle(): BookDocumentInlineStyle? {
    val style = documentStyle()
    val foreground = style.foregroundArgb.takeIf { hasAttr("data-katari-color") }
    val background = style.backgroundArgb.takeIf { hasAttr("data-katari-background") }
    val fontFamily = style.fontFamily.takeIf {
        hasAttr("data-katari-font-resource") || hasAttr("data-katari-font-generic")
    }
    val fontSizeScale = style.fontSizeScale.takeIf { hasAttr("data-katari-font-scale") }
    val bold = hasAttr("data-katari-bold") && style.bold
    if (foreground == null && background == null && fontFamily == null && fontSizeScale == null && !bold) {
        return null
    }
    return BookDocumentInlineStyle(
        foregroundArgb = foreground,
        backgroundArgb = background,
        fontFamily = fontFamily,
        fontSizeScale = fontSizeScale,
        bold = bold,
    )
}

internal fun BookDocumentInlineStyleRange.shifted(offset: Int) =
    copy(start = start + offset, endExclusive = endExclusive + offset)

internal fun BookDocumentInlineStyleRange.clippedAndShifted(
    sliceStart: Int,
    sliceEndExclusive: Int,
): BookDocumentInlineStyleRange? {
    val clippedStart = maxOf(start, sliceStart)
    val clippedEnd = minOf(endExclusive, sliceEndExclusive)
    if (clippedEnd <= clippedStart) return null
    return copy(
        start = clippedStart - sliceStart,
        endExclusive = clippedEnd - sliceStart,
    )
}

internal fun Element.removeDocumentStyleAttributes() {
    DOCUMENT_STYLE_ATTRIBUTES.forEach(::removeAttr)
}

internal fun BookDocumentStyle.merge(child: BookDocumentStyle): BookDocumentStyle = BookDocumentStyle(
    alignment = child.alignment ?: alignment,
    whiteSpace = child.whiteSpace.takeUnless { it == BookDocumentWhiteSpace.NORMAL } ?: whiteSpace,
    foregroundArgb = child.foregroundArgb ?: foregroundArgb,
    backgroundArgb = child.backgroundArgb ?: backgroundArgb,
    border = child.border ?: border,
    paddingEm = child.paddingEm.takeUnless { it == 0f } ?: paddingEm,
    fontFamily = child.fontFamily ?: fontFamily,
    fontSizeScale = child.fontSizeScale.takeUnless { it == 1f } ?: fontSizeScale,
    bold = child.bold || bold,
)

internal fun BookDocumentStyle.isMeaningBearingPanel(): Boolean =
    backgroundArgb != null || border != null
