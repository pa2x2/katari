package app.katari.backupconversion

import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.platform.presentation.platformPresentation
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.i18n.*

/** What Katari calls a setting and where it is: its Settings screen and group, or the menu that sets it. */
internal class KatariSettingName(val title: StringResource, val place: List<StringResource>)

/**
 * Katari's name for each setting a converted backup can bring over, by its key (D043, D053). A setting with a row in
 * Settings takes the row's title; one set from a menu, a sheet or the reader takes the label it has there, or a name
 * of its own where nothing there names it (D054). Kept by hand rather than read from the Settings screens, which leave
 * out rows the device has no use for, such as volume keys on a desktop.
 */
internal val katariSettingNames: Map<String, KatariSettingName> = buildMap {
    fun put(place: List<StringResource>, vararg names: Pair<String, StringResource>) {
        names.forEach { (key, title) -> put(key, KatariSettingName(title, place)) }
    }

    val general = MR.strings.pref_category_general
    put(
        listOf(general, MR.strings.pref_category_language_and_region),
        "app_date_format" to MR.strings.pref_date_format,
        "app_time_format" to MR.strings.pref_time_format,
        "relative_time_v2" to MR.strings.pref_relative_format,
    )

    val appearance = MR.strings.pref_category_appearance
    put(
        listOf(appearance, MR.strings.pref_category_theme),
        "pref_app_theme" to MR.strings.pref_app_theme,
        "pref_theme_dark_amoled_key" to MR.strings.pref_dark_theme_pure_black,
    )
    put(
        listOf(appearance, MR.strings.pref_category_theme, MR.strings.pref_app_theme),
        "pref_theme_mode_key" to MR.strings.backup_conversion_setting_light_or_dark,
    )
    put(
        listOf(appearance, MR.strings.pref_category_display),
        "pref_render_images_description" to MR.strings.pref_display_images_description,
        "tablet_ui_mode" to MR.strings.pref_tablet_ui_mode,
    )

    val library = MR.strings.pref_category_library
    put(
        listOf(library),
        "pref_display_mode_library" to MR.strings.action_display_mode,
        "pref_library_columns_portrait_key" to MR.strings.pref_library_cover_size,
    )
    put(
        listOf(library, MR.strings.categories),
        "categorized_display" to MR.strings.categorized_display_settings,
        "default_category" to MR.strings.default_category,
    )
    put(
        listOf(library, MR.strings.overlay_header),
        "display_download_badge" to MR.strings.action_display_download_badge,
        "display_language_badge" to MR.strings.action_display_language_badge,
        "display_local_badge" to MR.strings.action_display_local_badge,
        "display_unread_badge" to MR.strings.action_display_unseen_badge,
    )
    put(
        listOf(library, MR.strings.library_cover_actions_header),
        "display_continue_reading_button" to MR.strings.action_display_show_continue_button,
    )
    put(
        listOf(library, MR.strings.action_group),
        "display_category_tabs" to MR.strings.action_display_show_tabs,
        "display_number_of_items" to MR.strings.action_display_show_number_of_items,
    )
    put(
        listOf(library, MR.strings.pref_chapter_lists),
        "pref_chapter_swipe_end_action" to platformPresentation.chapterSwipeToEndLabel,
        "pref_chapter_swipe_start_action" to platformPresentation.chapterSwipeToStartLabel,
        "mark_duplicate_read_chapter_read" to MR.strings.pref_mark_duplicate_read_chapter_read,
        "pref_hide_missing_chapter_indicators" to MR.strings.pref_hide_missing_chapter_indicators,
    )
    put(
        listOf(MR.strings.label_library, MR.strings.action_filter),
        "pref_filter_library_bookmarked_v2" to MR.strings.action_filter_bookmarked,
        "pref_filter_library_completed_v2" to MR.strings.completed,
        "pref_filter_library_downloaded_v2" to MR.strings.label_downloaded,
        "pref_filter_library_interval_custom" to MR.strings.action_filter_interval_custom,
        "pref_filter_library_unread_v2" to MR.strings.action_filter_unread,
        "pref_filter_library_not_started" to MR.strings.label_not_started,
    )
    put(
        listOf(MR.strings.label_library, MR.strings.action_sort),
        "library_sorting_mode" to MR.strings.backup_conversion_setting_library_sort,
    )

    val chapterSettings = MR.strings.chapter_settings
    put(
        listOf(chapterSettings, MR.strings.action_filter),
        "default_chapter_filter_by_bookmarked" to MR.strings.action_filter_bookmarked,
        "default_chapter_filter_by_downloaded" to MR.strings.label_downloaded,
        "default_chapter_filter_by_read" to MR.strings.action_filter_unread,
    )
    // The sort and its direction are one choice in the chapter menu
    put(
        listOf(chapterSettings, MR.strings.action_sort),
        "default_chapter_sort_by_source_or_number" to MR.strings.backup_conversion_setting_chapter_sort,
        "default_chapter_sort_by_ascending_or_descending" to MR.strings.backup_conversion_setting_chapter_sort,
    )
    put(
        listOf(chapterSettings, MR.strings.action_filter),
        "default_chapter_filter_by_filler" to MR.strings.chapter_kind_filler,
    )
    put(
        listOf(chapterSettings, MR.strings.action_display),
        "default_chapter_display_by_name_or_number" to MR.strings.backup_conversion_setting_chapter_titles,
        "default_chapter_show_previews" to MR.strings.chapter_previews_and_summaries,
    )

    put(
        listOf(MR.strings.label_recent_updates, MR.strings.action_filter),
        "pref_filter_updates_bookmarked" to MR.strings.action_filter_bookmarked,
        "pref_filter_updates_downloaded" to MR.strings.label_downloaded,
        "pref_filter_updates_started" to MR.strings.label_started,
        "pref_filter_updates_unread" to MR.strings.action_filter_unseen,
        // Included and excluded categories are one list of three-way choices
        "pref_filter_updates_excluded_categories" to MR.strings.categories,
        "pref_filter_updates_included_categories" to MR.strings.categories,
    )
    put(
        listOf(MR.strings.label_upcoming, MR.strings.action_filter),
        "pref_filter_upcoming_excluded_categories" to MR.strings.categories,
        "pref_filter_upcoming_included_categories" to MR.strings.categories,
    )

    val libraryUpdates = MR.strings.pref_category_library_updates
    put(
        listOf(libraryUpdates, MR.strings.library_updates_schedule),
        "pref_library_update_interval_key" to MR.strings.pref_library_update_interval,
        "library_update_restriction" to MR.strings.pref_library_update_restriction,
    )
    put(
        listOf(libraryUpdates, MR.strings.library_updates_also),
        "auto_update_metadata" to MR.strings.pref_library_update_refresh_metadata,
        "library_show_updates_count" to MR.strings.pref_library_update_show_tab_badge,
    )
    // Restoring turns the included and excluded categories into each category's own rule
    put(
        listOf(libraryUpdates, MR.strings.categories),
        "library_update_categories" to MR.strings.library_updates_check_automatically,
        "library_update_categories_exclude" to MR.strings.library_updates_check_automatically,
    )
    put(
        listOf(libraryUpdates),
        "library_update_manga_restriction" to MR.strings.library_updates_skip_when,
    )

    val downloads = MR.strings.pref_category_downloads
    put(
        listOf(downloads),
        "pref_download_only_over_wifi_key" to platformPresentation.downloadOnlyOnUnmeteredLabel,
    )
    put(
        listOf(downloads, MR.strings.pref_category_download_behavior),
        "save_chapter_as_cbz" to MR.strings.save_chapter_as_cbz,
        "download_parallel_source_limit" to MR.strings.pref_download_concurrent_sources,
        "download_parallel_page_limit" to MR.strings.pref_download_concurrent_pages,
    )
    put(
        listOf(downloads, MR.strings.pref_category_delete_chapters),
        "pref_remove_after_marked_as_read_key" to MR.strings.pref_remove_after_marked_as_read,
        "remove_after_read_slots" to MR.strings.pref_remove_after_read,
        "pref_remove_bookmarked" to MR.strings.pref_remove_bookmarked_chapters,
        "remove_exclude_categories" to MR.strings.pref_remove_exclude_categories,
    )
    // Included and excluded categories are one list of three-way choices
    put(
        listOf(downloads, MR.strings.pref_category_auto_download),
        "download_new" to MR.strings.pref_download_new,
        "download_new_unread_chapters_only" to MR.strings.pref_download_new_unread_chapters_only,
        "download_new_categories" to MR.strings.categories,
        "download_new_categories_exclude" to MR.strings.categories,
    )
    put(
        listOf(downloads, MR.strings.download_ahead),
        "auto_download_while_reading" to MR.strings.auto_download_while_reading,
    )

    val browse = MR.strings.browse
    put(
        listOf(browse, MR.strings.label_sources),
        "browse_hide_in_library_items" to MR.strings.pref_hide_in_library_items,
        "pref_display_mode_catalogue" to MR.strings.action_display_mode,
        "source_languages" to MR.strings.backup_conversion_setting_source_languages,
        SourcePreferences.HIDDEN_SOURCES_KEY to MR.strings.backup_conversion_setting_hidden_sources,
        SourcePreferences.PINNED_SOURCES_KEY to MR.strings.backup_conversion_setting_pinned_sources,
    )
    // Restoring turns Mihon's switch for adult sources into the content warnings allowed
    put(
        listOf(browse, MR.strings.pref_category_extensions),
        "enabled_content_warnings" to MR.strings.pref_allowed_content_warnings,
        "show_nsfw_source" to MR.strings.pref_allowed_content_warnings,
        "apply_content_warnings_to_installed" to MR.strings.pref_apply_content_warnings_to_installed,
        "incognito_extensions" to MR.strings.pref_incognito_mode,
    )
    // Of the options Mihon migrated, restoring keeps deleting the old entry's downloads
    put(
        listOf(browse, MR.strings.label_migration),
        "migration_sources" to MR.strings.migrationConfigScreen_title,
        "migration_flags" to MR.strings.migrationConfigScreen_removeDownloadsTitle,
        "migration_deep_search" to MR.strings.migrationConfigScreen_deepSearchModeTitle,
        "migration_hide_unmatched" to MR.strings.migrationConfigScreen_hideUnmatchedTitle,
        "migration_hide_without_updates" to MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle,
        "migration_prioritize_by_chapters" to MR.strings.migrationConfigScreen_prioritizeByChaptersTitle,
        "pref_migration_sorting" to MR.strings.backup_conversion_setting_migration_order,
        "pref_migration_direction" to MR.strings.backup_conversion_setting_migration_order,
    )

    val data = MR.strings.label_data_storage
    put(listOf(data, MR.strings.label_backup), "backup_interval" to MR.strings.pref_backup_interval)
    put(
        listOf(data, MR.strings.pref_storage_usage),
        "auto_clear_chapter_cache" to MR.strings.pref_auto_clear_chapter_cache,
    )

    val animePlayer = listOf(MR.strings.pref_category_players, MR.strings.pref_anime_player)
    put(
        animePlayer + MR.strings.pref_anime_playback,
        AnimePlayerPreferences.ORIENTATION_KEY to MR.strings.pref_anime_orientation,
        AnimePlayerPreferences.AUTO_PLAY_NEXT_KEY to MR.strings.pref_anime_auto_play_next,
        AnimePlayerPreferences.MARKER_SKIP_KEY to MR.strings.anime_marker_skip,
        AnimePlayerPreferences.FIND_INTRO_TIMES_ONLINE_KEY to MR.strings.pref_anime_find_intro_times_online,
        AnimePlayerPreferences.DEFAULT_PLAYBACK_SPEED_PERCENT_KEY to MR.strings.pref_anime_default_speed,
        AnimePlayerPreferences.AUTO_ENTER_PICTURE_IN_PICTURE_KEY to MR.strings.pref_anime_auto_enter_picture_in_picture,
        AnimePlayerPreferences.SEEK_STEP_SECONDS_KEY to MR.strings.pref_anime_seek_step,
    )
    put(
        animePlayer + MR.strings.pref_anime_subtitles + MR.strings.pref_anime_default_subtitle_look,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_POSITION_KEY to MR.strings.anime_playback_subtitle_position,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE_KEY to MR.strings.anime_playback_subtitle_size,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR_KEY to MR.strings.anime_playback_subtitle_text_color,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_COLOR_KEY to
            MR.strings.anime_playback_subtitle_background_color,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_OPACITY_KEY to
            MR.strings.anime_playback_subtitle_background_opacity,
        AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_EDGE_KEY to MR.strings.anime_playback_subtitle_text_edge,
    )
    put(
        animePlayer + MR.strings.pref_anime_gestures,
        AnimePlayerDevicePreferences.REMEMBER_BRIGHTNESS_KEY to MR.strings.pref_anime_remember_brightness,
    )
    // Set in the player, by choosing its time
    put(
        listOf(MR.strings.pref_anime_player),
        AnimePlayerPreferences.SHOWS_REMAINING_TIME_KEY to MR.strings.anime_playback_show_remaining_time,
    )

    val reader = MR.strings.pref_manga_reader
    put(
        listOf(reader),
        "pref_default_reading_mode_key" to MR.strings.pref_viewer_type,
        "pref_double_tap_anim_speed" to platformPresentation.doubleTapAnimationSpeedLabel,
        "pref_show_reading_mode" to MR.strings.pref_show_reading_mode,
        "reader_navigation_overlay_on_start" to platformPresentation.showPageZonesOverlayLabel,
        "pref_enable_transitions_key" to MR.strings.pref_page_transitions,
        "pref_reader_load_ahead_only_over_wifi" to platformPresentation.loadAheadOnlyOnUnmeteredLabel,
        // E-Ink screens' flashes
        "pref_reader_flash" to MR.strings.pref_flash_page,
        "pref_reader_flash_duration" to MR.strings.pref_flash_duration,
        "pref_reader_flash_interval" to MR.strings.pref_flash_page_interval,
        "pref_reader_flash_mode" to MR.strings.pref_flash_with,
    )
    put(
        listOf(reader, MR.strings.pref_category_display),
        "pref_default_orientation_type_key" to MR.strings.pref_rotation_type,
        "pref_reader_theme_key" to MR.strings.pref_reader_theme,
        MangaReaderSettings.SHOW_STATUS_BAR_PREFERENCE_KEY to MR.strings.pref_reader_show_status_bar,
        MangaReaderSettings.SHOW_NAVIGATION_BAR_PREFERENCE_KEY to MR.strings.pref_reader_show_navigation_bar,
        "cutout_short" to MR.strings.pref_cutout_short,
        "pref_keep_screen_on_key" to MR.strings.pref_keep_screen_on,
        "pref_show_page_number_key" to MR.strings.pref_show_page_number,
    )
    put(
        listOf(reader, MR.strings.pref_category_reading),
        "skip_read" to MR.strings.pref_skip_read_chapters,
        "skip_filtered" to MR.strings.pref_skip_filtered_chapters,
        "skip_dupe" to MR.strings.pref_skip_dupe_chapters,
        MangaReaderSettings.CHAPTER_TRANSITION_PREFERENCE_KEY to MR.strings.pref_chapter_transition,
    )
    put(
        listOf(reader, MR.strings.pager_viewer),
        "reader_navigation_mode_pager" to platformPresentation.pageZonesLabel,
        "reader_tapping_inverted" to platformPresentation.invertPageZonesLabel,
        "pref_image_scale_type_key" to MR.strings.pref_image_scale_type,
        "pref_zoom_start_key" to MR.strings.pref_zoom_start,
        "crop_borders" to MR.strings.pref_crop_borders,
        "landscape_zoom" to MR.strings.pref_landscape_zoom,
        "navigate_pan" to MR.strings.pref_navigate_pan,
        "pref_dual_page_split" to MR.strings.pref_dual_page_split,
        "pref_dual_page_invert" to MR.strings.pref_dual_page_invert,
        "pref_dual_page_rotate" to MR.strings.pref_page_rotate,
        "pref_dual_page_rotate_invert" to MR.strings.pref_page_rotate_invert,
    )
    put(
        listOf(reader, MR.strings.webtoon_viewer),
        "reader_navigation_mode_webtoon" to platformPresentation.pageZonesLabel,
        "reader_tapping_inverted_webtoon" to platformPresentation.invertPageZonesLabel,
        "webtoon_side_padding" to MR.strings.pref_webtoon_side_padding,
        "reader_hide_threshold" to MR.strings.pref_hide_threshold,
        "crop_borders_webtoon" to MR.strings.pref_crop_borders,
        "pref_dual_page_split_webtoon" to MR.strings.pref_dual_page_split,
        "pref_dual_page_invert_webtoon" to MR.strings.pref_dual_page_invert,
        "pref_dual_page_rotate_webtoon" to MR.strings.pref_page_rotate,
        "pref_dual_page_rotate_invert_webtoon" to MR.strings.pref_page_rotate_invert,
        "pref_enable_double_tap_zoom_webtoon" to platformPresentation.doubleTapZoomLabel,
        "webtoon_disable_zoom_out" to MR.strings.pref_webtoon_disable_zoom_out,
    )
    put(
        listOf(reader, MR.strings.pref_reader_navigation),
        "reader_volume_keys" to MR.strings.pref_read_with_volume_keys,
        "reader_volume_keys_inverted" to MR.strings.pref_read_with_volume_keys_inverted,
        "pref_vertical_navigator" to MR.strings.pref_vertical_navigator,
        "pref_vertical_navigator_on_left" to MR.strings.pref_webtoon_vertical_navigator_on_left,
        "pref_vertical_navigator_height" to MR.strings.pref_vertical_navigator_height,
    )
    put(
        listOf(reader, MR.strings.pref_reader_actions),
        "reader_long_tap" to (
            platformPresentation.pageActionsOnLongPressLabel ?: MR.strings.backup_conversion_setting_page_actions
            ),
        "create_folder_per_manga" to MR.strings.pref_create_folder_per_manga,
    )
    // Set in the reader's own settings, on its custom filter tab
    put(
        listOf(reader, MR.strings.custom_filter),
        "pref_custom_brightness_key" to MR.strings.pref_custom_brightness,
        "custom_brightness_value" to MR.strings.backup_conversion_setting_brightness_level,
        "pref_color_filter_key" to MR.strings.pref_custom_color_filter,
        "color_filter_value" to MR.strings.backup_conversion_setting_color_filter_color,
        "color_filter_mode" to MR.strings.pref_color_filter_mode,
        "pref_grayscale" to MR.strings.pref_grayscale,
        "pref_inverted_colors" to MR.strings.pref_inverted_colors,
    )

    val security = MR.strings.pref_category_security
    put(
        listOf(security, MR.strings.pref_security),
        "use_biometric_lock" to MR.strings.lock_with_biometrics,
        "lock_app_after" to MR.strings.lock_when_idle,
        "secure_screen_v2" to MR.strings.secure_screen,
    )
    put(
        listOf(MR.strings.pref_category_notifications),
        "hide_notification_content" to MR.strings.hide_notification_content,
    )

    val advanced = MR.strings.pref_category_advanced
    put(
        listOf(advanced),
        "verbose_logging" to MR.strings.pref_verbose_logging,
        "redact_sensitive_headers" to MR.strings.pref_redact_sensitive_headers,
    )
    put(
        listOf(advanced, MR.strings.label_network),
        "doh_provider" to MR.strings.pref_dns_over_https,
        "default_user_agent" to MR.strings.pref_user_agent_string,
    )
    put(
        listOf(advanced, MR.strings.label_library),
        "pref_update_library_manga_titles" to MR.strings.pref_update_library_manga_titles,
        "disallow_non_ascii_filenames" to MR.strings.pref_disallow_non_ascii_filenames,
        "pref_enable_chapter_name_hash" to MR.strings.pref_download_name_hash,
    )
    put(listOf(advanced, MR.strings.label_extensions), "extension_installer" to MR.strings.ext_installer_pref)
}
