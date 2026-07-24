package mihon.entry.interactions.settings

import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class AnimePlayerPreferences(
    preferenceStore: PreferenceStore,
) : ViewerSettingsProvider {
    override val id: String = PROVIDER_ID
    override val category: ViewerSettingsCategory = ViewerSettingsCategory.PLAYER
    override val displayName: String = "Anime player"

    companion object {
        const val PROVIDER_ID = "builtin.anime.player"
        const val ENABLE_ANIME_PICTURE_IN_PICTURE_KEY = "enable_anime_picture_in_picture"
        const val ENABLE_ANIME_SEEK_PREVIEW_KEY = "enable_anime_seek_preview"
    }

    val enableAnimePictureInPicture: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_PICTURE_IN_PICTURE_KEY,
        false,
    )

    val enableAnimeSeekPreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_SEEK_PREVIEW_KEY,
        false,
    )

    val pictureInPictureSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, ENABLE_ANIME_PICTURE_IN_PICTURE_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = false,
        profilePreference = enableAnimePictureInPicture,
        codec = ViewerSettingCodecs.Boolean,
    )

    val seekPreviewSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, ENABLE_ANIME_SEEK_PREVIEW_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = false,
        profilePreference = enableAnimeSeekPreview,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        pictureInPictureSetting,
        seekPreviewSetting,
    )
}
