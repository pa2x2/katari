package mihon.entry.interactions.manga.reader.text.geometry

import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion

/**
 * How the image text features read relates to the raw page: it is the [shown] part of the page, all of it or what is
 * left once borders are cropped. The viewers cut and turn spreads only when drawing, so the image is never cut or
 * turned itself.
 */
internal data class MangaDisplayedPageGeometry(val shown: ImageRect) {
    /**
     * [region], recognized on the raw page, in displayed coordinates; `null` when none of it is shown. The kept part is
     * clipped to what is shown, and so is its container.
     */
    fun toDisplayed(region: RecognizedTextRegion): RecognizedTextRegion? {
        val bounds = toShown(region.bounds) ?: return null
        return region.copy(bounds = bounds, container = region.container?.let(::toShown))
    }

    private fun toShown(rect: ImageRect): ImageRect? = rect.intersect(shown)?.let {
        ImageRect(it.left - shown.left, it.top - shown.top, it.right - shown.left, it.bottom - shown.top)
    }
}
