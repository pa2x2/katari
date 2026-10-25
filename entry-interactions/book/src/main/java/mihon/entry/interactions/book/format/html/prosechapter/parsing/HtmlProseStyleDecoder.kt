package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentFlowStyle
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTextContext
import mihon.book.api.document.BookDocumentTextDirection
import mihon.book.api.document.BookDocumentWhiteSpace
import mihon.entry.interactions.book.format.html.prosechapter.css.DOCUMENT_STYLE_ATTRIBUTE_PREFIX
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssColor
import org.jsoup.nodes.Element

internal fun Element.documentBlockStyle(): BookDocumentStyle {
    val properties = documentStyleProperties()
    return BookDocumentStyle(
        alignment = properties["text-align"].toAlignment(),
        whiteSpace = properties["white-space"].toWhiteSpace(),
        foregroundArgb = properties["color"].toArgb(),
        backgroundArgb = properties["background-color"].toArgb(),
        border = properties.toBorder(),
        paddingEm = PADDING_SIDES.mapNotNull { properties[it].toEm() }.maxOrNull()?.coerceIn(0f, 4f) ?: 0f,
        fontFamily = properties.toFontFamily(),
        fontSizeScale = properties["font-size"].toFontScale() ?: 1f,
        bold = properties["font-weight"].isBold(),
        flow = BookDocumentFlowStyle(
            spacingBeforeEm = properties["margin-top"].toEm()?.coerceIn(0f, 8f) ?: 0f,
            spacingAfterEm = properties["margin-bottom"].toEm()?.coerceIn(0f, 8f) ?: 0f,
            lineHeightScale = properties["line-height"].toLineHeightScale() ?: 1.25f,
            firstLineIndentEm = properties["text-indent"].toEm()?.coerceIn(-8f, 8f) ?: 0f,
            direction = (properties["direction"] ?: attr("dir")).toTextDirection(),
            languageTag = declaredLanguageTag(),
        ),
    )
}

internal fun Element.documentInlineStyle(): BookDocumentInlineStyle? {
    val properties = documentStyleProperties()
    val tag = normalName()
    val decoration = properties["text-decoration-line"].orEmpty().lowercase()
    val vertical = properties["vertical-align"].orEmpty().lowercase()
    val foreground = properties["color"].toArgb()
    val background = properties["background-color"].toArgb()
    val fontFamily = properties.toFontFamily()
    val fontScale = properties["font-size"].toFontScale()
    val bold = tag in setOf("b", "strong") || properties["font-weight"].isBold()
    val italic = tag in setOf("i", "em", "cite", "dfn", "var") ||
        properties["font-style"].orEmpty().lowercase() in setOf("italic", "oblique")
    val underline = tag in setOf("u", "ins") || "underline" in decoration
    val strikethrough = tag in setOf("s", "del") || "line-through" in decoration
    val subscript = vertical == "sub" || (vertical != "super" && tag == "sub")
    val superscript = vertical == "super" || (vertical != "sub" && tag == "sup")
    val code = tag in setOf("code", "kbd", "samp")
    val small = tag == "small"
    val languageTag = declaredLanguageTag()
    val direction = attr("dir").toTextDirection()
    if (
        foreground == null && background == null && fontFamily == null && fontScale == null &&
        !bold && !italic && !underline && !strikethrough && !subscript && !superscript && !code && !small &&
        languageTag == null && direction == null
    ) {
        return null
    }
    return BookDocumentInlineStyle(
        foregroundArgb = foreground,
        backgroundArgb = background,
        fontFamily = fontFamily,
        fontSizeScale = fontScale,
        bold = bold,
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
        subscript = subscript,
        superscript = superscript,
        code = code,
        small = small,
        textContext = BookDocumentTextContext(languageTag, direction),
    )
}

internal fun BookDocumentStyle.mergedWith(child: BookDocumentStyle): BookDocumentStyle {
    return BookDocumentStyle(
        alignment = child.alignment ?: alignment,
        whiteSpace = if (child.whiteSpace == BookDocumentWhiteSpace.NORMAL) whiteSpace else child.whiteSpace,
        foregroundArgb = child.foregroundArgb ?: foregroundArgb,
        backgroundArgb = child.backgroundArgb ?: backgroundArgb,
        border = child.border ?: border,
        paddingEm = if (child.paddingEm == 0f) paddingEm else child.paddingEm,
        fontFamily = child.fontFamily ?: fontFamily,
        fontSizeScale = if (child.fontSizeScale == 1f) fontSizeScale else child.fontSizeScale,
        bold = bold || child.bold,
        flow = BookDocumentFlowStyle(
            spacingBeforeEm = if (child.spacingBeforeEm == 0f) spacingBeforeEm else child.spacingBeforeEm,
            spacingAfterEm = if (child.spacingAfterEm == 0f) spacingAfterEm else child.spacingAfterEm,
            lineHeightScale = if (child.lineHeightScale == 1.25f) lineHeightScale else child.lineHeightScale,
            firstLineIndentEm = if (child.firstLineIndentEm == 0f) firstLineIndentEm else child.firstLineIndentEm,
            direction = child.direction ?: direction,
            languageTag = child.languageTag ?: languageTag,
        ),
    )
}

