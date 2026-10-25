package mihon.entry.interactions.book.format.html.prosechapter.css

/**
 * A CSS length reduced to the three kinds a book layout can honour: absolute lengths in CSS pixels, lengths relative
 * to the font size, and percentages of the containing block. Viewport units and `calc()` aren't lengths here.
 */
internal sealed interface CssLength {
    data class Px(val value: Float) : CssLength
    data class Em(val value: Float) : CssLength
    data class Percent(val value: Float) : CssLength

    /** In em of the book's base text size; a percentage has no em value without its container. */
    fun toEm(): Float? = when (this) {
        is Px -> value / CSS_PX_PER_EM
        is Em -> value
        is Percent -> null
    }

    /** In CSS pixels, which the reader draws as density-independent pixels. */
    fun toDp(): Float? = when (this) {
        is Px -> value
        is Em -> value * CSS_PX_PER_EM
        is Percent -> null
    }
}

internal fun parseCssLength(raw: String?): CssLength? {
    val value = raw?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
    if (value == "0") return CssLength.Px(0f)
    val unitStart = value.indexOfFirst { it.isLetter() || it == '%' }.takeIf { it > 0 } ?: return null
    val number = value.substring(0, unitStart).toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    return when (value.substring(unitStart)) {
        "px" -> CssLength.Px(number)
        "pt" -> CssLength.Px(number * 4f / 3f)
        "pc" -> CssLength.Px(number * 16f)
        "in" -> CssLength.Px(number * 96f)
        "cm" -> CssLength.Px(number * 96f / 2.54f)
        "mm" -> CssLength.Px(number * 96f / 25.4f)
        "q" -> CssLength.Px(number * 96f / 101.6f)
        "em", "rem" -> CssLength.Em(number)
        // Both are about half an em in the fonts books use.
        "ex", "ch" -> CssLength.Em(number / 2f)
        "%" -> CssLength.Percent(number)
        else -> null
    }
}

/** Default browser text size, which authored pixel sizes are relative to. */
internal const val CSS_PX_PER_EM = 16f
