package mihon.entry.interactions.book.format.html.prosechapter.style

import mihon.book.api.document.BookDocumentBoxStyle
import mihon.book.api.document.BookDocumentFloat
import mihon.book.api.document.BookDocumentGridTrack
import mihon.book.api.document.BookDocumentInsets
import mihon.book.api.document.BookDocumentItemAlignment
import mihon.book.api.document.BookDocumentItemSizing
import mihon.book.api.document.BookDocumentJustification
import mihon.book.api.document.BookDocumentSideBySideLayout
import mihon.entry.interactions.book.format.html.prosechapter.css.CssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.cssComponents
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.splitTopLevel
import org.jsoup.nodes.Element

/**
 * The side-by-side layout of a flex row or a grid of more than one column, or null for an element whose children
 * stack: a block element, a flex column or a one-column grid.
 */
internal fun Element.sideBySideLayout(): BookDocumentSideBySideLayout? {
    val properties = documentStyleProperties()
    val columnGap = properties["column-gap"].toGapEm()
    val rowGap = properties["row-gap"].toGapEm()
    val alignItems = properties["align-items"].toItemAlignment()
    return when (properties["display"]?.trim()?.lowercase()) {
        "flex", "inline-flex" -> {
            val direction = properties["flex-direction"]?.trim()?.lowercase() ?: "row"
            if (direction != "row" && direction != "row-reverse") return null
            BookDocumentSideBySideLayout.Flex(
                wrap = properties["flex-wrap"]?.trim()?.lowercase().let { it == "wrap" || it == "wrap-reverse" },
                reversed = direction == "row-reverse",
                justify = properties["justify-content"].toJustification(),
                alignItems = alignItems,
                columnGapEm = columnGap,
                rowGapEm = rowGap,
            )
        }
        "grid", "inline-grid" -> {
            val template = properties["grid-template-columns"]?.lowercase() ?: return null
            val autoFill = template.autoFillMinEm()
            val columns = if (autoFill == null) template.gridTracks() else emptyList()
            if (autoFill == null && columns.size < 2) return null
            BookDocumentSideBySideLayout.Grid(
                columns = columns,
                autoFillMinEm = autoFill,
                alignItems = alignItems,
                columnGapEm = columnGap,
                rowGapEm = rowGap,
            )
        }
        else -> null
    }
}

/** Width rules of an element as an item of a side-by-side row. */
internal fun Element.itemSizing(): BookDocumentItemSizing {
    val properties = documentStyleProperties()
    val basis = properties["flex-basis"]?.trim()?.lowercase()
        ?.takeUnless { it == "auto" || it == "content" }
        ?: properties["width"]
    val basisLength = parseCssLength(basis)
    val start = properties["grid-column-start"]?.trim()?.lowercase()
    val end = properties["grid-column-end"]?.trim()?.lowercase()
    val column = start?.toIntOrNull()?.takeIf { it > 0 }?.minus(1)
    val span = start.span() ?: end.span() ?: end?.toIntOrNull()?.let { endLine ->
        column?.let { endLine - 1 - it }
    } ?: 1
    return BookDocumentItemSizing(
        grow = properties["flex-grow"]?.trim()?.toFloatOrNull()?.coerceIn(0f, MAX_FLEX_FACTOR) ?: 0f,
        shrink = properties["flex-shrink"]?.trim()?.toFloatOrNull()?.coerceIn(0f, MAX_FLEX_FACTOR) ?: 1f,
        basisEm = basisLength?.toEm()?.coerceIn(0f, MAX_LENGTH_EM),
        basisFraction = (basisLength as? CssLength.Percent)?.value?.div(100f)?.coerceIn(0f, 1f),
        column = column?.takeIf { it < MAX_TRACKS },
        columnSpan = span.coerceIn(1, MAX_TRACKS),
    )
}

/** Whether text after this element wraps around it, and how wide it is. */
internal fun Element.documentFloat(): BookDocumentFloat? {
    val properties = documentStyleProperties()
    val side = when (properties["float"]?.trim()?.lowercase()) {
        "left", "inline-start" -> BookDocumentFloat.Side.LEFT
        "right", "inline-end" -> BookDocumentFloat.Side.RIGHT
        else -> return null
    }
    val width = parseCssLength(properties["width"])
    return BookDocumentFloat(
        side = side,
        widthEm = width?.toEm()?.coerceIn(1f, MAX_LENGTH_EM),
        widthFraction = (width as? CssLength.Percent)?.value?.div(100f)?.coerceIn(MIN_FLOAT_FRACTION, 1f),
    )
}