private fun Element.documentStyleProperties(): Map<String, String> = buildMap {
    attributes().asList().forEach { attribute ->
        if (attribute.key.startsWith(DOCUMENT_STYLE_ATTRIBUTE_PREFIX)) {
            put(attribute.key.removePrefix(DOCUMENT_STYLE_ATTRIBUTE_PREFIX), attribute.value)
        }
    }
}

private fun String?.toAlignment(): BookDocumentAlignment? = when (this?.trim()?.lowercase()) {
    "left", "start" -> BookDocumentAlignment.START
    "center" -> BookDocumentAlignment.CENTER
    "right", "end" -> BookDocumentAlignment.END
    else -> null
}

private fun String?.toWhiteSpace(): BookDocumentWhiteSpace = when (this?.trim()?.lowercase()) {
    "pre" -> BookDocumentWhiteSpace.PRE
    "pre-wrap", "break-spaces" -> BookDocumentWhiteSpace.PRE_WRAP
    else -> BookDocumentWhiteSpace.NORMAL
}

private fun String?.toTextDirection(): BookDocumentTextDirection? = when (this?.trim()?.lowercase()) {
    "ltr" -> BookDocumentTextDirection.LEFT_TO_RIGHT
    "rtl" -> BookDocumentTextDirection.RIGHT_TO_LEFT
    else -> null
}

private fun Element.declaredLanguageTag(): String? = attr("lang")
    .ifBlank { attr("xml:lang") }
    .trim()
    .take(64)
    .takeIf(String::isNotEmpty)

private fun String?.toLineHeightScale(): Float? {
    val value = this?.trim()?.lowercase() ?: return null
    val scale = when {
        value.endsWith("%") -> value.removeSuffix("%").toFloatOrNull()?.div(100f)
        value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()
        else -> value.toFloatOrNull()
    }
    return scale?.coerceIn(0.8f, 3f)
}

private fun String?.toGenericFontFamily(): BookDocumentFontFamily.Generic? {
    val family = this?.lowercase()?.split(',')?.map(String::trim)?.firstNotNullOfOrNull { value ->
        when (value.trim('"', '\'')) {
            "serif" -> BookDocumentFontFamily.GenericFamily.SERIF
            "sans-serif", "system-ui" -> BookDocumentFontFamily.GenericFamily.SANS_SERIF
            "monospace" -> BookDocumentFontFamily.GenericFamily.MONOSPACE
            else -> null
        }
    } ?: return null
    return BookDocumentFontFamily.Generic(family)
}

private fun Map<String, String>.toFontFamily(): BookDocumentFontFamily? =
    get("font-resource")?.trim()?.takeIf(String::isNotEmpty)?.let(BookDocumentFontFamily::Resource)
        ?: get("font-family").toGenericFontFamily()

private fun String?.isBold(): Boolean {
    val value = this?.trim()?.lowercase() ?: return false
    return value in setOf("bold", "bolder") || value.toIntOrNull()?.let { it >= 600 } == true
}

private fun String?.toFontScale(): Float? {
    val value = this?.trim()?.lowercase() ?: return null
    val scale = when {
        value.endsWith("rem") -> value.removeSuffix("rem").toFloatOrNull()
        value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()
        value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()?.div(16f)
        value.endsWith("%") -> value.removeSuffix("%").toFloatOrNull()?.div(100f)
        value == "small" -> 0.85f
        value == "large" -> 1.2f
        else -> null
    }
    return scale?.coerceIn(0.75f, 1.5f)
}

private fun String?.toEm(): Float? {
    val value = this?.trim()?.substringBefore(' ')?.lowercase() ?: return null
    return when {
        value.endsWith("rem") -> value.removeSuffix("rem").toFloatOrNull()
        value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()
        value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()?.div(16f)
        value == "0" -> 0f
        else -> null
    }
}

private fun Map<String, String>.toBorder(): BookDocumentBorder? {
    val side = BORDER_SIDES.firstOrNull { side -> get("border-$side-style").toBorderStyle() != null } ?: return null
    val style = get("border-$side-style").toBorderStyle() ?: return null
    val width = get("border-$side-width").toDp()?.coerceIn(0.5f, 8f) ?: 3f
    return BookDocumentBorder(width, get("border-$side-color").toArgb(), style)
}

private fun String?.toBorderStyle(): BookDocumentBorderStyle? = when (this?.trim()?.lowercase()) {
    "solid", "double", "groove", "ridge", "inset", "outset" -> BookDocumentBorderStyle.SOLID
    "dashed" -> BookDocumentBorderStyle.DASHED
    "dotted" -> BookDocumentBorderStyle.DOTTED
    else -> null
}

private fun String?.toDp(): Float? {
    val value = this?.trim()?.lowercase() ?: return null
    return when {
        value.endsWith("px") -> value.removeSuffix("px").toFloatOrNull()
        value.endsWith("pt") -> value.removeSuffix("pt").toFloatOrNull()?.times(4f / 3f)
        value == "thin" -> 1f
        value == "medium" -> 3f
        value == "thick" -> 5f
        else -> null
    }
}

private fun String?.toArgb(): Long? = parseCssColor(this)

private val PADDING_SIDES = listOf("padding-top", "padding-right", "padding-bottom", "padding-left")
private val BORDER_SIDES = listOf("top", "right", "bottom", "left")
