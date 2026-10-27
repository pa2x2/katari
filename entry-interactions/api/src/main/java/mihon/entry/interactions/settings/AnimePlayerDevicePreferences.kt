package mihon.entry.interactions.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Anime player settings that belong to the screen and the room rather than the viewer, so every profile shares them.
 */
class AnimePlayerDevicePreferences(
    preferenceStore: PreferenceStore,
) {
    val rememberBrightness: Preference<Boolean> = preferenceStore.getBoolean(REMEMBER_BRIGHTNESS_KEY, true)

    /** The brightness gesture level the player last closed at, 0 to 100; unset while the player follows the system. */
    val brightnessLevel: Preference<Int> = preferenceStore.getInt(
        Preference.appStateKey(BRIGHTNESS_LEVEL_KEY),
        UNSET_BRIGHTNESS_LEVEL,
    )

    companion object {
        const val UNSET_BRIGHTNESS_LEVEL = -1
        private const val REMEMBER_BRIGHTNESS_KEY = "anime_player_remember_brightness"
        private const val BRIGHTNESS_LEVEL_KEY = "anime_player_brightness_level"
    }
}
