package eu.kanade.tachiyomi.ui.video.player.brightness

/**
 * The picture's brightness as the side gesture, the wheel and the keys set it, a level in [levels]. Levels from 0 up
 * are the screen's own brightness, where the player may set it (Android); levels below 0 are [ExtraDim]. Desktop, whose
 * screen brightness isn't the player's to change, has extra dim alone.
 */
internal interface VideoPlayerBrightness {
    val level: Int

    val levels: IntRange

    /** The share of the picture's light the dark layer over it takes away, 0 for none. */
    val pictureDim: Float

    /** Sets the level a gesture, key or slider asks for and returns it clamped to [levels]. */
    fun adjust(level: Int): Int

    /** Re-reads the level and applies it again, as each new episode starts. */
    fun reapply()
}
