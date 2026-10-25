package tachiyomi.domain.entry.model

/** How a title's video fills the player, chosen by the viewer. */
sealed interface PictureFraming {
    /** The whole picture, with bars where its shape differs from the screen's. */
    data object Fit : PictureFraming

    /** The picture covers the screen, cut where it overflows. */
    data object Fill : PictureFraming

    /**
     * A pinch. [scale] is relative to [Fit] and at least 1. [panX] and [panY] are how far the picture has moved from
     * the centre, as a share of how far the scale lets it move: from -1 to 1, negative to the left or up.
     */
    data class Zoom(val scale: Double, val panX: Double, val panY: Double) : PictureFraming
}
