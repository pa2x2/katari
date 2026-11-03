package mihon.core.common.web

/**
 * What one pointer of a touchscreen did on an [ExtensionPage], which the page takes as a person's input. Coordinates
 * and radii are in the page's CSS pixels from its top left corner.
 *
 * @property pointerId tells the pointers of one touch apart; a pointer keeps it from [Phase.PRESSED] until it is
 * [Phase.RELEASED] or [Phase.CANCELLED].
 * @property radiusX the contact ellipse's radius across before [rotation], 0 where the input reports no contact size.
 * @property radiusY its radius along before [rotation].
 * @property rotation the ellipse's clockwise rotation in degrees, in `0..90`.
 * @property pressure from 0 to 1, 0 where the input reports none.
 */
data class ExtensionPageTouch(
    val pointerId: Int,
    val phase: Phase,
    val tool: Tool,
    val x: Float,
    val y: Float,
    val radiusX: Float,
    val radiusY: Float,
    val rotation: Float,
    val pressure: Float,
) {
    enum class Phase { PRESSED, MOVED, RELEASED, CANCELLED }

    enum class Tool { FINGER, STYLUS, ERASER, MOUSE }
}
