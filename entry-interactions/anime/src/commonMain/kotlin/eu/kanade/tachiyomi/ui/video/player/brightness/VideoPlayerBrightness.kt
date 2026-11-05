package eu.kanade.tachiyomi.ui.video.player.brightness

/**
 * The screen's brightness as the player's side gesture sets it, a level from 0 to [MAX_LEVEL]. Only Android has it:
 * a desktop screen's brightness isn't the player's to change, so desktop gives the player none.
 */
internal interface VideoPlayerBrightness {
    val level: Int

    /** What the content overlay draws over the video: a dim at level 0, nothing otherwise. */
    val overlayValue: Int

    /** Sets the level a side gesture asks for and returns it clamped to the gesture range. */
    fun adjust(level: Int): Int

    /** Re-reads the level and applies it again, as each new episode starts. */
    fun reapply()

    companion object {
        const val MAX_LEVEL = 100
    }
}
