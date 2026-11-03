package mihon.translation.runtime.preference

import mihon.translation.api.engine.TranslationEngineId

/** The engine translation uses until the user picks one: the translator of the platform itself. */
class DeviceDefaultTranslationEngine(val engine: TranslationEngineId) {
    internal companion object {
        /** The default where the platform has no translator of its own, which no engine answers to. */
        val NONE = TranslationEngineId("translation-unavailable")
    }
}
