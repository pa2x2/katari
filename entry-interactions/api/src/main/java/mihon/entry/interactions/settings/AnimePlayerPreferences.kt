package mihon.entry.interactions.settings

import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

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
        const val SEEK_STEP_SECONDS_KEY = "anime_seek_step_seconds"
        const val ORIENTATION_KEY = "anime_player_orientation"
        const val AUTO_PLAY_NEXT_KEY = "anime_auto_play_next"
        const val GESTURE_GUIDE_SHOWN_KEY = "anime_gesture_guide_shown"

        /** The steps the seek buttons and double tap offer; each has its own rewind and forward icon. */
        val SEEK_STEP_SECONDS_OPTIONS = listOf(5, 10, 30)
    }

    val enableAnimePictureInPicture: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_PICTURE_IN_PICTURE_KEY,
        false,
    )

    val enableAnimeSeekPreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_SEEK_PREVIEW_KEY,
        false,
    )

    val seekStepSeconds: Preference<Int> = preferenceStore.getInt(SEEK_STEP_SECONDS_KEY, 10)

    val autoPlayNext: Preference<Boolean> = preferenceStore.getBoolean(AUTO_PLAY_NEXT_KEY, true)

    val gestureGuideShown: Preference<Boolean> = preferenceStore.getBoolean(GESTURE_GUIDE_SHOWN_KEY, false)

    val orientation: Preference<AnimePlayerOrientation> = preferenceStore.getEnum(
        ORIENTATION_KEY,
        AnimePlayerOrientation.FOLLOW_VIDEO,
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

    val seekStepSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, SEEK_STEP_SECONDS_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = 10,
        profilePreference = seekStepSeconds,
        codec = ViewerSettingCodecs.Int,
        validate = { it in SEEK_STEP_SECONDS_OPTIONS },
    )

    val orientationSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, ORIENTATION_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = AnimePlayerOrientation.FOLLOW_VIDEO,
        profilePreference = orientation,
        codec = ViewerSettingCodecs.codec(
            encode = AnimePlayerOrientation::name,
            decode = { encoded -> AnimePlayerOrientation.entries.firstOrNull { it.name == encoded } },
        ),
    )

    val autoPlayNextSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, AUTO_PLAY_NEXT_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = true,
        profilePreference = autoPlayNext,
        codec = ViewerSettingCodecs.Boolean,
    )

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        pictureInPictureSetting,
        seekPreviewSetting,
        seekStepSetting,
        orientationSetting,
        autoPlayNextSetting,
    )
}
