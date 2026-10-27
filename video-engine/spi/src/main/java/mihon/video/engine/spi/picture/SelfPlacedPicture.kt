package mihon.video.engine.spi.picture

/**
 * A player that scales and moves the picture inside its video output itself, where otherwise the output's view would be
 * transformed. Its output covers the whole player instead, so what it draws over the picture, such as its own
 * subtitles, stays on screen however far the picture is zoomed.
 */
interface SelfPlacedPicture {
    /**
     * Shows the picture at [scale] times the size that fits the output, moved from the centre by [offsetX] and
     * [offsetY]: shares of the scaled picture's width and height.
     */
    fun placePicture(scale: Double, offsetX: Double, offsetY: Double)
}
