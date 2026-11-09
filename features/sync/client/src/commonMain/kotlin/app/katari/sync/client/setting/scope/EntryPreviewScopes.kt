package app.katari.sync.client.setting.scope

/**
 * `EntryInteractionPreferences`: page and frame previews on entry screens, set in Browse. Their size follows the
 * screen unless the person syncs it.
 */
internal val EntryPreviewScopes = PreferenceScopeTable(SettingsArea.BROWSE) {
    person("enable_manga_preview", "manga_preview_page_count", "enable_anime_preview", "anime_preview_page_count")
    deviceByDefault("manga_preview_size", "anime_preview_size")
}