/** The part of a floated element's box drawn inside its slot, or null when it draws nothing there. */
internal fun BookDocumentBoxStyle.insideFloat(): BookDocumentBoxStyle? = copy(
    marginEm = marginEm.copy(left = 0f, right = 0f),
    centered = false,
    maxWidthEm = null,
    widthFraction = null,
    fitContent = false,
).takeIf { it.decorated || it.paddingEm != BookDocumentInsets() }

internal fun Element.clearsFloats(): Boolean =
    documentStyleProperties()["clear"]?.trim()?.lowercase() in
        setOf("left", "right", "both", "inline-start", "inline-end")

private fun String.gridTracks(): List<BookDocumentGridTrack> = expandedRepeats().mapNotNull { track ->
    when {
        track.startsWith("minmax(") -> track.functionArguments().lastOrNull()?.toTrack()
        track.startsWith("fit-content(") -> BookDocumentGridTrack.Auto
        else -> track.toTrack()
    }
}.take(MAX_TRACKS)

// "repeat(auto-fill, minmax(150px, 1fr))" and its auto-fit form: as many columns of at least the minimum as fit.
private fun String.autoFillMinEm(): Float? {
    val repeat = cssComponents().singleOrNull()?.takeIf { it.startsWith("repeat(") } ?: return null
    val (count, track) = repeat.functionArguments().takeIf { it.size == 2 } ?: return null
    if (count != "auto-fill" && count != "auto-fit") return null
    val minimum = if (track.startsWith("minmax(")) track.functionArguments().firstOrNull() else track
    return parseCssLength(minimum)?.toEm()?.coerceIn(1f, MAX_LENGTH_EM)
}

private fun String.expandedRepeats(): List<String> = cssComponents().flatMap { component ->
    if (!component.startsWith("repeat(")) return@flatMap listOf(component)
    val arguments = component.functionArguments()
    val count = arguments.firstOrNull()?.toIntOrNull()?.coerceIn(1, MAX_TRACKS) ?: return@flatMap emptyList()
    val tracks = arguments.drop(1).joinToString(",").cssComponents()
    List(count) { tracks }.flatten()
}

private fun String.toTrack(): BookDocumentGridTrack? = when {
    endsWith("fr") -> removeSuffix("fr").toFloatOrNull()?.let {
        BookDocumentGridTrack.Flexible(it.coerceIn(0f, MAX_FLEX_FACTOR))
    }
    this in setOf("auto", "min-content", "max-content") -> BookDocumentGridTrack.Auto
    else -> when (val length = parseCssLength(this)) {
        is CssLength.Percent -> BookDocumentGridTrack.OfRow((length.value / 100f).coerceIn(0f, 1f))
        null -> null
        else -> length.toEm()?.let { BookDocumentGridTrack.Fixed(it.coerceIn(0f, MAX_LENGTH_EM)) }
    }
}

private fun String.functionArguments(): List<String> =
    substringAfter('(').substringBeforeLast(')').splitTopLevel(',').map(String::trim)

private fun String?.span(): Int? = this?.takeIf { it.startsWith("span") }?.removePrefix("span")?.trim()?.toIntOrNull()

private fun String?.toGapEm(): Float = parseCssLength(this)?.toEm()?.coerceIn(0f, MAX_GAP_EM) ?: 0f

private fun String?.toJustification(): BookDocumentJustification = when (this?.trim()?.lowercase()) {
    "center" -> BookDocumentJustification.CENTER
    "end", "flex-end", "right" -> BookDocumentJustification.END
    "space-between" -> BookDocumentJustification.SPACE_BETWEEN
    "space-around" -> BookDocumentJustification.SPACE_AROUND
    "space-evenly" -> BookDocumentJustification.SPACE_EVENLY
    else -> BookDocumentJustification.START
}

private fun String?.toItemAlignment(): BookDocumentItemAlignment = when (this?.trim()?.lowercase()) {
    "start", "flex-start", "self-start", "baseline" -> BookDocumentItemAlignment.START
    "center" -> BookDocumentItemAlignment.CENTER
    "end", "flex-end", "self-end" -> BookDocumentItemAlignment.END
    else -> BookDocumentItemAlignment.STRETCH
}

private const val MAX_TRACKS = 24
private const val MAX_FLEX_FACTOR = 100f
private const val MAX_LENGTH_EM = 200f
private const val MAX_GAP_EM = 8f
private const val MIN_FLOAT_FRACTION = 0.05f
