package mihon.tts.runtime.selection

import mihon.tts.api.engine.TtsEngineId
import mihon.tts.runtime.preference.ProfileTtsPreferences

internal class ProfileTtsEngineResolver(
    private val preferences: ProfileTtsPreferences,
) {
    fun resolve(): TtsEngineId? {
        if (!preferences.engine.isSet()) return null
        return preferences.engine.get()
    }
}
