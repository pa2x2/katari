package mihon.tts.runtime.feature

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import mihon.language.api.identification.TextLanguageDetection
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineSelection
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.TtsEngineChoiceReason
import mihon.tts.api.preparation.TtsLanguageChoiceReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.preparation.TtsRejectionReason
import mihon.tts.api.preparation.TtsSystemSetupReason
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.preparation.TtsVoiceChoiceReason
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameterSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsProcessingPolicy
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.api.voice.TtsVoiceSelection
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.runtime.playback.RuntimeReadyTts
import mihon.tts.runtime.playback.TtsPlaybackCoordinator
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.request.segmentTtsText
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEnginePreparation
import mihon.tts.spi.engine.TtsEngineRegistry
import java.util.Locale

internal class DefaultTtsFeature(
    private val engineRegistry: TtsEngineRegistry,
    knownEngineCatalog: KnownTtsEngineCatalog,
    private val textLanguageDetectors: List<TextLanguageDetector>,
    private val preferences: ProfileTtsPreferences,
    private val selectedEngine: suspend () -> TtsEngineId?,
    scope: CoroutineScope,
    audioFocus: TtsAudioFocus,
) : TtsFeature {
    private val owner = Any()
    private val knownEngines: List<KnownTtsEngine> = knownEngineCatalog.knownEngines
    private val playbackCoordinator = TtsPlaybackCoordinator(
        scope = scope,
        audioFocus = audioFocus,
        mapPreparation = ::mapPreparation,
    )

    override suspend fun prepare(request: TtsRequest): TtsPreparation {
        if (request.text.isBlank()) return TtsPreparation.Rejected(TtsRejectionReason.BlankInput)
        val codePointCount = request.text.codePointCount(0, request.text.length)
        if (codePointCount > MAXIMUM_REQUEST_CODE_POINTS) {
            return TtsPreparation.Rejected(
                TtsRejectionReason.InputTooLarge(codePointCount, MAXIMUM_REQUEST_CODE_POINTS),
            )
        }

        val language = resolveLanguage(request)
            ?: return TtsPreparation.LanguageChoiceRequired(TtsLanguageChoiceReason.Undetermined)
        val engineId = when (val selection = request.engine) {
            TtsEngineSelection.ProfileDefault -> selectedEngine()
                ?: return TtsPreparation.EngineChoiceRequired(
                    TtsEngineChoiceReason.NoEngineConfigured,
                    knownEngines,
                )
            is TtsEngineSelection.Explicit -> selection.engine
        }
        val engine = engineRegistry.find(engineId)
            ?: return TtsPreparation.EngineChoiceRequired(
                TtsEngineChoiceReason.SelectedEngineUnavailable(engineId),
                knownEngines,
            )
        val voiceInspection = inspectVoices(engine)
        val voices = when (voiceInspection) {
            is TtsVoiceInspection.Available -> voiceInspection.voices
            is TtsVoiceInspection.VoiceDataRequired -> return TtsPreparation.SystemSetupRequired(
                engine = engineId,
                presentation = engine.presentation,
                reason = TtsSystemSetupReason.VoiceDataRequired,
            )
            is TtsVoiceInspection.Unavailable -> return TtsPreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(
                    engineId,
                    voiceInspection.reason ?: "TTS voices are unavailable",
                ),
            )
            is TtsVoiceInspection.Failed -> return TtsPreparation.Unavailable(
                TtsUnavailableReason.ProviderInspectionFailed(engineId, voiceInspection.reason),
            )
        }
        val compatibleVoices = voices.filter { it.supports(language) }.sortedWith(VOICE_ORDER)
        val voice = resolveVoice(request.voice, language, engine, voices, compatibleVoices)
            ?: return voiceChoiceRequired(request.voice, engineId, language, compatibleVoices)
        val networkAllowed = when (request.processingPolicy) {
            TtsProcessingPolicy.ProfileDefault -> preferences.allowNetworkVoices.get()
            TtsProcessingPolicy.OnDeviceOnly -> false
            TtsProcessingPolicy.NetworkAllowed -> true
        }
        if (voice.processing == TtsVoiceProcessing.NetworkRequired && !networkAllowed) {
            return TtsPreparation.Unavailable(TtsUnavailableReason.NetworkVoiceProhibited(voice.id))
        }
        val parameters = when (val selection = request.parameters) {
            TtsParameterSelection.ProfileDefault -> TtsParameters(
                speechRate = preferences.speechRate.get(),
                pitch = preferences.pitch.get(),
            )
            is TtsParameterSelection.Explicit -> selection.parameters
        }
        validateParameters(engine, parameters)?.let { return TtsPreparation.Rejected(it) }

        val resolved = ResolvedTtsRequest(
            text = request.text,
            language = language,
            engine = engineId,
            voice = voice,
            parameters = parameters,
            networkProcessingAllowed = networkAllowed,
        )
        val maximumSegmentCodePoints = when (val limit = engine.capabilities.inputLimit) {
            is TtsInputLimit.MaximumCodePoints -> limit.value
            TtsInputLimit.Unspecified -> MAXIMUM_REQUEST_CODE_POINTS
        }
        val segments = segmentTtsText(request.text, maximumSegmentCodePoints)
        val firstRequest = resolved.copy(text = segments.first().text)
        return try {
            when (val preparation = engine.prepare(firstRequest)) {
                is TtsEnginePreparation.Ready -> TtsPreparation.Ready(
                    speech = RuntimeReadyTts(
                        owner = owner,
                        engine = engine,
                        request = resolved,
                        segments = segments,
                        firstProviderRequest = preparation.request,
                    ),
                    request = resolved,
                    presentation = engine.presentation,
                )
                else -> mapPreparation(engine, preparation)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsPreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(engineId, "TTS engine preparation failed"),
            )
        }
    }

    override suspend fun play(ready: mihon.tts.api.preparation.ReadyTts): TtsPlaybackStart {
        val runtimeReady = ready as? RuntimeReadyTts
            ?: return TtsPlaybackStart.Failed(TtsPlaybackFailureReason.InvalidReadyTts)
        if (runtimeReady.owner !== owner || engineRegistry.find(runtimeReady.request.engine) !== runtimeReady.engine) {
            return TtsPlaybackStart.Failed(TtsPlaybackFailureReason.InvalidReadyTts)
        }
        return try {
            playbackCoordinator.play(runtimeReady)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsPlaybackStart.Failed(
                TtsPlaybackFailureReason.ProviderFailure(
                    runtimeReady.engine.catalogEntry.id,
                    "TTS playback could not start",
                ),
            )
        }
    }

    internal fun onAudioFocusLost() = playbackCoordinator.onAudioFocusLost()

    private suspend fun resolveLanguage(request: TtsRequest): LanguageTag? {
        return when (val selection = request.language) {
            is TtsLanguageSelection.Explicit -> selection.language
            TtsLanguageSelection.Automatic -> textLanguageDetectors.firstNotNullOfOrNull { detector ->
                when (val detection = detector.detect(request.text)) {
                    is TextLanguageDetection.Detected -> detection.language
                    TextLanguageDetection.Undetermined,
                    is TextLanguageDetection.Unavailable,
                    -> null
                }
            }
        }
    }

    private suspend fun inspectVoices(engine: TtsEngine): TtsVoiceInspection {
        return try {
            engine.inspectVoices()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsVoiceInspection.Failed(engine.catalogEntry.id, "TTS voices could not be inspected")
        }
    }

    private fun resolveVoice(
        selection: TtsVoiceSelection,
        language: LanguageTag,
        engine: TtsEngine,
        voices: List<TtsVoice>,
        compatibleVoices: List<TtsVoice>,
    ): TtsVoice? {
        return when (selection) {
            is TtsVoiceSelection.Explicit -> voices.singleOrNull { it.id == selection.voice }
                ?.takeIf { it.supports(language) && it.id.engine == engine.catalogEntry.id }
            TtsVoiceSelection.LanguageDefault -> {
                val preferred = preferences.voice(language).get()
                if (preferred != null) {
                    voices.singleOrNull { it.id == preferred }?.takeIf { it.supports(language) }
                } else {
                    compatibleVoices.firstOrNull()
                }
            }
        }
    }

    private fun voiceChoiceRequired(
        selection: TtsVoiceSelection,
        engine: TtsEngineId,
        language: LanguageTag,
        voices: List<TtsVoice>,
    ): TtsPreparation.VoiceChoiceRequired {
        val unavailable = when (selection) {
            is TtsVoiceSelection.Explicit -> selection.voice
            TtsVoiceSelection.LanguageDefault -> preferences.voice(language).get()
        }
        return TtsPreparation.VoiceChoiceRequired(
            engine = engine,
            language = language,
            reason = unavailable?.let(TtsVoiceChoiceReason::SelectedVoiceUnavailable)
                ?: TtsVoiceChoiceReason.NoCompatibleVoice,
            voices = voices,
        )
    }

    private fun validateParameters(engine: TtsEngine, parameters: TtsParameters): TtsRejectionReason? {
        val rateSupported = engine.capabilities.speechRate.supports(parameters.speechRate)
        if (!rateSupported) return TtsRejectionReason.UnsupportedSpeechRate(parameters.speechRate)
        val pitchSupported = engine.capabilities.pitch.supports(parameters.pitch)
        if (!pitchSupported) return TtsRejectionReason.UnsupportedPitch(parameters.pitch)
        return null
    }

    private fun mapPreparation(ready: RuntimeReadyTts, preparation: TtsEnginePreparation): TtsPreparation {
        return mapPreparation(ready.engine, preparation)
    }

    private fun mapPreparation(
        engine: TtsEngine,
        preparation: TtsEnginePreparation,
    ): TtsPreparation {
        return when (preparation) {
            is TtsEnginePreparation.Ready -> error("Ready TTS preparation must be handled by its caller")
            is TtsEnginePreparation.ProviderDisclosureRequired -> TtsPreparation.ProviderDisclosureRequired(
                engine = engine.catalogEntry.id,
                presentation = engine.presentation,
                disclosure = preparation.disclosure,
            )
            is TtsEnginePreparation.SystemSetupRequired -> TtsPreparation.SystemSetupRequired(
                engine = engine.catalogEntry.id,
                presentation = engine.presentation,
                reason = preparation.reason,
            )
            is TtsEnginePreparation.Unavailable -> TtsPreparation.Unavailable(preparation.reason)
        }
    }

    private fun TtsVoice.supports(language: LanguageTag): Boolean {
        if (this.language == language) return true
        return Locale.forLanguageTag(this.language.value).language == Locale.forLanguageTag(language.value).language
    }

    private fun TtsParameterSupport.supports(value: Float): Boolean {
        return when (this) {
            TtsParameterSupport.Unsupported -> value == 1f
            is TtsParameterSupport.Supported -> value in range.minimum..range.maximum
        }
    }

    private companion object {
        const val MAXIMUM_REQUEST_CODE_POINTS = 50_000
        val VOICE_ORDER = compareBy<TtsVoice>(
            { it.processing == TtsVoiceProcessing.NetworkRequired },
            { it.name.lowercase(Locale.ROOT) },
            { it.id.value },
        )
    }
}
