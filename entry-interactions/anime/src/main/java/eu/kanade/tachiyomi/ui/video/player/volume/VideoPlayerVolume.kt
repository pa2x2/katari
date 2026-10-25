package eu.kanade.tachiyomi.ui.video.player.volume

import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player

/**
 * The volume the side gesture shows and changes. It is the player's device volume when the player offers it, which
 * follows the output the audio goes to, and the music stream's volume otherwise.
 */
internal class VideoPlayerVolume(
    private val audioManager: AudioManager,
    private val currentPlayer: () -> Player?,
) {
    var level by mutableStateOf(0)
        private set
    var maxLevel by mutableStateOf(1)
        private set

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
        level = when {
            syncedPlayer != null && syncedPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) -> {
                syncedPlayer.getDeviceVolume().coerceIn(0, this.maxLevel)
            }
            else -> {
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    .coerceIn(0, this.maxLevel)
            }
        }
    }

    /** Sets the level a side gesture asks for through [player] and returns it clamped to the volume range. */
    fun adjust(player: Player, level: Int): Int {
        val maxLevel = maxLevel.coerceAtLeast(1)
        val normalizedLevel = level.coerceIn(0, maxLevel)
        if (this.level != normalizedLevel) {
            this.level = normalizedLevel
        }
        if (player.isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)) {
            player.setDeviceVolume(normalizedLevel, 0)
        } else if (player.canSetDeviceVolumeCompat()) {
            player.setDeviceVolumeCompat(normalizedLevel)
        } else {
            val streamMaxVolume = audioManager.safeMusicStreamMaxVolume()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                normalizedLevel.coerceIn(0, streamMaxVolume),
                0,
            )
        }
        return normalizedLevel
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
