package mihon.entry.interactions.book.format.html.prosechapter.style

import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentBoxBorders
import mihon.book.api.document.BookDocumentBoxStyle
import mihon.book.api.document.BookDocumentCorners
import mihon.book.api.document.BookDocumentInsets
import mihon.entry.interactions.book.format.html.prosechapter.css.CssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.cssComponents
import mihon.entry.interactions.book.format.html.prosechapter.css.isTransparentColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssLength
import org.jsoup.nodes.Element

/**
 * Decodes the box an element draws or the room it takes, or null when it has neither.
 *
 * Vertical margins of a [container] always belong to its box, because its blocks come from its children. A leaf
 * element's vertical margins stay block spacing unless it has a box for another reason.
 */
internal fun Element.documentBoxStyle(container: Boolean): BookDocumentBoxStyle? {
    val properties = documentStyleProperties()
    val margins = SIDES.associateWith { side -> properties["margin-$side"] }
    val centered = margins["left"].isAuto() && margins["right"].isAuto()
    val width = parseCssLength(properties["width"])
    val maxWidth = parseCssLength(properties["max-width"])
    val style = BookDocumentBoxStyle(
        backgroundArgb = properties["background-color"].toArgb()?.takeUnless(Long::isTransparentColor),
        backgroundGradient = properties["background-image"].toGradient(),
        borders = BookDocumentBoxBorders(
            top = properties.toBorder("top"),
            right = properties.toBorder("right"),
            bottom = properties.toBorder("bottom"),
            left = properties.toBorder("left"),
        ),
        cornerRadiiDp = BookDocumentCorners(
            topLeft = properties["border-top-left-radius"].toRadius(),
            topRight = properties["border-top-right-radius"].toRadius(),
            bottomRight = properties["border-bottom-right-radius"].toRadius(),
            bottomLeft = properties["border-bottom-left-radius"].toRadius(),
        ),
        paddingEm = SIDES.map { side -> properties["padding-$side"].toSpacingEm() }.toInsets(),
        marginEm = SIDES.map { side -> margins[side].toSpacingEm() }.toInsets(),
        centered = centered,
        maxWidthEm = listOfNotNull(width?.toEm(), maxWidth?.toEm()).minOrNull()?.coerceIn(MIN_WIDTH_EM, MAX_WIDTH_EM),
        widthFraction = listOfNotNull(width.fraction(), maxWidth.fraction()).minOrNull()
            ?.takeIf { it < 1f }
            ?.coerceAtLeast(MIN_FRACTION),
        fitContent = properties["width"]?.lowercase() in FIT_CONTENT_WIDTHS ||
            properties["display"]?.lowercase() in FIT_CONTENT_DISPLAYS,
        shadows = properties["box-shadow"].toBoxShadows(MAX_SHADOWS),
    )
    val takesRoom = style.paddingEm != BookDocumentInsets() ||
        style.marginEm.left > 0f || style.marginEm.right > 0f ||
        style.centered || style.maxWidthEm != null || style.widthFraction != null || style.fitContent ||
        (container && (style.marginEm.top > 0f || style.marginEm.bottom > 0f))
    return style.takeIf { it.decorated || takesRoom }
}

private fun Map<String, String>.toBorder(side: String): BookDocumentBorder? {
    val style = when (get("border-$side-style")?.trim()?.lowercase()) {
        "solid" -> BookDocumentBorderStyle.SOLID
        "dashed" -> BookDocumentBorderStyle.DASHED
        "dotted" -> BookDocumentBorderStyle.DOTTED
        "double" -> BookDocumentBorderStyle.DOUBLE
        "groove" -> BookDocumentBorderStyle.GROOVE
        "ridge" -> BookDocumentBorderStyle.RIDGE
        "inset" -> BookDocumentBorderStyle.INSET
        "outset" -> BookDocumentBorderStyle.OUTSET
        else -> return null
    }
    val width = when (val value = get("border-$side-width")?.trim()?.lowercase()) {
        null, "medium" -> MEDIUM_BORDER_DP
        "thin" -> THIN_BORDER_DP
        "thick" -> THICK_BORDER_DP
        else -> parseCssLength(value)?.toDp() ?: MEDIUM_BORDER_DP
    }
    if (width <= 0f) return null
    val color = get("border-$side-color").toArgb()
    if (color?.isTransparentColor() == true) return null
    return BookDocumentBorder(width.coerceIn(MIN_BORDER_DP, MAX_BORDER_DP), color, style)
}

// An elliptical corner ("8px 4px") is drawn circular with its horizontal radius.
private fun String?.toRadius(): Float =
    this?.cssComponents()?.firstOrNull()?.let(::parseCssLength)?.toDp()?.coerceIn(0f, MAX_RADIUS_DP) ?: 0f

private fun String?.toSpacingEm(): Float = parseCssLength(this)?.toEm()?.coerceIn(0f, MAX_SPACING_EM) ?: 0f

private fun String?.isAuto(): Boolean = this?.trim().equals("auto", ignoreCase = true)

private fun CssLength?.fraction(): Float? = (this as? CssLength.Percent)?.value?.div(100f)

private fun List<Float>.toInsets() = BookDocumentInsets(top = get(0), right = get(1), bottom = get(2), left = get(3))

private fun String?.toArgb(): Long? = parseCssColor(this)

private val SIDES = listOf("top", "right", "bottom", "left")
private val FIT_CONTENT_WIDTHS = setOf("fit-content", "max-content", "min-content")
private val FIT_CONTENT_DISPLAYS = setOf("inline-block", "inline-flex", "inline-grid", "table", "inline-table")

private const val THIN_BORDER_DP = 1f
private const val MEDIUM_BORDER_DP = 3f
private const val THICK_BORDER_DP = 5f
private const val MIN_BORDER_DP = 0.5f
private const val MAX_BORDER_DP = 8f
private const val MAX_RADIUS_DP = 200f
private const val MAX_SPACING_EM = 8f
private const val MIN_WIDTH_EM = 1f
private const val MAX_WIDTH_EM = 200f
private const val MIN_FRACTION = 0.05f
private const val MAX_SHADOWS = 4
