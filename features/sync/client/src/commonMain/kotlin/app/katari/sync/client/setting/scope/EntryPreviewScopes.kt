package app.katari.sync.client.setting.scope

/** `EntryInteractionPreferences`: page and frame previews on entry screens. Their size follows the screen. */
internal val EntryPreviewScopes = PreferenceScopeTable {
    person("enable_manga_preview", "manga_preview_page_count", "enable_anime_preview", "anime_preview_page_count")
    device("manga_preview_size", "anime_preview_size")
}
