package eu.kanade.tachiyomi.ui.video.player.components

/**
 * How far the player's bars reach in from its edges, in pixels, as they were last laid out: the top bar from the top,
 * and the timeline with its buttons from the bottom.
 */
internal data class VideoPlayerBarReach(val top: Int, val bottom: Int) {
    /** Whether the point [y] pixels down a player [height] pixels tall is where a bar shows. */
    fun covers(y: Float, height: Float): Boolean = y < top || y >= height - bottom
}
