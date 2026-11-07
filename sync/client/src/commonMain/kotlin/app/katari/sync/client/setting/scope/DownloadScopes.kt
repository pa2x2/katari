package app.katari.sync.client.setting.scope

/**
 * Downloads and the device's caches: `DownloadPreferences`, `BackupPreferences` and the media caches' auto-clear
 * switches. Downloads never leave the device, and the rules that start them are kept in the store all profiles share
 * with category IDs of whichever profile set them, so they stay too.
 */
internal val DownloadScopes = PreferenceScopeTable {
    device(
        "pref_download_only_over_wifi_key",
        "save_chapter_as_cbz",
        "auto_download_while_reading",
        "remove_after_read_slots",
        "pref_remove_after_marked_as_read_key",
        "pref_remove_bookmarked",
        "remove_exclude_categories",
        "download_new",
        "download_new_categories",
        "download_new_categories_exclude",
        "download_new_unread_chapters_only",
        "download_parallel_source_limit",
        "download_parallel_page_limit",
    )

    device("backup_interval")

    // EntryMediaCacheAutoClearPreference keys of the manga, book and anime caches
    device("auto_clear_chapter_cache", "auto_clear_book_materialization_cache", "auto_clear_anime_playback_cache")
}
