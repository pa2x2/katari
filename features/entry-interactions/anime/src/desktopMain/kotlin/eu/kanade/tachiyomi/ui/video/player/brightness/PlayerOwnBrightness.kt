package eu.kanade.tachiyomi.ui.video.player.brightness

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences

/**
 * The desktop's brightness: extra dim alone, as the screen's brightness isn't the player's to change, so 0 leaves the
 * picture as the screen shows it. Like [eu.kanade.tachiyomi.ui.video.player.volume.PlayerOwnVolume] the level outlives
 * the player, kept with the device's player settings.
 */
internal class PlayerOwnBrightness(private val preferences: AnimePlayerDevicePreferences) : VideoPlayerBrightness {
    override val levels = -ExtraDim.LEVELS..0

    override var level by mutableIntStateOf(preferences.brightnessLevel.get().takeIf { it in levels } ?: 0)
        private set

    override val pictureDim: Float
        get() = ExtraDim.pictureDim(level)

    override fun adjust(level: Int): Int {
        this.level = level.coerceIn(levels)
        preferences.brightnessLevel.set(this.level)
        return this.level
    }

    // Only the player sets it, so there is nothing to read again
    override fun reapply() = Unit
}
