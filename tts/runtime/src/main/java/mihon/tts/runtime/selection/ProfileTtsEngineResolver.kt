package mihon.tts.runtime.selection

import mihon.tts.api.engine.TtsEngineId
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.spi.engine.TtsEngineRegistry

internal class ProfileTtsEngineResolver(
    private val preferences: ProfileTtsPreferences,
    private val registry: TtsEngineRegistry,
) {
    fun resolve(): TtsEngineId? {
        return if (preferences.engine.isSet()) {
            preferences.engine.get()
        } else {
            registry.engines.firstOrNull()?.catalogEntry?.id
        }
    }

    fun isExplicitlySelected(): Boolean = preferences.engine.isSet()
}
