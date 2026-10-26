package mihon.entry.interactions.book.format.html.prosechapter.style

import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentFlowStyle
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTextContext
import mihon.book.api.document.BookDocumentTextDirection
import mihon.book.api.document.BookDocumentTextTransform
import mihon.book.api.document.BookDocumentWhiteSpace
import mihon.entry.interactions.book.format.html.prosechapter.css.CssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.DOCUMENT_STYLE_ATTRIBUTE_PREFIX
import mihon.entry.interactions.book.format.html.prosechapter.css.isTransparentColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssLength
import org.jsoup.nodes.Element

/**
 * Resolves the block style of [element] inside a parent styled `this`: inherited CSS properties fall back to the
 * parent's value when the element doesn't declare them, margins don't inherit.
 */
internal fun BookDocumentStyle.resolvedFor(element: Element): BookDocumentStyle {
    val properties = element.documentStyleProperties()
    return BookDocumentStyle(
        alignment = properties["text-align"].toAlignment() ?: alignment,
        whiteSpace = properties["white-space"].toWhiteSpace() ?: whiteSpace,
        foregroundArgb = properties["color"].toArgb() ?: foregroundArgb,
        fontFamily = properties.toFontFamily() ?: fontFamily,
        fontSizeScale = properties["font-size"].toFontScale() ?: fontSizeScale,
        bold = properties["font-weight"].toBold() ?: bold,
        italic = properties["font-style"].toItalic() ?: italic,
        letterSpacingEm = properties["letter-spacing"].toLetterSpacingEm() ?: letterSpacingEm,
        textTransform = properties["text-transform"].toTextTransform(textTransform),
        smallCaps = properties["font-variant"].toSmallCaps() ?: smallCaps,
        textShadow = properties["text-shadow"]?.toTextShadow() ?: textShadow.takeIf { "text-shadow" !in properties },
        flow = BookDocumentFlowStyle(
            spacingBeforeEm = properties["margin-top"].toSpacingEm(),
            spacingAfterEm = properties["margin-bottom"].toSpacingEm(),
            lineHeightScale = properties["line-height"].toLineHeightScale() ?: lineHeightScale,
            firstLineIndentEm = parseCssLength(properties["text-indent"])?.toEm()?.coerceIn(-8f, 8f)
                ?: firstLineIndentEm,
            direction = (properties["direction"] ?: element.attr("dir")).toTextDirection() ?: direction,
            languageTag = element.declaredLanguageTag() ?: languageTag,
        ),
    )
}

/** The style without the spacing around the block, for blocks whose element's margins belong to a box. */
internal fun BookDocumentStyle.withoutSpacing(): BookDocumentStyle =
    copy(flow = flow.copy(spacingBeforeEm = 0f, spacingAfterEm = 0f))

internal fun Element.documentInlineStyle(): BookDocumentInlineStyle? {
    val properties = documentStyleProperties()
    val tag = normalName()
    val decoration = properties["text-decoration-line"].orEmpty().lowercase()
    val vertical = properties["vertical-align"].orEmpty().lowercase()
    val foreground = properties["color"].toArgb()
    val background = properties["background-color"].toArgb()?.takeUnless(Long::isTransparentColor)
    val fontFamily = properties.toFontFamily()
    val fontScale = properties["font-size"].toFontScale()
    val bold = tag in setOf("b", "strong") || properties["font-weight"].toBold() == true
    val italic = tag in setOf("i", "em", "cite", "dfn", "var") || properties["font-style"].toItalic() == true
    val underline = tag in setOf("u", "ins") || "underline" in decoration
    val strikethrough = tag in setOf("s", "del") || "line-through" in decoration
    val subscript = vertical == "sub" || (vertical != "super" && tag == "sub")
    val superscript = vertical == "super" || (vertical != "sub" && tag == "sup")
    val code = tag in setOf("code", "kbd", "samp")
    val small = tag == "small"
    val languageTag = declaredLanguageTag()
    val direction = attr("dir").toTextDirection()
    val letterSpacing = properties["letter-spacing"].toLetterSpacingEm()
    val textTransform = properties["text-transform"].toTextTransform(null)
    val smallCaps = properties["font-variant"].toSmallCaps() == true
    val textShadow = properties["text-shadow"].toTextShadow()
    if (
        foreground == null && background == null && fontFamily == null && fontScale == null &&
        !bold && !italic && !underline && !strikethrough && !subscript && !superscript && !code && !small &&
        letterSpacing == null && textTransform == null && !smallCaps && textShadow == null &&
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
        letterSpacingEm = letterSpacing,
        textTransform = textTransform,
        smallCaps = smallCaps,
        textShadow = textShadow,
        textContext = BookDocumentTextContext(languageTag, direction),
    )
}

internal fun Element.documentStyleProperties(): Map<String, String> = buildMap {
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

private fun String?.toWhiteSpace(): BookDocumentWhiteSpace? = when (this?.trim()?.lowercase()) {
    "pre" -> BookDocumentWhiteSpace.PRE
    "pre-wrap", "break-spaces" -> BookDocumentWhiteSpace.PRE_WRAP
    "normal", "nowrap", "pre-line" -> BookDocumentWhiteSpace.NORMAL
    else -> null
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

private fun String?.toBold(): Boolean? {
    val value = this?.trim()?.lowercase() ?: return null
    return when {
        value in setOf("bold", "bolder") -> true
        value in setOf("normal", "lighter") -> false
        else -> value.toIntOrNull()?.let { it >= 600 }
    }
}

private fun String?.toItalic(): Boolean? = when (this?.trim()?.lowercase()?.substringBefore(' ')) {
    "italic", "oblique" -> true
    "normal" -> false
    else -> null
}

private fun String?.toSmallCaps(): Boolean? = when (this?.trim()?.lowercase()) {
    null -> null
    "small-caps", "all-small-caps", "petite-caps", "all-petite-caps" -> true
    else -> false
}

// "none" ends an inherited transform; an unknown value keeps it.
private fun String?.toTextTransform(inherited: BookDocumentTextTransform?): BookDocumentTextTransform? =
    when (this?.trim()?.lowercase()) {
        "uppercase" -> BookDocumentTextTransform.UPPERCASE
        "lowercase" -> BookDocumentTextTransform.LOWERCASE
        "capitalize" -> BookDocumentTextTransform.CAPITALIZE
        "none" -> null
        else -> inherited
    }

private fun String?.toLetterSpacingEm(): Float? {
    if (this?.trim().equals("normal", ignoreCase = true)) return 0f
    return parseCssLength(this)?.toEm()?.coerceIn(-0.5f, 2f)
}

private fun String?.toSpacingEm(): Float = parseCssLength(this)?.toEm()?.coerceIn(0f, 8f) ?: 0f

private fun String?.toFontScale(): Float? {
    val value = this?.trim()?.lowercase() ?: return null
    val scale = when (value) {
        "small" -> 0.85f
        "large" -> 1.2f
        else -> when (val length = parseCssLength(value)) {
            is CssLength.Percent -> length.value / 100f
            else -> length?.toEm()
        }
    }
    return scale?.coerceIn(0.75f, 1.5f)
}

private fun String?.toArgb(): Long? = parseCssColor(this)
