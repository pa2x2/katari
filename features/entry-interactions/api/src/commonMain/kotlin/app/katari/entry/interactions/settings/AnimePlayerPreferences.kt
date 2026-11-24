package app.katari.entry.interactions.settings

import app.katari.entry.interactions.player.PlaybackSpeed
import app.katari.entry.viewer.settings.ViewerSettingCodecs
import app.katari.entry.viewer.settings.ViewerSettingDefinition
import app.katari.entry.viewer.settings.ViewerSettingId
import app.katari.entry.viewer.settings.ViewerSettingScope
import app.katari.entry.viewer.settings.ViewerSettingsCategory
import app.katari.entry.viewer.settings.ViewerSettingsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.entry.model.SubtitlePosition
import tachiyomi.domain.entry.model.SubtitleTextEdge

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
        const val MARKER_SKIP_KEY = "anime_marker_skip"
        const val FIND_INTRO_TIMES_ONLINE_KEY = "anime_find_intro_times_online"
        const val INTRO_TIMES_FROM_ANISKIP_KEY = "anime_intro_times_from_aniskip"
        const val INTRO_TIMES_FROM_THEINTRODB_KEY = "anime_intro_times_from_theintrodb"
        const val BACKGROUND_PLAYBACK_KEY = "anime_background_playback"

        // Renamed when the guide gained the sideways seek, so every profile sees it once more.
        const val GESTURE_GUIDE_SHOWN_KEY = "anime_gesture_guide_shown_seek_swipe"
        const val SHOWS_REMAINING_TIME_KEY = "anime_shows_remaining_time"
        const val SUBTITLES_IN_PICTURE_KEY = "anime_subtitles_in_picture"
        const val PREFERRED_QUALITY_HEIGHT_KEY = "anime_preferred_quality_height"
        const val DEFAULT_PLAYBACK_SPEED_PERCENT_KEY = "anime_default_playback_speed_percent"
        const val DEFAULT_SUBTITLE_POSITION_KEY = "anime_default_subtitle_position"
        const val DEFAULT_SUBTITLE_TEXT_SIZE_KEY = "anime_default_subtitle_text_size"
        const val DEFAULT_SUBTITLE_TEXT_COLOR_KEY = "anime_default_subtitle_text_color"
        const val DEFAULT_SUBTITLE_BACKGROUND_COLOR_KEY = "anime_default_subtitle_background_color"
        const val DEFAULT_SUBTITLE_BACKGROUND_OPACITY_KEY = "anime_default_subtitle_background_opacity"
        const val DEFAULT_SUBTITLE_TEXT_EDGE_KEY = "anime_default_subtitle_text_edge"

        /** The subtitles' text size before the viewer sets one, a share of the picture's height. */
        const val BUILT_IN_SUBTITLE_TEXT_SIZE = 0.0533f

        /** ARGB. */
        const val BUILT_IN_SUBTITLE_TEXT_COLOR = 0xFFFFFFFF.toInt()

        /** ARGB, its alpha replaced by the background opacity. */
        const val BUILT_IN_SUBTITLE_BACKGROUND_COLOR = 0xFF000000.toInt()
        const val BUILT_IN_SUBTITLE_BACKGROUND_OPACITY = 0.72f

        /** Video heights a series starts at until one is picked in the player; 0 asks for the highest offered. */
        val PREFERRED_QUALITY_HEIGHT_OPTIONS = listOf(0, 1080, 720, 480, 360)

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

    val markerSkip: Preference<AnimeMarkerSkipMode> = preferenceStore.getEnum(
        MARKER_SKIP_KEY,
        AnimeMarkerSkipMode.BUTTON,
    )

    /**
     * Looking intro and credits times up online sends series titles and episode numbers away, to the services that
     * [introTimesFromAniSkip] and [introTimesFromTheIntroDb] leave on.
     */
    val findIntroTimesOnline: Preference<Boolean> = preferenceStore.getBoolean(FIND_INTRO_TIMES_ONLINE_KEY, false)

    val introTimesFromAniSkip: Preference<Boolean> = preferenceStore.getBoolean(INTRO_TIMES_FROM_ANISKIP_KEY, true)

    val introTimesFromTheIntroDb: Preference<Boolean> =
        preferenceStore.getBoolean(INTRO_TIMES_FROM_THEINTRODB_KEY, true)

    val backgroundPlayback: Preference<Boolean> = preferenceStore.getBoolean(BACKGROUND_PLAYBACK_KEY, false)

    val preferredQualityHeight: Preference<Int> = preferenceStore.getInt(PREFERRED_QUALITY_HEIGHT_KEY, 0)

    /** A [PlaybackSpeed]'s hundredths. */
    val defaultPlaybackSpeedPercent: Preference<Int> = preferenceStore.getInt(
        DEFAULT_PLAYBACK_SPEED_PERCENT_KEY,
        PlaybackSpeed.NORMAL.hundredths,
    )

    val gestureGuideShown: Preference<Boolean> = preferenceStore.getBoolean(GESTURE_GUIDE_SHOWN_KEY, false)

    val showsRemainingTime: Preference<Boolean> = preferenceStore.getBoolean(SHOWS_REMAINING_TIME_KEY, false)

    /** Off lets subtitles leave the picture for the band under it when there is room. */
    val subtitlesInPicture: Preference<Boolean> = preferenceStore.getBoolean(SUBTITLES_IN_PICTURE_KEY, false)

    // The look of subtitles in every series that has none of its own, set on its own Settings page. Where subtitles are
    // dragged off the position grid stays with each series.
    val defaultSubtitlePosition: Preference<SubtitlePosition> = preferenceStore.getEnum(
        DEFAULT_SUBTITLE_POSITION_KEY,
        SubtitlePosition.BOTTOM_CENTER,
    )

    /** A share of the picture's height. */
    val defaultSubtitleTextSize: Preference<Float> =
        preferenceStore.getFloat(DEFAULT_SUBTITLE_TEXT_SIZE_KEY, BUILT_IN_SUBTITLE_TEXT_SIZE)

    /** ARGB. */
    val defaultSubtitleTextColor: Preference<Int> =
        preferenceStore.getInt(DEFAULT_SUBTITLE_TEXT_COLOR_KEY, BUILT_IN_SUBTITLE_TEXT_COLOR)

    /** ARGB, its alpha replaced by [defaultSubtitleBackgroundOpacity]. */
    val defaultSubtitleBackgroundColor: Preference<Int> =
        preferenceStore.getInt(DEFAULT_SUBTITLE_BACKGROUND_COLOR_KEY, BUILT_IN_SUBTITLE_BACKGROUND_COLOR)

    val defaultSubtitleBackgroundOpacity: Preference<Float> =
        preferenceStore.getFloat(DEFAULT_SUBTITLE_BACKGROUND_OPACITY_KEY, BUILT_IN_SUBTITLE_BACKGROUND_OPACITY)

    val defaultSubtitleTextEdge: Preference<SubtitleTextEdge> = preferenceStore.getEnum(
        DEFAULT_SUBTITLE_TEXT_EDGE_KEY,
        SubtitleTextEdge.OUTLINE,
    )

    private val defaultSubtitleLook: List<Preference<*>> = listOf(
        defaultSubtitlePosition,
        defaultSubtitleTextSize,
        defaultSubtitleTextColor,
        defaultSubtitleBackgroundColor,
        defaultSubtitleBackgroundOpacity,
        defaultSubtitleTextEdge,
    )

    /** Emits now and whenever the default subtitle look changes, such as from another device. */
    fun defaultSubtitleLookChanges(): Flow<Unit> = combine(defaultSubtitleLook.map { it.changes() }) { }

    fun resetDefaultSubtitleLook() {
        defaultSubtitleLook.forEach(Preference<*>::delete)
    }

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

    val markerSkipSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, MARKER_SKIP_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = AnimeMarkerSkipMode.BUTTON,
        profilePreference = markerSkip,
        codec = ViewerSettingCodecs.codec(
            encode = AnimeMarkerSkipMode::name,
            decode = { encoded -> AnimeMarkerSkipMode.entries.firstOrNull { it.name == encoded } },
        ),
    )

    val findIntroTimesOnlineSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, FIND_INTRO_TIMES_ONLINE_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = false,
        profilePreference = findIntroTimesOnline,
        codec = ViewerSettingCodecs.Boolean,
    )

    val introTimesFromAniSkipSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, INTRO_TIMES_FROM_ANISKIP_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = true,
        profilePreference = introTimesFromAniSkip,
        codec = ViewerSettingCodecs.Boolean,
    )

    val introTimesFromTheIntroDbSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, INTRO_TIMES_FROM_THEINTRODB_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = true,
        profilePreference = introTimesFromTheIntroDb,
        codec = ViewerSettingCodecs.Boolean,
    )

    val backgroundPlaybackSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, BACKGROUND_PLAYBACK_KEY),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = false,
        profilePreference = backgroundPlayback,
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
        processorDefault = PlaybackSpeed.NORMAL.hundredths,
        profilePreference = defaultPlaybackSpeedPercent,
        codec = ViewerSettingCodecs.Int,
        validate = PlaybackSpeed::isValid,
    )

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        autoEnterPictureInPictureSetting,
        seekPreviewSetting,
        seekStepSetting,
        orientationSetting,
        autoPlayNextSetting,
        markerSkipSetting,
        findIntroTimesOnlineSetting,
        introTimesFromAniSkipSetting,
        introTimesFromTheIntroDbSetting,
        backgroundPlaybackSetting,
        preferredQualityHeightSetting,
        defaultPlaybackSpeedPercentSetting,
    )
}
