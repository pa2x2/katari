package eu.kanade.tachiyomi.ui.video.player.volume

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.video.engine.spi.player.VideoPlayer

/**
 * The desktop's volume: the player's own, in percent, up to 150 % with the boost above 100 %, as mpv counts it. The
 * level outlives the player, kept with the device's player settings; muting lasts until the player closes or the
 * volume is set again.
 */
internal class PlayerOwnVolume(private val preferences: AnimePlayerDevicePreferences) :
    VideoPlayerVolume,
    VideoPlayerVolumeControl {
    override var percent by mutableIntStateOf(
        preferences.volumePercent.get().coerceIn(0, VideoPlayer.MAX_VOLUME_BOOST_PERCENT),
    )
        private set
    override var muted by mutableStateOf(false)
        private set

    override val maxPercent: Int = VideoPlayer.MAX_VOLUME_BOOST_PERCENT

    override val level: Int get() = percent
    override val maxLevel: Int = VideoPlayer.NO_VOLUME_BOOST_PERCENT
    override val boostLevels: Int = VideoPlayer.MAX_VOLUME_BOOST_PERCENT - VideoPlayer.NO_VOLUME_BOOST_PERCENT

    override val boostPercent: Int
        get() = percent.coerceAtLeast(VideoPlayer.NO_VOLUME_BOOST_PERCENT)

    override fun sync(player: VideoPlayer?) {
        player ?: return
        player.setVolume(if (muted) 0f else percent.coerceAtMost(maxLevel) / maxLevel.toFloat())
        player.setVolumeBoost(boostPercent)
    }

    override fun adjust(player: VideoPlayer, level: Int): Int {
        setPercent(player, level)
        return percent
    }

    override fun setPercent(player: VideoPlayer, percent: Int) {
        this.percent = percent.coerceIn(0, maxPercent)
        muted = false
        preferences.volumePercent.set(this.percent)
        sync(player)
    }

    override fun setMuted(player: VideoPlayer, muted: Boolean) {
        this.muted = muted
        sync(player)
    }
}
