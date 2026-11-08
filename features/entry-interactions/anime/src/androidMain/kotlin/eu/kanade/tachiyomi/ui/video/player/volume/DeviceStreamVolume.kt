package eu.kanade.tachiyomi.ui.video.player.volume

import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.video.engine.spi.player.VideoPlayer
import kotlin.math.roundToInt

/**
 * The player's volume on Android: up to [maxLevel] the music stream's volume. A player that can boost adds
 * [boostLevels] above that, half the device range again, up to the boost's maximum. The boost only holds at full device
 * volume, so turning the device volume down drops it, and it starts off each time the player opens.
 */
internal class DeviceStreamVolume(
    private val audioManager: AudioManager,
) : VideoPlayerVolume {
    override var level by mutableStateOf(0)
        private set
    override var maxLevel by mutableStateOf(1)
        private set
    override var boostLevels by mutableStateOf(0)
        private set

    /** The boost the gesture set, as a level from 0 to [boostLevels]. */
    private var boostLevel = 0

    override val boostPercent: Int
        get() = boostPercentFor(boostLevel)

    override fun sync(player: VideoPlayer?) {
        this.maxLevel = audioManager.safeMusicStreamMaxVolume()
        val deviceLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, this.maxLevel)
        val boosts = player?.capabilities?.volumeBoost == true
        boostLevels = if (boosts) (this.maxLevel + 1) / 2 else 0
        if (deviceLevel < this.maxLevel) boostLevel = 0
        boostLevel = boostLevel.coerceAtMost(boostLevels)
        player?.setVolumeBoost(boostPercent)
        level = deviceLevel + boostLevel
    }

    override fun adjust(player: VideoPlayer, level: Int): Int {
        val maxLevel = maxLevel.coerceAtLeast(1)
        val normalizedLevel = level.coerceIn(0, gestureMaxLevel)
        val deviceLevel = normalizedLevel.coerceAtMost(maxLevel)
        boostLevel = normalizedLevel - deviceLevel
        if (this.level != normalizedLevel) {
            this.level = normalizedLevel
        }
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            deviceLevel.coerceIn(0, audioManager.safeMusicStreamMaxVolume()),
            0,
        )
        player.setVolumeBoost(boostPercent)
        return normalizedLevel
    }

    private fun boostPercentFor(boostLevel: Int): Int {
        if (boostLevels <= 0) return VideoPlayer.NO_VOLUME_BOOST_PERCENT
        val range = VideoPlayer.MAX_VOLUME_BOOST_PERCENT - VideoPlayer.NO_VOLUME_BOOST_PERCENT
        return VideoPlayer.NO_VOLUME_BOOST_PERCENT + (range * boostLevel / boostLevels.toFloat()).roundToInt()
    }
}

private fun AudioManager.safeMusicStreamMaxVolume(): Int =
    getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
