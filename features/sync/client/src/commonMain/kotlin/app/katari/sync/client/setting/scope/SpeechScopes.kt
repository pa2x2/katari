package app.katari.sync.client.setting.scope

/** `ProfileTtsPreferences`. Speed and pitch sync; engines and voices are the device's own. */
internal val SpeechScopes = PreferenceScopeTable(SettingsArea.TEXT_TO_SPEECH) {
    person("tts_speech_rate", "tts_pitch")
    device(
        DeviceBound.INSTALLED,
        "tts_engine",
        "tts_default_voice",
        "tts_voice_override_languages",
        "tts_allow_network_voices",
    )
    // The voice picked per language
    devicePrefix(DeviceBound.INSTALLED, "tts_voice_")
}
