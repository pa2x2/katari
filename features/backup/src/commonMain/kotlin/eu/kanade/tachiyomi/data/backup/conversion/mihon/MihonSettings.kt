package eu.kanade.tachiyomi.data.backup.conversion.mihon

import app.katari.entry.interactions.reader.settings.ChapterTransitionMode
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.core.common.preference.TriState

/**
 * What becomes of each app setting a Mihon backup holds (D026, D040-D042), from the table of every key Mihon's
 * preference classes write at mihonapp/mihon f8e30a860860 and its Katari counterpart (workspace research/05).
 */
internal object MihonSettings {
    /**
     * Settings Katari has under the same key, type and meaning, and those whose key restoring already converts
     * because released Katari wrote it too (`Legacy*Conversion`s in PreferenceRestorer).
     */
    val CARRIED = setOf(
        "app_date_format",
        "app_time_format",
        "apply_content_warnings_to_installed",
        "auto_clear_chapter_cache",
        "auto_download_while_reading",
        "auto_update_metadata",
        "backup_interval",
        "browse_hide_in_library_items",
        "categorized_display",
        "color_filter_mode",
        "color_filter_value",
        "create_folder_per_manga",
        "crop_borders",
        "crop_borders_webtoon",
        "custom_brightness_value",
        "cutout_short",
        "default_category",
        "default_chapter_display_by_name_or_number",
        "default_chapter_filter_by_bookmarked",
        "default_chapter_filter_by_downloaded",
        "default_chapter_filter_by_read",
        "default_chapter_sort_by_ascending_or_descending",
        "default_chapter_sort_by_source_or_number",
        "default_user_agent",
        "disallow_non_ascii_filenames",
        "display_category_tabs",
        "display_continue_reading_button",
        "display_download_badge",
        "display_language_badge",
        "display_local_badge",
        "display_number_of_items",
        "display_unread_badge",
        "doh_provider",
        "download_new",
        "download_new_categories",
        "download_new_categories_exclude",
        "download_new_unread_chapters_only",
        "download_parallel_page_limit",
        "download_parallel_source_limit",
        "enabled_content_warnings",
        "extension_installer",
        "hide_notification_content",
        "incognito_extensions",
        "landscape_zoom",
        "library_show_updates_count",
        "library_sorting_mode",
        "library_update_categories",
        "library_update_categories_exclude",
        "library_update_manga_restriction",
        "library_update_restriction",
        "lock_app_after",
        "mark_duplicate_read_chapter_read",
        "migration_deep_search",
        "migration_flags",
        "migration_hide_unmatched",
        "migration_hide_without_updates",
        "migration_prioritize_by_chapters",
        "migration_sources",
        "navigate_pan",
        "pref_app_theme",
        "pref_chapter_swipe_end_action",
        "pref_chapter_swipe_start_action",
        "pref_color_filter_key",
        "pref_custom_brightness_key",
        "pref_default_orientation_type_key",
        "pref_default_reading_mode_key",
        "pref_display_mode_catalogue",
        "pref_display_mode_library",
        "pref_double_tap_anim_speed",
        "pref_download_only_over_wifi_key",
        "pref_dual_page_invert",
        "pref_dual_page_invert_webtoon",
        "pref_dual_page_rotate",
        "pref_dual_page_rotate_invert",
        "pref_dual_page_rotate_invert_webtoon",
        "pref_dual_page_rotate_webtoon",
        "pref_dual_page_split",
        "pref_dual_page_split_webtoon",
        "pref_enable_double_tap_zoom_webtoon",
        "pref_enable_chapter_name_hash",
        "pref_enable_transitions_key",
        "pref_filter_library_bookmarked_v2",
        "pref_filter_library_completed_v2",
        "pref_filter_library_downloaded_v2",
        "pref_filter_library_interval_custom",
        "pref_filter_library_unread_v2",
        "pref_filter_upcoming_excluded_categories",
        "pref_filter_upcoming_included_categories",
        "pref_filter_updates_bookmarked",
        "pref_filter_updates_downloaded",
        "pref_filter_updates_excluded_categories",
        "pref_filter_updates_included_categories",
        "pref_filter_updates_started",
        "pref_filter_updates_unread",
        "pref_grayscale",
        "pref_hide_missing_chapter_indicators",
        "pref_image_scale_type_key",
        "pref_inverted_colors",
        "pref_keep_screen_on_key",
        "pref_library_columns_portrait_key",
        "pref_library_update_interval_key",
        "pref_migration_direction",
        "pref_migration_sorting",
        "pref_reader_flash",
        "pref_reader_flash_duration",
        "pref_reader_flash_interval",
        "pref_reader_flash_mode",
        "pref_reader_load_ahead_only_over_wifi",
        "pref_reader_theme_key",
        "pref_remove_after_marked_as_read_key",
        "pref_remove_bookmarked",
        "pref_render_images_description",
        "pref_show_page_number_key",
        "pref_show_reading_mode",
        "pref_theme_dark_amoled_key",
        "pref_theme_mode_key",
        "pref_update_library_manga_titles",
        "pref_vertical_navigator",
        "pref_vertical_navigator_height",
        "pref_vertical_navigator_on_left",
        "pref_zoom_start_key",
        "reader_hide_threshold",
        "reader_long_tap",
        "reader_navigation_mode_pager",
        "reader_navigation_mode_webtoon",
        "reader_navigation_overlay_on_start",
        "reader_tapping_inverted",
        "reader_tapping_inverted_webtoon",
        "reader_volume_keys",
        "reader_volume_keys_inverted",
        "redact_sensitive_headers",
        "relative_time_v2",
        "remove_after_read_slots",
        "remove_exclude_categories",
        "save_chapter_as_cbz",
        "secure_screen_v2",
        "show_nsfw_source",
        "skip_dupe",
        "skip_filtered",
        "skip_read",
        "source_languages",
        "use_biometric_lock",
        "verbose_logging",
        "webtoon_disable_zoom_out",
        "webtoon_side_padding",
    )

