package app.katari.sync.client.setting.scope

/** `ProfileTextRecognitionPreferences`: providers and pipelines are installed per device. */
internal val TextRecognitionScopes = PreferenceScopeTable {
    device("text_recognition_provider", "text_recognition_override_languages")
    devicePrefix("text_recognition_pipeline_")
}
