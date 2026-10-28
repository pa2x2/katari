package mihon.desktop.tts

import dev.zacsweers.metro.Inject
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.availability.TtsDeviceAvailability
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsHostActions
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

/**
 * Speech settings with no engine to set up: speech is not offered, so the speech settings are hidden, and nothing can
 * be chosen, so they keep nothing. Stands in until desktop has a speech engine.
 */
@Inject
class NoTtsHostActions : TtsHostActions {
    override val isOffered: Boolean = false
    override val knownEngines: List<KnownTtsEngine> = emptyList()
    override val selectedEngine: Preference<TtsEngineId> = InMemoryPreference("tts_engine", null, NO_ENGINE)
    override val speechRate: Preference<Float> = InMemoryPreference("tts_speech_rate", null, 1f)
    override val pitch: Preference<Float> = InMemoryPreference("tts_pitch", null, 1f)
    override val allowNetworkVoices: Preference<Boolean> = InMemoryPreference("tts_allow_network_voices", null, false)

    override suspend fun deviceAvailability(): TtsDeviceAvailability = TtsDeviceAvailability.NoEnginesInstalled

    override suspend fun inspectEngines(): TtsEngineInspection =
        TtsEngineInspection(engines = emptyList(), selectedEngine = null)

    override suspend fun inspectVoices(engine: TtsEngineId): TtsVoiceInspection =
        TtsVoiceInspection.Unavailable(engine)

    override suspend fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
    ): TtsHostActionResult = TtsHostActionResult.SetupUnsupported

    override fun supportsSetup(engine: TtsEngineId): Boolean = false

    override suspend fun openSetup(engine: TtsEngineId): TtsHostActionResult = TtsHostActionResult.SetupUnsupported

    override suspend fun installVoiceData(engine: TtsEngineId, languages: Set<LanguageTag>): TtsHostActionResult =
        TtsHostActionResult.SetupUnsupported

    override fun selectedVoice(language: LanguageTag): TtsVoiceId? = null

    override fun selectedVoiceOverrides(): Map<LanguageTag, TtsVoiceId> = emptyMap()

    override fun setSelectedEngine(engine: TtsEngineId) = Unit

    override fun setSelectedVoice(language: LanguageTag, voice: TtsVoiceId?) = Unit

    private companion object {
        val NO_ENGINE = TtsEngineId("tts-unavailable")
    }
}
