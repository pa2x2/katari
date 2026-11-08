package eu.kanade.tachiyomi.ui.video.player.volume

import app.katari.video.engine.spi.player.VideoPlayer

/**
 * The volume the side gesture shows and changes, as a level from 0 to [gestureMaxLevel]: up to [maxLevel] the
 * platform's volume, and above it [boostLevels] more where the player boosts
 * ([app.katari.video.engine.spi.player.VideoPlayerCapabilities.volumeBoost]). Android's is the device's music volume.
 */
internal interface VideoPlayerVolume {
    /** The volume, plus the boost's levels above [maxLevel]. */
    val level: Int
    val maxLevel: Int
    val boostLevels: Int

    val gestureMaxLevel: Int
        get() = maxLevel + boostLevels

    /** The boost as mpv's volume level, [VideoPlayer.NO_VOLUME_BOOST_PERCENT] while there is none. */
    val boostPercent: Int

    /** Reads the volume again, as it may have changed outside the player, and sets [player]'s boost to match. */
    fun sync(player: VideoPlayer?)

    /**
     * Sets the level a side gesture asks for through [player] and returns it clamped to the gesture range: the volume
     * first, then the boost.
     */
    fun adjust(player: VideoPlayer, level: Int): Int
}
