package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import tachiyomi.i18n.*

/**
 * Aniyomi's names for its settings Katari has nothing for and the places it keeps them, as the report lists them
 * (D075), and the settings the report leaves out: from Aniyomi's screens at aniyomiorg/aniyomi 97414446b8a9 (workspace
 * research/08). Places start where Aniyomi's settings, video player or library start.
 */
internal object AniyomiSettingLabels {
    val NO_COUNTERPART: Map<String, OtherAppSettingLabel> = mapOf(
        "pref_preserve_watching_position" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_preserve_watching_position,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
        ),
        "pref_progress_preference" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_progress_mark_as_seen,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
        ),
        "pref_allow_gestures_in_panels" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_controls_allow_gestures_in_panels,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_controls,
        ),
        "pref_show_loading" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_controls_show_loading,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_controls,
        ),
        "pref_show_current_chapter" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_controls_show_chapter_indicator,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_controls,
        ),
        "pref_remember_volume" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_remember_volume,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_controls,
        ),
        "pref_show_failed_hosters" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_hosters_show_failure,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_hosters,
        ),
        "pref_show_empty_hosters" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_hosters_show_empty,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_hosters,
        ),
        "player_fullscreen" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_fullscreen,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "player_hide_controls" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_hide_controls,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_display_vol_as_per" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_controls_display_volume_percentage,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_show_system_status_bar" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_show_system_bar,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_reduce_motion" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_reduce_motion,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_player_time_to_disappear" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_time_to_disappear,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_panel_opacity" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_panel_opacity,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.action_display,
        ),
        "pref_waiting_time_aniskip" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_waiting_time_aniskip,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_intro_skip,
        ),
        "pref_disabled_ani_skip_chapters" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_disable_aniskip_chapter,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_intro_skip,
        ),
        "pref_enable_pip" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_enable_pip,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_pip,
        ),
        "pref_pip_episode_toasts" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_pip_episode_toasts,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_pip,
        ),
        "pip_replace_with_previous" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_pip_replace_with_previous,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_pip,
        ),
        "pref_always_use_external_player" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_always_use_external_player,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_external_player,
        ),
        "external_player_preference" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_external_player_preference,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_internal,
            MR.strings.backup_conversion_aniyomi_place_pref_category_external_player,
        ),
        "default_speed_presets" to aniyomi(
            MR.strings.backup_conversion_aniyomi_default_speed_presets,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_speed,
        ),
        "pref_gesture_volume_brightness" to aniyomi(
            MR.strings.backup_conversion_aniyomi_enable_volume_brightness_gestures,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_sliders,
        ),
        "pref_swap_volume_and_brightness" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_controls_swap_vol_brightness,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_sliders,
        ),
        "pref_gesture_horizontal_seek" to aniyomi(
            MR.strings.backup_conversion_aniyomi_enable_horizontal_seek_gesture,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_seeking,
        ),
        "pref_show_seekbar" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_show_seekbar,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_seeking,
        ),
        "pref_default_intro_length" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_default_intro_length,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_seeking,
        ),
        "pref_player_smooth_seek" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_smooth_seek,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_player_seeking,
        ),
        "pref_left_double_tap" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_left_double_tap,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_double_tap,
        ),
        "pref_center_double_tap" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_center_double_tap,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_double_tap,
        ),
        "pref_right_double_tap" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_right_double_tap,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_double_tap,
        ),
        "pref_media_previous" to aniyomi(
            MR.strings.action_previous,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_media_controls,
        ),
        "pref_media_playpause" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_media_playpause,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_media_controls,
        ),
        "pref_media_next" to aniyomi(
            MR.strings.action_next,
            MR.strings.pref_category_player,
            MR.strings.pref_anime_gestures,
            MR.strings.backup_conversion_aniyomi_place_pref_category_media_controls,
        ),
        "pref_subtitle_lang" to aniyomi(
            MR.strings.backup_conversion_aniyomi_preferred_languages,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_subtitles,
        ),
        "pref_subtitle_whitelist" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_subtitle_whitelist,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_subtitles,
        ),
        "pref_subtitle_blacklist" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_subtitle_blacklist,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_subtitles,
        ),
        "pref_screenshot_subtitles" to aniyomi(
            MR.strings.backup_conversion_aniyomi_screenshot_show_subs,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.backup_conversion_aniyomi_place_screenshot,
        ),
        "pref_subtitle_font" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_typography_font,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_sub_scale" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_scale,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_misc_title,
        ),
        "pref_sub_border_size" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_typography_border_size,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_bold_subtitles" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_bold_subtitles,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_italic_subtitles" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_italic_subtitles,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_border_color_subtitles" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_subtitles_color_border,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_colors_title,
        ),
        "sub_shadow_offset" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_subtitles_shadow_offset,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_sub_justify" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_sub_justify,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
        ),
        "pref_override_subtitles_ass" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_override_ass,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_misc_title,
        ),
        "pref_subtitles_delay" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_delay,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_delay,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_delay_title,
        ),
        "pref_subtitles_speed" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_speed,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_subtitles,
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_delay,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_delay_title,
        ),
        "pref_audio_lang" to aniyomi(
            MR.strings.backup_conversion_aniyomi_preferred_languages,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_audio,
        ),
        "pref_audio_pitch_correction" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_audio_pitch_correction,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_audio,
        ),
        "pref_audio_config" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_audio_channels,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_audio,
        ),
        "pref_audio_volume_boost_cap" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_audio_boost_cap,
            MR.strings.pref_category_player,
            MR.strings.anime_playback_audio,
        ),
        "pref_audio_delay" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_delay,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.anime_playback_audio,
            MR.strings.backup_conversion_aniyomi_player_sheets_sub_delay_delay,
            MR.strings.backup_conversion_aniyomi_place_player_sheets_audio_delay_title,
        ),
        "pref_try_hwdec" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_try_hw,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_decoder,
        ),
        "pref_gpu_next" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_gpu_next_title,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_decoder,
        ),
        "pref_video_debanding" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_debanding_title,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_decoder,
        ),
        "use_yuv420p" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_use_yuv420p_title,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_decoder,
        ),
        "pref_player_filter_brightness" to aniyomi(
            MR.strings.anime_playback_brightness,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.label_more,
            MR.strings.filter_title,
        ),
        "pref_player_filter_saturation" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_filters_saturation,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.label_more,
            MR.strings.filter_title,
        ),
        "pref_player_filter_contrast" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_filters_contrast,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.label_more,
            MR.strings.filter_title,
        ),
        "pref_player_filter_gamma" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_filters_gamma,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.label_more,
            MR.strings.filter_title,
        ),
        "pref_player_filter_hue" to aniyomi(
            MR.strings.backup_conversion_aniyomi_player_sheets_filters_hue,
            MR.strings.backup_conversion_aniyomi_place_video_player,
            MR.strings.label_more,
            MR.strings.filter_title,
        ),
        "mpv_scripts" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_mpv_user_files,
            MR.strings.pref_category_player,
            MR.strings.pref_category_advanced,
        ),
        "pref_mpv_conf" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_mpv_conf,
            MR.strings.pref_category_player,
            MR.strings.pref_category_advanced,
        ),
        "pref_mpv_input" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_mpv_input,
            MR.strings.pref_category_player,
            MR.strings.pref_category_advanced,
        ),
        "pref_animelib_columns_landscape_key" to aniyomi(
            MR.strings.backup_conversion_mihon_landscape_columns,
            MR.strings.backup_conversion_aniyomi_place_anime_library,
            MR.strings.action_display,
        ),
        "hidden_categories" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_category_hide_hidden,
            MR.strings.label_library,
            MR.strings.backup_conversion_kind_categories,
        ),
        "default_episode_show_summaries" to aniyomi(
            MR.strings.backup_conversion_aniyomi_show_episode_summaries,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.episode_settings,
            MR.strings.action_display,
        ),
        "default_season_filter_by_downloaded" to aniyomi(
            MR.strings.label_downloaded,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_filter_by_unseen" to aniyomi(
            MR.strings.action_filter_unseen,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_filter_by_started" to aniyomi(
            MR.strings.label_started,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_filter_by_completed" to aniyomi(
            MR.strings.completed,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_filter_by_bookmarked" to aniyomi(
            MR.strings.action_filter_bookmarked,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_filter_by_fillermarked" to aniyomi(
            MR.strings.backup_conversion_aniyomi_action_filter_fillermarked,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_filter,
        ),
        "default_season_sort_by_source_or_number" to aniyomi(
            MR.strings.backup_conversion_aniyomi_default_season_sort_by_source_or_number,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_sort,
        ),
        "default_season_sort_by_ascending_or_descending" to aniyomi(
            MR.strings.backup_conversion_aniyomi_default_season_sort_by_ascending_or_descending,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_sort,
        ),
        "default_season_grid_display_mode" to aniyomi(
            MR.strings.action_display_mode,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
        ),
        "default_season_grid_display_size" to aniyomi(
            MR.strings.backup_conversion_aniyomi_default_season_grid_display_size,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
        ),
        "default_season_download_overlay" to aniyomi(
            MR.strings.backup_conversion_aniyomi_action_display_download_badge_anime,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.filter_mode_overlay,
        ),
        "default_season_unseen_overlay" to aniyomi(
            MR.strings.backup_conversion_aniyomi_action_display_unseen_badge,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.filter_mode_overlay,
        ),
        "default_season_local_overlay" to aniyomi(
            MR.strings.action_display_local_badge,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.filter_mode_overlay,
        ),
        "default_season_lang_overlay" to aniyomi(
            MR.strings.action_display_language_badge,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.filter_mode_overlay,
        ),
        "default_season_continue_overlay" to aniyomi(
            MR.strings.action_display_show_continue_watching_button,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.filter_mode_overlay,
        ),
        "default_season_display_mode" to aniyomi(
            MR.strings.backup_conversion_aniyomi_default_season_display_mode,
            MR.strings.backup_conversion_aniyomi_place_anime_page,
            MR.strings.backup_conversion_aniyomi_place_season_settings,
            MR.strings.action_display,
            MR.strings.backup_conversion_aniyomi_place_action_display_grid_mode,
        ),
        "pref_update_season_on_refresh" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_update_seasons_refresh,
            MR.strings.label_library,
            MR.strings.backup_conversion_aniyomi_place_pref_library_season,
        ),
        "pref_update_season_on_library_update" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_update_seasons_update,
            MR.strings.label_library,
            MR.strings.backup_conversion_aniyomi_place_pref_library_season,
        ),
        "use_external_downloader" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_use_external_downloader,
            MR.strings.label_downloads_tab,
            MR.strings.backup_conversion_aniyomi_place_pref_category_external_downloader,
        ),
        "external_downloader_selection" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_external_downloader_selection,
            MR.strings.label_downloads_tab,
            MR.strings.backup_conversion_aniyomi_place_pref_category_external_downloader,
        ),
        "split_tall_images" to aniyomi(
            MR.strings.backup_conversion_aniyomi_split_tall_images,
            MR.strings.label_downloads_tab,
        ),
        "pref_download_fillermarked" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_download_fillermarked_items,
            MR.strings.label_downloads_tab,
            MR.strings.backup_conversion_aniyomi_place_pref_category_delete_chapters,
        ),
        "download_slots" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_download_slots,
            MR.strings.label_downloads_tab,
        ),
        "download_speed_limit" to aniyomi(
            MR.strings.backup_conversion_aniyomi_download_speed_limit,
            MR.strings.label_downloads_tab,
        ),
        "data_saver" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "ignore_jpeg" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_ignore_jpeg,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "ignore_gif" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_ignore_gif,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "data_saver_image_quality" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_image_quality,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "data_saver_image_format_jpeg" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_image_format,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "data_saver_server" to aniyomi(
            MR.strings.backup_conversion_aniyomi_bandwidth_data_saver_server,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "data_saver_color_bw" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_color_bw,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "data_saver_excluded" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_excluded,
            MR.strings.browse,
            MR.strings.backup_conversion_aniyomi_place_manga_sources,
        ),
        "data_saver_downloader" to aniyomi(
            MR.strings.backup_conversion_aniyomi_data_saver_downloader,
            MR.strings.pref_category_advanced,
            MR.strings.backup_conversion_aniyomi_data_saver,
        ),
        "track_on_adding_to_library" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_track_on_add_library,
            MR.strings.backup_conversion_mihon_tracking,
        ),
        "show_next_episode_airing_time" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_show_next_episode_airing_time,
            MR.strings.backup_conversion_mihon_tracking,
        ),
        "start_screen" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_start_screen,
            MR.strings.pref_category_appearance,
            MR.strings.action_display,
        ),
        "bottom_rail_nav_style" to aniyomi(
            MR.strings.backup_conversion_aniyomi_bottom_rail_nav_style,
            MR.strings.pref_category_appearance,
            MR.strings.action_display,
        ),
        "pref_always_decode_long_strip_with_ssiv" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_always_decode_long_strip_with_ssiv_2,
            MR.strings.pref_category_advanced,
            MR.strings.pref_category_reader,
        ),
        "pref_preserve_reading_position" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_preserve_reading_position,
            MR.strings.pref_category_reader,
            MR.strings.pref_category_reading,
        ),
        "pref_torrserver_enable" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_torrents_enable,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_torrents,
        ),
        "pref_torrserver_port" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_torrents_port,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_torrents,
        ),
        "pref_torrserver_tackers" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_torrents_trackers,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_torrents,
        ),
        "pref_torrserver_proxymode" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_torrents_proxy_mode,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_torrents,
        ),
        "pref_torrserver_proxyurl" to aniyomi(
            MR.strings.backup_conversion_aniyomi_pref_player_torrents_proxy_url,
            MR.strings.pref_category_player,
            MR.strings.backup_conversion_aniyomi_place_pref_player_torrents,
        ),
    )

    /** Followed by a tracker's id: the anime library's filter by each tracker. */
    const val ANIME_LIBRARY_TRACKED_FILTER_PREFIX = "pref_filter_animelib_tracked_"

    val ANIME_LIBRARY_TRACKED_FILTER = aniyomi(
        MR.strings.backup_conversion_mihon_tracked,
        MR.strings.backup_conversion_aniyomi_place_anime_library,
        MR.strings.action_filter,
    )

    /**
     * Kept by the app rather than chosen by a person: remembered values, counters, one-time notices and keys Aniyomi
     * no longer reads; its display profile, a file on the device it was set on; and settings older Aniyomi renamed or
     * moved out of backups (EnumsMigration, VideoPlayerPreferenceMigration and NoAppStateMigration), which a backup
     * made before those ran still holds.
     */
    val IGNORED = setOf(
        "player_brightness_value",
        "player_volume_value",
        "pref_player_aspect_state",
        "pref_subtitles_secondary_delay",
        "pref_player_statistics_page",
        "library_random_anime_sort_seed",
        "library_unread_updates_count",
        "library_unseen_updates_count",
        "animeext_updates_count",
        "anime_extension_repos",
        "extension_repos",
        "last_ext_check",
        "pref_torrserver_shownotice",
        "backup_flags",
        "pref_display_profile_key",
        "pref_sub_select_conf",
        "pref_deband",
        "pref_player_view_mode",
        "gpu_next",
        "pref_hwdec",
        "pref_invert_playback_txt",
        "pref_invert_duration_txt",
        "pref_download_only",
        "incognito_mode",
        "last_catalogue_source",
        "trusted_signatures",
        "last_app_closed",
        "library_update_last_timestamp",
        "last_used_category",
        "last_app_check",
        "last_version_code",
        "storage_dir",
    )

    private fun aniyomi(label: StringResource, vararg place: StringResource) =
        OtherAppSettingLabel(label, place.toList())
}
