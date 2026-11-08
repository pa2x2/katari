package app.katari.sync.client.setting.scope

/**
 * `MangaReaderPreferences`. How pages read syncs; what depends on the screen, the device's keys, its network and
 * power, or a one-off hint stays.
 */
internal val MangaReaderScopes = PreferenceScopeTable {
    person(
        "pref_enable_transitions_key",
        "pref_double_tap_anim_speed",
        "pref_show_page_number_key",
        "pref_vertical_navigator",
        "pref_vertical_navigator_on_left",
        "pref_show_reading_mode",
        "pref_default_reading_mode_key",
        "pref_enable_double_tap_zoom_webtoon",
        "pref_image_scale_type_key",
        "pref_zoom_start_key",
        "pref_reader_theme_key",
        "chapter_transition",
        "crop_borders",
        "crop_borders_webtoon",
        "navigate_pan",
        "landscape_zoom",
        "skip_read",
        "skip_filtered",
        "skip_dupe",
        "webtoon_disable_zoom_out",
        "reader_auto_scroll",
        "reader_auto_scroll_speed",
        "reader_page_text_translation_overlay",
        "pref_grayscale",
        "pref_inverted_colors",
        "reader_long_tap",
        "reader_navigation_mode_pager",
        "reader_navigation_mode_webtoon",
        "reader_tapping_inverted",
        "reader_tapping_inverted_webtoon",
        "reader_navigation_overlay_on_start",
    )

    // The screen: orientation, system bars, cutout, two-page spreads, padding and sizes in pixels
    device(
        "pref_default_orientation_type_key",
        "pref_reader_show_status_bar",
        "pref_reader_show_navigation_bar",
        "cutout_short",
        "pref_keep_screen_on_key",
        "pref_vertical_navigator_height",
        "webtoon_side_padding",
        "reader_hide_threshold",
        "pref_pager_page_layout",
        "pref_dual_page_split",
        "pref_dual_page_invert",
        "pref_dual_page_split_webtoon",
        "pref_dual_page_invert_webtoon",
        "pref_dual_page_rotate",
        "pref_dual_page_rotate_invert",
        "pref_dual_page_rotate_webtoon",
        "pref_dual_page_rotate_invert_webtoon",
        "pref_custom_brightness_key",
        "custom_brightness_value",
        "pref_color_filter_key",
        "color_filter_value",
        "color_filter_mode",
    )
    // E-ink refresh flashes
    device("pref_reader_flash", "pref_reader_flash_duration", "pref_reader_flash_interval", "pref_reader_flash_mode")
    // Hardware keys, network and power, saved pages and one-off hints
    device(
        "reader_volume_keys",
        "reader_volume_keys_inverted",
        "pref_reader_load_ahead_only_over_wifi",
        "reader_page_text_process_ahead_unmetered_only",
        "reader_page_text_process_ahead_charging_only",
        "create_folder_per_manga",
        "reader_input_guide_shown",
        "reader_navigation_overlay_new_user",
    )

    // Keys of earlier versions, read only by the migrations that converted them
    device(
        "pref_webtoon_vertical_navigator",
        "pref_webtoon_vertical_navigator_on_left",
        "always_show_chapter_transition",
    )

    // ReaderBasePreferences: the display's colour profile
    device("pref_display_profile_key")

    // ReaderChapterPreparationPreferences: preparing the next chapter ahead fetches it over the network, per settings
    // surface; the old key it was converted from and which surfaces were converted.
    device("reader_prepare_next_chapter", "reader_prepare_next_chapter_migrated_surfaces")
    devicePrefix("reader_prepare_next_chapter:")
}
