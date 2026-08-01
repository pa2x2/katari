package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentWhiteSpace
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.DOCUMENT_STYLE_ATTRIBUTE_PREFIX
import org.jsoup.nodes.Element

internal fun Element.documentBlockStyle(): BookDocumentStyle {
    val properties = documentStyleProperties()
    return BookDocumentStyle(
        alignment = properties["text-align"].toAlignment(),
        whiteSpace = properties["white-space"].toWhiteSpace(),
        foregroundArgb = properties["color"].toArgb(),
        backgroundArgb = properties["background-color"].toArgb(),
        border = properties.toBorder(),
        paddingEm = properties["padding"].toEm()?.coerceIn(0f, 4f) ?: 0f,
        fontFamily = properties["font-family"].toGenericFontFamily(),
        fontSizeScale = properties["font-size"].toFontScale() ?: 1f,
        bold = properties["font-weight"].isBold(),
    )
}

internal fun Element.documentInlineStyle(): BookDocumentInlineStyle? {
    val properties = documentStyleProperties()
    val tag = normalName()
    val decoration = properties["text-decoration"].orEmpty().lowercase()
    val vertical = properties["vertical-align"].orEmpty().lowercase()
    val foreground = properties["color"].toArgb()
    val background = properties["background-color"].toArgb()
    val fontFamily = properties["font-family"].toGenericFontFamily()
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
    if (
        foreground == null && background == null && fontFamily == null && fontScale == null &&
        !bold && !italic && !underline && !strikethrough && !subscript && !superscript && !code && !small
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
    )
}

internal fun BookDocumentStyle.mergedWith(child: BookDocumentStyle): BookDocumentStyle = BookDocumentStyle(
    alignment = child.alignment ?: alignment,
    whiteSpace = if (child.whiteSpace == BookDocumentWhiteSpace.NORMAL) whiteSpace else child.whiteSpace,
    foregroundArgb = child.foregroundArgb ?: foregroundArgb,
    backgroundArgb = child.backgroundArgb ?: backgroundArgb,
    border = child.border ?: border,
    paddingEm = if (child.paddingEm == 0f) paddingEm else child.paddingEm,
    fontFamily = child.fontFamily ?: fontFamily,
    fontSizeScale = if (child.fontSizeScale == 1f) fontSizeScale else child.fontSizeScale,
    bold = bold || child.bold,
)

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

private fun String?.isBold(): Boolean {
    val value = this?.trim()?.lowercase() ?: return false
    return value in setOf("bold", "bolder") || value.toIntOrNull()?.let { it >= 600 } == true
}

private fun String?.toFontScale(): Float? {
    val value = this?.trim()?.lowercase() ?: return null
    val scale = when {
        value.endsWith("em") -> value.removeSuffix("em").toFloatOrNull()
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
    val style = when (get("border-style")?.trim()?.lowercase()) {
        "solid" -> BookDocumentBorderStyle.SOLID
        "dashed" -> BookDocumentBorderStyle.DASHED
        "dotted" -> BookDocumentBorderStyle.DOTTED
        else -> return null
    }
    val width = get("border-width").toDp()?.coerceIn(0.5f, 8f) ?: 1f
    return BookDocumentBorder(width, get("border-color").toArgb(), style)
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

private fun String?.toArgb(): Long? {
    val value = this?.trim()?.lowercase() ?: return null
    NAMED_COLORS[value]?.let { return it }
    if (!value.startsWith('#')) return null
    return when (value.length) {
        4 -> {
            val red = value[1].digitToIntOrNull(16) ?: return null
            val green = value[2].digitToIntOrNull(16) ?: return null
            val blue = value[3].digitToIntOrNull(16) ?: return null
            (0xFF000000L or (red * 17L shl 16) or (green * 17L shl 8) or (blue * 17L))
        }
        7 -> value.drop(1).toLongOrNull(16)?.let { 0xFF000000L or it }
        9 -> {
            val rgb = value.substring(1, 7).toLongOrNull(16) ?: return null
            val alpha = value.substring(7, 9).toLongOrNull(16) ?: return null
            (alpha shl 24) or rgb
        }
        else -> null
    }
}

private val NAMED_COLORS = mapOf(
    "black" to 0xFF000000L,
    "white" to 0xFFFFFFFFL,
    "red" to 0xFFFF0000L,
    "green" to 0xFF008000L,
    "blue" to 0xFF0000FFL,
    "gray" to 0xFF808080L,
    "grey" to 0xFF808080L,
    "transparent" to 0x00000000L,
)
