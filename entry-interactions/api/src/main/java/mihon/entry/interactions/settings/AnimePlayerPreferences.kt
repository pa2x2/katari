package mihon.entry.interactions.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class AnimePlayerPreferences(
    preferenceStore: PreferenceStore,
) {
    companion object {
        const val ENABLE_ANIME_PICTURE_IN_PICTURE_KEY = "enable_anime_picture_in_picture"
        const val ENABLE_ANIME_SEEK_PREVIEW_KEY = "enable_anime_seek_preview"

        val profileKeys = setOf(
            ENABLE_ANIME_PICTURE_IN_PICTURE_KEY,
            ENABLE_ANIME_SEEK_PREVIEW_KEY,
        )
    }

    val enableAnimePictureInPicture: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_PICTURE_IN_PICTURE_KEY,
        false,
    )

    val enableAnimeSeekPreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_SEEK_PREVIEW_KEY,
        false,
    )
}
