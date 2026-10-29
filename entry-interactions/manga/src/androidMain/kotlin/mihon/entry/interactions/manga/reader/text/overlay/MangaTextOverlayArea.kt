package mihon.entry.interactions.manga.reader.text.overlay

import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion

/**
 * Where a translation of [region] is drawn. Translations are usually longer than the original, so inside a speech
 * bubble they may use the bubble's interior (the rectangle inscribed in its outline) as well as the original text's
 * place; free text only gets its own place, slightly padded, so artwork around it stays visible.
 */
internal fun overlayArea(region: RecognizedTextRegion): ImageRect {
    val container = region.container
    if (container == null) {
        val padding = (maxOf(region.bounds.width, region.bounds.height) * FREE_TEXT_PADDING).toInt()
        return ImageRect(
            left = region.bounds.left - padding,
            top = region.bounds.top - padding,
            right = region.bounds.right + padding,
            bottom = region.bounds.bottom + padding,
        )
    }
    val insetX = (container.width * BUBBLE_INSET).toInt()
    val insetY = (container.height * BUBBLE_INSET).toInt()
    val interior = ImageRect(
        left = container.left + insetX,
        top = container.top + insetY,
        right = (container.right - insetX).coerceAtLeast(container.left + insetX + 1),
        bottom = (container.bottom - insetY).coerceAtLeast(container.top + insetY + 1),
    )
    return interior.union(region.bounds).intersect(container) ?: region.bounds
}

/** Share of a bubble's size lost on each side to its rounded outline; an ellipse's inscribed rectangle loses ~15%. */
private const val BUBBLE_INSET = 0.15

private const val FREE_TEXT_PADDING = 0.04
