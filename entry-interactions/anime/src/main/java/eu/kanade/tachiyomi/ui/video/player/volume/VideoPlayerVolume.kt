package eu.kanade.tachiyomi.ui.video.player.volume

import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import mihon.video.engine.spi.audio.VolumeBoost
import kotlin.math.roundToInt

/**
 * The volume the side gesture shows and changes. Up to [maxLevel] it is the player's device volume when the player
 * offers it, which follows the output the audio goes to, and the music stream's volume otherwise. A player that can
 * boost ([VolumeBoost]) adds [boostLevels] above that, half the device range again, up to the boost's maximum. The
 * boost only holds at full device volume, so turning the device volume down drops it, and it starts off each time the
 * player opens.
 */
internal class VideoPlayerVolume(
    private val audioManager: AudioManager,
    private val currentPlayer: () -> Player?,
) {
    /** The device volume, plus the boost's levels above [maxLevel]. */
    var level by mutableStateOf(0)
        private set
    var maxLevel by mutableStateOf(1)
        private set
    var boostLevels by mutableStateOf(0)
        private set

    val gestureMaxLevel: Int
        get() = maxLevel + boostLevels

    /** The boost the gesture set, as a level from 0 to [boostLevels]. */
    private var boostLevel = 0

    /** The boost as mpv's volume level, 100 while there is none. */
    val boostPercent: Int
        get() = boostPercentFor(boostLevel)

    fun sync(
        player: Player? = null,
        deviceInfo: DeviceInfo? = player?.deviceInfo,
    ) {
        val syncedPlayer = player ?: currentPlayer()
        val currentDeviceInfo = deviceInfo ?: syncedPlayer?.deviceInfo
        val maxLevel = currentDeviceInfo?.maxVolume
            ?.takeIf { it > 0 }
            ?: audioManager.safeMusicStreamMaxVolume()
        this.maxLevel = maxLevel.coerceAtLeast(1)
        val deviceLevel = when {
            syncedPlayer != null && syncedPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) -> {
                syncedPlayer.getDeviceVolume().coerceIn(0, this.maxLevel)
            }
            else -> {
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    .coerceIn(0, this.maxLevel)
            }
        }
        val boost = syncedPlayer as? VolumeBoost
        boostLevels = if (boost?.canBoostVolume == true) (this.maxLevel + 1) / 2 else 0
        if (deviceLevel < this.maxLevel) boostLevel = 0
        boostLevel = boostLevel.coerceAtMost(boostLevels)
        boost?.setVolumeBoost(boostPercent)
        level = deviceLevel + boostLevel
    }

    /**
     * Sets the level a side gesture asks for through [player] and returns it clamped to the gesture range: the device
     * volume first, then the boost.
     */
    fun adjust(player: Player, level: Int): Int {
        val maxLevel = maxLevel.coerceAtLeast(1)
        val normalizedLevel = level.coerceIn(0, gestureMaxLevel)
        val deviceLevel = normalizedLevel.coerceAtMost(maxLevel)
        boostLevel = normalizedLevel - deviceLevel
        if (this.level != normalizedLevel) {
            this.level = normalizedLevel
        }
        if (player.isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)) {
            player.setDeviceVolume(deviceLevel, 0)
        } else if (player.canSetDeviceVolumeCompat()) {
            player.setDeviceVolumeCompat(deviceLevel)
        } else {
            val streamMaxVolume = audioManager.safeMusicStreamMaxVolume()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                deviceLevel.coerceIn(0, streamMaxVolume),
                0,
            )
        }
        (player as? VolumeBoost)?.setVolumeBoost(boostPercent)
        return normalizedLevel
    }

    private fun boostPercentFor(boostLevel: Int): Int {
        if (boostLevels <= 0) return VolumeBoost.NO_VOLUME_BOOST_PERCENT
        val range = VolumeBoost.MAX_VOLUME_BOOST_PERCENT - VolumeBoost.NO_VOLUME_BOOST_PERCENT
        return VolumeBoost.NO_VOLUME_BOOST_PERCENT + (range * boostLevel / boostLevels.toFloat()).roundToInt()
    }
}

private fun AudioManager.safeMusicStreamMaxVolume(): Int =
    getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

@Suppress("DEPRECATION")
private fun Player.canSetDeviceVolumeCompat(): Boolean =
    isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME)

@Suppress("DEPRECATION")
private fun Player.setDeviceVolumeCompat(volume: Int) {
    setDeviceVolume(volume)
}
