package eu.kanade.tachiyomi.ui.video.player.volume

import mihon.video.engine.spi.player.VideoPlayer

/**
 * The volume button and slider in the controls, where the player's own volume is the one people set, as on desktop.
 * Android has none: its volume keys and the side swipe set the device's volume.
 */
internal interface VideoPlayerVolumeControl {
    /** From 0 to [maxPercent], what plays unless [muted]; above [VideoPlayer.NO_VOLUME_BOOST_PERCENT] it is boosted. */
    val percent: Int
    val maxPercent: Int
    val muted: Boolean

    /** Sets [player]'s volume to [percent], which unmutes it. */
    fun setPercent(player: VideoPlayer, percent: Int)

    fun setMuted(player: VideoPlayer, muted: Boolean)
}
