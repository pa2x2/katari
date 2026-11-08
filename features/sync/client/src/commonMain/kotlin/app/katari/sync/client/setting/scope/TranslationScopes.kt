package app.katari.sync.client.setting.scope

/**
 * Translation: `EntryTranslatePreferences`, `ProfileTranslationPreferences`, `TranslatorLanguagePreferences`, the book
 * reader's automatic selection translation and the translation servers' connections. Languages and what to translate
 * sync. Engines, servers and their keys stay on the device, as does everything that only works with them: automatic
 * selection translation, per settings surface.
 */
internal val TranslationScopes = PreferenceScopeTable {
    // EntryTranslatePreferences, in the store all profiles share
    person("translate_new_chapters", "translate_ahead_while_reading")
    device("translate_only_while_charging", "translate_only_over_wifi", "translate_queue_paused")

    person(
        "translation_target_language",
        "translation_recent_languages",
        "translator_source_language",
        "translator_target_language",
    )
    device("translation_engine")

    device("translation_automatic_selection_enabled", "translation_automatic_selection_migrated_surfaces")
    devicePrefix("translation_automatic_selection_enabled:")

    // ServerConnectionConfiguration, each server's own file
    device("endpoint", "verified-endpoint", "provider-disclosure-accepted")
}
