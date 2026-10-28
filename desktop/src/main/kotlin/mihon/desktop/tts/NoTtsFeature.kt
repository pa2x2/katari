package mihon.desktop.tts

import dev.zacsweers.metro.Inject
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsEngineChoiceReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest

/**
 * Speech with no engine to speak: every request asks for an engine and offers none, as Android does on a device with
 * no speech engine installed. Stands in until desktop has a speech engine.
 */
@Inject
class NoTtsFeature : TtsFeature {
    override suspend fun prepare(request: TtsRequest): TtsPreparation =
        TtsPreparation.EngineChoiceRequired(TtsEngineChoiceReason.NoEngineConfigured, engines = emptyList())

    override suspend fun play(ready: ReadyTts): TtsPlaybackStart =
        TtsPlaybackStart.Failed(TtsPlaybackFailureReason.InvalidReadyTts)
}
