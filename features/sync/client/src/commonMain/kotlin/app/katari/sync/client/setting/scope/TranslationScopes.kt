package app.katari.sync.client.setting.scope

/**
 * Translation: `EntryTranslatePreferences`, `ProfileTranslationPreferences`, `TranslatorLanguagePreferences`, the book
 * reader's automatic selection translation and the translation servers' connections. Languages and what to translate
 * sync. Engines, servers and their keys stay on the device, as does everything that only works with them: automatic
 * selection translation, per settings surface.
 */
internal val TranslationScopes = PreferenceScopeTable(SettingsArea.TRANSLATION) {
    // EntryTranslatePreferences, in the store all profiles share
    person("translate_new_chapters", "translate_ahead_while_reading")
    device(DeviceBound.NETWORK, "translate_only_while_charging", "translate_only_over_wifi")
    device(DeviceBound.APP, "translate_queue_paused")

    person(
        "translation_target_language",
        "translation_recent_languages",
        "translator_source_language",
        "translator_target_language",
    )
    device(DeviceBound.INSTALLED, "translation_engine")

    device(DeviceBound.INSTALLED, "translation_automatic_selection_enabled")
    devicePrefix(DeviceBound.INSTALLED, "translation_automatic_selection_enabled:")
    device(DeviceBound.APP, "translation_automatic_selection_migrated_surfaces")

    // ServerConnectionConfiguration, each server's own file
    device(DeviceBound.INSTALLED, "endpoint", "verified-endpoint", "provider-disclosure-accepted")
}
