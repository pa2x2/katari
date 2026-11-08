package mihon.entry.interactions.book.format.html.prosechapter.css

/**
 * Turns a declaration into the longhand properties Katari reads, expanding shorthands (`margin`, `border`,
 * `background`, `font`, `flex`, …) the way browsers do: a component left out of a shorthand resets its longhand.
 * Properties Katari doesn't read expand to nothing.
 */
internal object CssShorthands {
    fun expand(property: String, value: String): List<Pair<String, String>> {
        if (property in LONGHANDS) return listOf(property to value)
        val components = value.cssComponents()
        return when (property) {
            "margin", "padding" -> boxSides(components)?.let { sides ->
                SIDES.zip(sides) { side, v ->
                    "$property-$side" to
                        v
                }
            }
            "border" -> SIDES.flatMap { side -> border("border-$side", components) }
            "border-top", "border-right", "border-bottom", "border-left" -> border(property, components)
            "border-width", "border-style", "border-color" -> {
                val part = property.substringAfter("border-")
                boxSides(components)?.let { sides -> SIDES.zip(sides) { side, v -> "border-$side-$part" to v } }
            }
            "border-radius" -> {
                val horizontal = value.substringBefore('/').cssComponents()
                boxSides(horizontal)?.let { corners ->
                    CORNERS.zip(corners) { corner, v -> "border-$corner-radius" to v }
                }
            }
            "background" -> background(value)
            "font" -> font(components)
            "text-decoration" -> textDecoration(components)
            "flex" -> flex(components)
            "flex-flow" -> components.map { component ->
                if (component in FLEX_WRAPS) "flex-wrap" to component else "flex-direction" to component
            }
            "gap", "grid-gap" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("row-gap" to it.first(), "column-gap" to it.last())
            }
            "grid-column" -> value.split('/').map(String::trim).let { parts ->
                listOf("grid-column-start" to parts[0], "grid-column-end" to (parts.getOrNull(1) ?: "auto"))
            }
            "place-items" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("align-items" to it.first(), "justify-items" to it.last())
            }
            "place-content" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("align-content" to it.first(), "justify-content" to it.last())
            }
            "margin-inline" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("margin-left" to it.first(), "margin-right" to it.last())
            }
            "margin-block" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("margin-top" to it.first(), "margin-bottom" to it.last())
            }
            "padding-inline" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("padding-left" to it.first(), "padding-right" to it.last())
            }
            "padding-block" -> components.takeIf { it.size in 1..2 }?.let {
                listOf("padding-top" to it.first(), "padding-bottom" to it.last())
            }
            "margin-inline-start" -> listOf("margin-left" to value)
            "margin-inline-end" -> listOf("margin-right" to value)
            "margin-block-start" -> listOf("margin-top" to value)
            "margin-block-end" -> listOf("margin-bottom" to value)
            "padding-inline-start" -> listOf("padding-left" to value)
            "padding-inline-end" -> listOf("padding-right" to value)
            "border-inline-start" -> border("border-left", components)
            "border-inline-end" -> border("border-right", components)
            "inline-size" -> listOf("width" to value)
            "max-inline-size" -> listOf("max-width" to value)
            "min-inline-size" -> listOf("min-width" to value)
            "word-wrap" -> listOf("overflow-wrap" to value)
            else -> null
        }.orEmpty()
    }

    private fun boxSides(components: List<String>): List<String>? = when (components.size) {
        1 -> List(4) { components[0] }
        2 -> listOf(components[0], components[1], components[0], components[1])
        3 -> listOf(components[0], components[1], components[2], components[1])
        4 -> components
        else -> null
    }

    private fun border(prefix: String, components: List<String>): List<Pair<String, String>> {
        if (components.size == 1 && components[0].lowercase() in CSS_WIDE_KEYWORDS) {
            return BORDER_PARTS.map { "$prefix-$it" to components[0] }
        }
        var width = "medium"
        var style = "none"
        var color = "currentcolor"
        components.forEach { component ->
            when {
                component.lowercase() in BORDER_STYLES -> style = component.lowercase()
                component.lowercase() in BORDER_WIDTHS || component.first().isDigit() || component.first() == '.' ->
                    width =
                        component
                else -> color = component
            }
        }
        return listOf("$prefix-width" to width, "$prefix-style" to style, "$prefix-color" to color)
    }

    private fun background(value: String): List<Pair<String, String>> {
        if (value.trim().lowercase() in CSS_WIDE_KEYWORDS) {
            return listOf("background-color" to value.trim(), "background-image" to value.trim())
        }
        // Only the last layer may carry a colour; earlier layers can still be gradients drawn over it.
        val layers = value.splitTopLevel(',')
        var color = "transparent"
        val images = mutableListOf<String>()
        layers.forEachIndexed { index, layer ->
            layer.cssComponents().forEach { component ->
                val lower = component.lowercase()
                when {
                    lower.contains("gradient(") -> images += component
                    lower.startsWith("url(") || lower == "none" -> Unit
                    index == layers.lastIndex && parseCssColor(component) != null -> color = component
                    index == layers.lastIndex && lower == "currentcolor" -> color = component
                }
            }
        }
        return listOf(
            "background-color" to color,
            "background-image" to (images.joinToString(", ").ifEmpty { "none" }),
        )
    }

    private fun font(components: List<String>): List<Pair<String, String>>? {
        if (components.size == 1) {
            // System fonts (caption, menu, …) and CSS-wide keywords.
            return if (components[0].lowercase() in CSS_WIDE_KEYWORDS) {
                FONT_LONGHANDS.map { it to components[0] }
            } else {
                null
            }
        }
        var style = "normal"
        var weight = "normal"
        var variant = "normal"
        var index = 0
        while (index < components.size) {
            val lower = components[index].lowercase()
            when {
                lower in setOf("italic", "oblique") -> style = lower
                lower in setOf("bold", "bolder", "lighter") || lower.toIntOrNull() != null -> weight = lower
                lower == "small-caps" -> variant = lower
                lower == "normal" -> Unit
                else -> break
            }
            index++
        }
        val sizePart = components.getOrNull(index) ?: return null
        val family = components.drop(index + 1).joinToString(" ").takeIf(String::isNotBlank) ?: return null
        val size = sizePart.substringBefore('/')
        val lineHeight = sizePart.substringAfter('/', "").ifEmpty { "normal" }
        return listOf(
            "font-style" to style,
            "font-variant" to variant,
            "font-weight" to weight,
            "font-size" to size,
            "line-height" to lineHeight,
            "font-family" to family,
        )
    }

    private fun textDecoration(components: List<String>): List<Pair<String, String>> {
        val lines = components.filter { it.lowercase() in DECORATION_LINES }
        val color = components.firstOrNull {
            it.lowercase() !in DECORATION_LINES && it.lowercase() !in DECORATION_STYLES
        }
        return listOfNotNull(
            "text-decoration-line" to (lines.joinToString(" ").ifEmpty { "none" }),
            "text-decoration-color" to (color ?: "currentcolor"),
        )
    }

    private fun flex(components: List<String>): List<Pair<String, String>>? {
        val (grow, shrink, basis) = when {
            components.size == 1 && components[0].lowercase() == "none" -> Triple("0", "0", "auto")
            components.size == 1 && components[0].lowercase() == "auto" -> Triple("1", "1", "auto")
            components.size == 1 && components[0].lowercase() in CSS_WIDE_KEYWORDS ->
                Triple(components[0], components[0], components[0])
            components.size == 1 && components[0].toFloatOrNull() != null -> Triple(components[0], "1", "0%")
            components.size == 1 -> Triple("1", "1", components[0])
            components.size == 2 && components[1].toFloatOrNull() != null -> Triple(components[0], components[1], "0%")
            components.size == 2 -> Triple(components[0], "1", components[1])
            components.size == 3 -> Triple(components[0], components[1], components[2])
            else -> return null
        }
        return listOf("flex-grow" to grow, "flex-shrink" to shrink, "flex-basis" to basis)
    }

    private val SIDES = listOf("top", "right", "bottom", "left")
    private val CORNERS = listOf("top-left", "top-right", "bottom-right", "bottom-left")
    private val BORDER_PARTS = listOf("width", "style", "color")
    private val BORDER_STYLES =
        setOf("none", "hidden", "solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset")
    private val BORDER_WIDTHS = setOf("thin", "medium", "thick")
    private val FLEX_WRAPS = setOf("wrap", "nowrap", "wrap-reverse")
    private val DECORATION_LINES = setOf("none", "underline", "overline", "line-through")
    private val DECORATION_STYLES = setOf("solid", "double", "dotted", "dashed", "wavy")
    private val FONT_LONGHANDS =
        listOf("font-style", "font-variant", "font-weight", "font-size", "line-height", "font-family")

    /** The longhand properties a document style is built from. */
    val LONGHANDS: Set<String> = buildSet {
        addAll(
            listOf(
                "color", "background-color", "background-image", "box-shadow", "text-shadow", "opacity",
                "font-family", "font-size", "font-style", "font-variant", "font-weight", "line-height",
                "letter-spacing", "word-spacing", "text-transform", "text-align", "text-indent",
                "text-decoration-line", "text-decoration-color", "direction", "vertical-align", "white-space",
                "overflow-wrap", "display", "visibility", "width", "min-width", "max-width", "height", "float",
                "clear", "flex-direction", "flex-wrap", "flex-grow", "flex-shrink", "flex-basis", "order",
                "justify-content", "justify-items", "align-items", "align-content", "align-self", "row-gap",
                "column-gap", "grid-template-columns", "grid-column-start", "grid-column-end", "list-style-type",
            ),
        )
        SIDES.forEach { side ->
            add("margin-$side")
            add("padding-$side")
            BORDER_PARTS.forEach { add("border-$side-$it") }
        }
        CORNERS.forEach { add("border-$it-radius") }
    }
}

internal val CSS_WIDE_KEYWORDS = setOf("inherit", "initial", "unset", "revert", "revert-layer")
