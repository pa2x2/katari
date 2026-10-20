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

        // Named for the switch that also showed the PiP button; kept so saved choices carry over.
        const val AUTO_ENTER_PICTURE_IN_PICTURE_KEY = "enable_anime_picture_in_picture"
        const val ENABLE_ANIME_SEEK_PREVIEW_KEY = "enable_anime_seek_preview"
        const val SEEK_STEP_SECONDS_KEY = "anime_seek_step_seconds"
        const val ORIENTATION_KEY = "anime_player_orientation"
        const val AUTO_PLAY_NEXT_KEY = "anime_auto_play_next"
        const val GESTURE_GUIDE_SHOWN_KEY = "anime_gesture_guide_shown"
        const val SHOWS_REMAINING_TIME_KEY = "anime_shows_remaining_time"
        const val PREFERRED_QUALITY_HEIGHT_KEY = "anime_preferred_quality_height"
        const val DEFAULT_PLAYBACK_SPEED_PERCENT_KEY = "anime_default_playback_speed_percent"

        /** Video heights a series starts at until one is picked in the player; 0 asks for the highest offered. */
        val PREFERRED_QUALITY_HEIGHT_OPTIONS = listOf(0, 1080, 720, 480, 360)

        val DEFAULT_PLAYBACK_SPEED_PERCENT_OPTIONS = listOf(50, 75, 100, 125, 150, 200)

        /** The steps the seek buttons and double tap offer; each has its own rewind and forward icon. */
        val SEEK_STEP_SECONDS_OPTIONS = listOf(5, 10, 30)
    }

    val autoEnterPictureInPicture: Preference<Boolean> = preferenceStore.getBoolean(
        AUTO_ENTER_PICTURE_IN_PICTURE_KEY,
        false,
    )

    val enableAnimeSeekPreview: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_ANIME_SEEK_PREVIEW_KEY,
        false,
    )

    val seekStepSeconds: Preference<Int> = preferenceStore.getInt(SEEK_STEP_SECONDS_KEY, 10)

    val autoPlayNext: Preference<Boolean> = preferenceStore.getBoolean(AUTO_PLAY_NEXT_KEY, true)

    val preferredQualityHeight: Preference<Int> = preferenceStore.getInt(PREFERRED_QUALITY_HEIGHT_KEY, 0)

    val defaultPlaybackSpeedPercent: Preference<Int> = preferenceStore.getInt(DEFAULT_PLAYBACK_SPEED_PERCENT_KEY, 100)

    val gestureGuideShown: Preference<Boolean> = preferenceStore.getBoolean(GESTURE_GUIDE_SHOWN_KEY, false)

    val showsRemainingTime: Preference<Boolean> = preferenceStore.getBoolean(SHOWS_REMAINING_TIME_KEY, false)

    val orientation: Preference<AnimePlayerOrientation> = preferenceStore.getEnum(
        ORIENTATION_KEY,
        AnimePlayerOrientation.FOLLOW_VIDEO,
    )

    val autoEnterPictureInPictureSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, AUTO_ENTER_PICTURE_IN_PICTURE_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = false,
        profilePreference = autoEnterPictureInPicture,
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

    val preferredQualityHeightSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, PREFERRED_QUALITY_HEIGHT_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = 0,
        profilePreference = preferredQualityHeight,
        codec = ViewerSettingCodecs.Int,
        validate = { it in PREFERRED_QUALITY_HEIGHT_OPTIONS },
    )

    val defaultPlaybackSpeedPercentSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, DEFAULT_PLAYBACK_SPEED_PERCENT_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = 100,
        profilePreference = defaultPlaybackSpeedPercent,
        codec = ViewerSettingCodecs.Int,
        validate = { it in DEFAULT_PLAYBACK_SPEED_PERCENT_OPTIONS },
    )

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        autoEnterPictureInPictureSetting,
        seekPreviewSetting,
        seekStepSetting,
        orientationSetting,
        autoPlayNextSetting,
        preferredQualityHeightSetting,
        defaultPlaybackSpeedPercentSetting,
    )
}
