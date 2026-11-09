package app.katari.sync.client.setting.scope

/**
 * The library, its updates and the lists built from it: `LibraryPreferences`, `GlobalLibraryPreferences`,
 * `DuplicatePreferences`, `UpdatesPreferences` and `UpcomingPreferences`.
 */
internal val LibraryScopes = PreferenceScopeTable(SettingsArea.LIBRARY) {
    // LibraryPreferences: display, sorting, filters, grouping, badges and categories
    person(
        "pref_display_mode_library",
        "pref_library_pinned_display_style",
        "display_continue_reading_button",
        "pref_group_library_type",
        "display_download_badge",
        "display_unread_badge",
        "display_local_badge",
        "display_language_badge",
        "display_entry_type_badge",
        "display_merged_badge",
        "library_show_updates_count",
        "default_category",
        "display_category_tabs",
        "display_number_of_items",
        "categorized_display",
    )
    menu(MenuSettings.LIBRARY_SORT) {
        person("library_sorting_mode")
    }
    menu(MenuSettings.LIBRARY_FILTERS) {
        person(
            "pref_filter_library_downloaded_v2",
            "pref_filter_library_unread_v2",
            "pref_filter_library_bookmarked_v2",
            "pref_filter_library_completed_v2",
            "pref_filter_library_not_started",
            "pref_filter_library_interval_custom",
        )
    }
    // The cover size fits the screen; the random order's seed is redrawn on each device.
    deviceByDefault("pref_library_cover_size")
    device(DeviceBound.APP, "library_random_sort_seed")

    // LibraryPreferences: the rules of library updates sync, how often they run stays unless the person syncs it, and
    // on what network and power they run stays
    area(SettingsArea.LIBRARY_UPDATES) {
        person(
            "library_update_manga_restriction",
            "library_update_paused_sources",
            "library_update_excluded_entry_types",
            "auto_update_metadata",
            "pref_update_library_manga_titles",
        )
        deviceByDefault("pref_library_update_interval_key")
    }
    device(DeviceBound.NETWORK, "library_update_restriction")

    // LibraryPreferences: chapter list defaults and swipe actions
    menu(MenuSettings.CHAPTER_LIST_DEFAULTS) {
        person(
            "default_chapter_filter_by_read",
            "default_chapter_filter_by_downloaded",
            "default_chapter_filter_by_bookmarked",
            "default_chapter_filter_by_filler",
            "default_chapter_show_previews",
            "default_chapter_sort_by_source_or_number",
            "default_chapter_display_by_name_or_number",
            "default_chapter_sort_by_ascending_or_descending",
        )
    }
    person(
        "pref_hide_missing_chapter_indicators",
        "pref_chapter_swipe_end_action",
        "pref_chapter_swipe_start_action",
    )

    // Keys of earlier versions, read only to convert them
    device(
        DeviceBound.APP,
        "library_update_categories",
        "library_update_categories_exclude",
        "library_update_excluded_sources",
        "pref_library_columns_portrait_key",
        "pref_library_columns_landscape_key",
    )

    // GlobalLibraryPreferences, in the store all profiles share. File names follow the device's storage.
    person("mark_duplicate_read_chapter_read")
    device(DeviceBound.STORAGE, "disallow_non_ascii_filenames", "pref_enable_chapter_name_hash")

    // DuplicatePreferences
    person(
        "extended_duplicate_detection_enabled",
        "extended_duplicate_detection_minimum_match_score",
        "extended_duplicate_detection_description_weight",
        "extended_duplicate_detection_author_weight",
        "extended_duplicate_detection_artist_weight",
        "extended_duplicate_detection_cover_weight",
        "extended_duplicate_detection_genre_weight",
        "extended_duplicate_detection_status_weight",
        "extended_duplicate_detection_chapter_count_weight",
        "extended_duplicate_detection_title_weight",
        "extended_duplicate_detection_title_exclusion_patterns",
    )

    // UpdatesPreferences and UpcomingPreferences
    person(
        "pref_filter_updates_downloaded",
        "pref_filter_updates_unread",
        "pref_filter_updates_started",
        "pref_filter_updates_bookmarked",
        "pref_filter_updates_included_categories",
        "pref_filter_updates_excluded_categories",
        "pref_filter_updates_included_entry_types",
        "pref_filter_updates_excluded_entry_types",
        "pref_filter_updates_included_sources",
        "pref_filter_updates_excluded_sources",
        "pref_filter_upcoming_included_categories",
        "pref_filter_upcoming_excluded_categories",
    )
}
