package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import org.jsoup.nodes.Element

/** A plain SVG image wrapper can use the publication image loader without SVG external access. */
internal fun Element.singleWrappedImage(): Element? {
    val image = children().singleOrNull { it.normalName() == "image" } ?: return null
    if (children().any { it.normalName() !in setOf("image", "title", "desc") }) return null
    if (getAllElements().any { element ->
            listOf("transform", "clip-path", "mask", "filter", "style", "opacity").any(element::hasAttr)
        }
    ) {
        return null
    }
    if (image.attr("x").ifBlank { "0" }.toFloatOrNull() != 0f ||
        image.attr("y").ifBlank { "0" }.toFloatOrNull() != 0f
    ) {
        return null
    }
    val viewBox = attr("viewBox").trim().split(Regex("[ ,]+")).mapNotNull(String::toFloatOrNull)
    if (viewBox.size != 4 || viewBox[0] != 0f || viewBox[1] != 0f) return null
    if (image.attr("width").toFloatOrNull() != viewBox[2] ||
        image.attr("height").toFloatOrNull() != viewBox[3]
    ) {
        return null
    }
    return image
}