    /** Carried, but kept by the app rather than chosen by a person, so the report doesn't list them (D044). */
    val CARRIED_UNLISTED = setOf(
        "ext_updates_count",
        "reader_navigation_overlay_new_user",
        "library_random_sort_seed",
        "pref_hardware_bitmap_threshold",
    )

    /** Mihon settings Katari has nothing for: its WebGPU reader options and a few others. */
    val NO_COUNTERPART = setOf(
        "pref_high_quality_renderer_key",
        "pref_dual_page_view",
        "webgpu_transition_animation",
        "webgpu_dual_transition_animation",
        "webgpu_cutout_mode",
        "webgpu_dual_cutout_mode",
        "webgpu_continuous_minwidth",
        "webgpu_continuous_gap",
        "webgpu_fast_rendering_only",
        "webgpu_hold_until_final",
        "webgpu_crop_instead_of_zoom",
        "webgpu_disable_double_tap_gestures",
        "webgpu_image_filters",
        "webgpu_tile_filters",
        "webgpu_upscaler",
        "webgpu_downscaler",
        "pref_filter_updates_hide_excluded_scanlators",
        "pref_library_columns_landscape_key",
        "pref_webtoon_vertical_navigator",
        "pref_webtoon_vertical_navigator_on_left",
    )

    /** Consent given to Mihon, which isn't consent to Katari (D040). */
    val CONSENT = setOf("analytics", "crashlytics")

    /** Keys of trackers' settings, some followed by the tracker's id; Katari has no trackers. */
    val TRACKER_KEYS = setOf("anilist_score_type", "kitsu_score_type", "mangabaka_score_type")
    val TRACKER_KEY_PREFIXES = listOf(
        "pref_auto_update_manga_sync_key",
        "pref_auto_update_manga_on_mark_read",
        "pref_filter_library_tracked_",
        "__PRIVATE_pref_mangasync_",
        "__PRIVATE_pref_tracker_auth_expired_",
        "__PRIVATE_track_token_",
    )

    /** Mihon's older key for its extension repos, which travel as stores instead. */
    val IGNORED = setOf("extension_repos")

    /**
     * The Katari settings [preference] becomes when Katari keeps it under another key or shape, empty where it becomes
     * none; null for a setting Katari doesn't rename.
     */
    fun renamed(preference: BackupPreference): List<BackupPreference>? {
        val (key, value) = preference
        return when (key) {
            SourcePreferences.LEGACY_HIDDEN_SOURCES_KEY -> {
                listOf(BackupPreference(SourcePreferences.HIDDEN_SOURCES_KEY, value))
            }
            SourcePreferences.MANGA_PINNED_SOURCES_KEY -> {
                listOf(BackupPreference(SourcePreferences.PINNED_SOURCES_KEY, value))
            }
            // Mihon's fullscreen hid both bars; Katari has a switch for each, which hide the bars unless turned on
            "fullscreen" -> if ((value as? BooleanPreferenceValue)?.value == false) {
                listOf(
                    BackupPreference(MangaReaderSettings.SHOW_STATUS_BAR_PREFERENCE_KEY, BooleanPreferenceValue(true)),
                    BackupPreference(
                        MangaReaderSettings.SHOW_NAVIGATION_BAR_PREFERENCE_KEY,
                        BooleanPreferenceValue(true),
                    ),
                )
            } else {
                emptyList()
            }
            "always_show_chapter_transition" -> {
                val always = (value as? BooleanPreferenceValue)?.value ?: return emptyList()
                val mode = if (always) ChapterTransitionMode.ALWAYS else ChapterTransitionMode.WHEN_NEEDED
                listOf(
                    BackupPreference(
                        MangaReaderSettings.CHAPTER_TRANSITION_PREFERENCE_KEY,
                        StringPreferenceValue(mode.name),
                    ),
                )
            }
            // Mihon filters on started, Katari on not started: the same entries, the opposite sense
            "pref_filter_library_started_v2" -> {
                val state = (value as? StringPreferenceValue)?.value ?: return emptyList()
                val inverted = when (state) {
                    TriState.ENABLED_IS.name -> TriState.ENABLED_NOT
                    TriState.ENABLED_NOT.name -> TriState.ENABLED_IS
                    else -> TriState.DISABLED
                }
                listOf(BackupPreference("pref_filter_library_not_started", StringPreferenceValue(inverted.name)))
            }
            // Katari has no Always or In landscape (D042)
            TABLET_UI_MODE_KEY -> {
                val mode = (value as? StringPreferenceValue)?.value ?: return emptyList()
                val katariMode = TabletUiMode.entries.find { it.name == mode } ?: TabletUiMode.AUTOMATIC
                listOf(BackupPreference(key, StringPreferenceValue(katariMode.name)))
            }
            else -> null
        }
    }

    const val TABLET_UI_MODE_KEY = "tablet_ui_mode"
}
