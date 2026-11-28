package app.katari.entry.interactions.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Anime player settings that belong to the screen and the room rather than the viewer, so every profile shares them.
 */
class AnimePlayerDevicePreferences(
    preferenceStore: PreferenceStore,
) {
    val rememberBrightness: Preference<Boolean> = preferenceStore.getBoolean(REMEMBER_BRIGHTNESS_KEY, true)

    /**
     * The brightness level the player last closed at, negative for extra dim: up to 100 where the player sets the
     * screen's brightness, as on Android, up to 0 where it only dims the picture, as on desktop. Unset while the player
     * follows the system's brightness and leaves the picture undimmed.
     */
    val brightnessLevel: Preference<Int> = preferenceStore.getInt(
        Preference.appStateKey(BRIGHTNESS_LEVEL_KEY),
        UNSET_BRIGHTNESS_LEVEL,
    )

    /**
     * The player's own volume in percent, up to 150 with the boost, where the player sets its own rather than the
     * device's, as on desktop.
     */
    val volumePercent: Preference<Int> = preferenceStore.getInt(Preference.appStateKey(VOLUME_PERCENT_KEY), 100)

    companion object {
        // Below every level, extra dim's negative ones included
        const val UNSET_BRIGHTNESS_LEVEL = Int.MIN_VALUE
        const val REMEMBER_BRIGHTNESS_KEY = "anime_player_remember_brightness"
        private const val BRIGHTNESS_LEVEL_KEY = "anime_player_brightness_level"
        private const val VOLUME_PERCENT_KEY = "anime_player_volume_percent"
    }
}
