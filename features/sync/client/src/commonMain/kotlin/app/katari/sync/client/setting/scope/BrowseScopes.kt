package app.katari.sync.client.setting.scope

/** Sources, extensions, feeds and migration: `SourcePreferences` and `GlobalSourcePreferences`. */
internal val BrowseScopes = PreferenceScopeTable {
    // SourcePreferences
    person(
        "enabled_content_warnings",
        "hide_not_rated_extensions",
        "apply_content_warnings_to_installed",
        "pref_display_mode_catalogue",
        "source_languages",
        "hidden_sources",
        "pinned_sources",
        "incognito_extensions",
        "browse_hide_in_library_items",
        "saved_feed_presets",
        "saved_source_feeds",
        "pref_migration_sorting",
        "pref_migration_direction",
        "migration_sources",
        "migration_remove_downloads",
        "migration_deep_search",
        "migration_prioritize_by_chapters",
        "migration_hide_unmatched",
        "migration_hide_without_updates",
    )
    personPrefix("pref_display_mode_catalogue_")

    // Keys of earlier versions, read only to convert them: hidden and pinned sources before one set held them, the
    // 18+ switch before content warnings, the repositories before they had their own table, and the migration options
    // before only the download choice was remembered.
    device(
        "hidden_catalogues",
        "hidden_manga_catalogues",
        "hidden_anime_catalogues",
        "pinned_catalogues",
        "pinned_anime_catalogues",
        "show_nsfw_source",
        "extension_repos",
        "migration_flags",
    )

    // GlobalSourcePreferences: how many installed extensions have updates
    device("ext_updates_count")
}
