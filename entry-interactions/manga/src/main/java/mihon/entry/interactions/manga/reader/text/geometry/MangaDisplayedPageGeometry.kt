package mihon.entry.interactions.manga.reader.text.geometry

import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion

/**
 * How the image a viewer displays relates to the raw page: the viewer derives an image by [transform], then shows the
 * [shown] part of it (all of it, or what is left once borders are cropped).
 */
internal data class MangaDisplayedPageGeometry(
    val transform: MangaPageTransform,
    val shown: ImageRect,
) {
    /**
     * [region], recognized on the raw page, in displayed coordinates; `null` when the displayed image doesn't show most
     * of it. The kept part is clipped to what is shown, and so is its container.
     */
    fun toDisplayed(region: RecognizedTextRegion): RecognizedTextRegion? {
        val piece = transform.pieceShowing(region.bounds) ?: return null
        val bounds = piece.toTransformed(region.bounds)?.let(::toShown) ?: return null
        return region.copy(
            bounds = bounds,
            container = region.container?.let(piece::toTransformed)?.let(::toShown),
        )
    }

    private fun toShown(rect: ImageRect): ImageRect? = rect.intersect(shown)?.let {
        ImageRect(it.left - shown.left, it.top - shown.top, it.right - shown.left, it.bottom - shown.top)
    }
}
