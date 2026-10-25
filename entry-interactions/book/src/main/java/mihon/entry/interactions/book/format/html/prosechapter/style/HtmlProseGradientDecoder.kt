package mihon.entry.interactions.book.format.html.prosechapter.style

import mihon.book.api.document.BookDocumentGradient
import mihon.entry.interactions.book.format.html.prosechapter.css.CssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.cssComponents
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssColor
import mihon.entry.interactions.book.format.html.prosechapter.css.parseCssLength
import mihon.entry.interactions.book.format.html.prosechapter.css.splitTopLevel

/**
 * Decodes the topmost gradient of a `background-image`. Radial gradients keep their colours and drop their shape,
 * size and centre, which a reading page has no room to honour.
 */
internal fun String?.toGradient(): BookDocumentGradient? {
    val value = this?.lowercase() ?: return null
    val layer = value.firstGradientLayer() ?: return null
    val function = layer.substringBefore('(').trim()
    val kind = when (function.removePrefix("repeating-")) {
        "linear-gradient" -> BookDocumentGradient.Kind.LINEAR
        "radial-gradient" -> BookDocumentGradient.Kind.RADIAL
        else -> return null
    }
    val arguments = layer.substringAfter('(').substringBeforeLast(')').splitTopLevel(',').map(String::trim)
    val angle = if (kind == BookDocumentGradient.Kind.LINEAR) arguments.firstOrNull()?.toGradientAngle() else null
    val leading = arguments.firstOrNull()?.let { first -> angle != null || first.toStops() == null } == true
    val stops = arguments.drop(if (leading) 1 else 0).flatMap { argument -> argument.toStops().orEmpty() }
        .take(MAX_GRADIENT_STOPS)
    if (stops.size < 2) return null
    return BookDocumentGradient(
        kind = kind,
        angleDegrees = angle ?: DEFAULT_ANGLE,
        stops = stops,
        repeating = function.startsWith("repeating-"),
    )
}

// The shorthand joins layers with ", ", so a gradient's own commas need the parenthesis-aware split.
private fun String.firstGradientLayer(): String? = splitTopLevel(',').map(String::trim).firstOrNull {
    "gradient(" in it
}

// A stop is a colour with up to two positions; a bare position between stops is a hint, which is dropped.
private fun String.toStops(): List<BookDocumentGradient.Stop>? {
    val components = cssComponents()
    val color = components.firstOrNull()?.let(::parseCssColor) ?: return null
    val positions = components.drop(1).map { component -> (parseCssLength(component) as? CssLength.Percent)?.value }
    if (positions.isEmpty()) return listOf(BookDocumentGradient.Stop(color))
    return positions.take(2).map { position -> BookDocumentGradient.Stop(color, position?.div(100f)) }
}

private fun String.toGradientAngle(): Float? {
    if (startsWith("to ")) return SIDE_ANGLES[removePrefix("to ").split(' ').filter(String::isNotEmpty).sorted()]
    val unitStart = indexOfFirst(Char::isLetter).takeIf { it > 0 } ?: return null
    val number = substring(0, unitStart).toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    val degrees = when (substring(unitStart)) {
        "deg" -> number
        "grad" -> number * 0.9f
        "rad" -> Math.toDegrees(number.toDouble()).toFloat()
        "turn" -> number * 360f
        else -> return null
    }
    return ((degrees % 360f) + 360f) % 360f
}

private val SIDE_ANGLES = mapOf(
    listOf("top") to 0f,
    listOf("right", "top") to 45f,
    listOf("right") to 90f,
    listOf("bottom", "right") to 135f,
    listOf("bottom") to 180f,
    listOf("bottom", "left") to 225f,
    listOf("left") to 270f,
    listOf("left", "top") to 315f,
)

private const val DEFAULT_ANGLE = 180f
private const val MAX_GRADIENT_STOPS = 16
