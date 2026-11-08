package app.katari.tts.runtime.selection

import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.runtime.preference.ProfileTtsPreferences

internal class ProfileTtsEngineResolver(
    private val preferences: ProfileTtsPreferences,
) {
    fun resolve(): TtsEngineId? {
        if (!preferences.engine.isSet()) return null
        return preferences.engine.get()
    }
}
