package app.katari.tts.runtime.playback

import app.katari.tts.api.preparation.ReadyTts
import app.katari.tts.api.request.ResolvedTtsRequest
import app.katari.tts.runtime.request.TtsTextSegment
import app.katari.tts.spi.engine.ReadyTtsEngineRequest
import app.katari.tts.spi.engine.TtsEngine

internal class RuntimeReadyTts(
    val owner: Any,
    val engine: TtsEngine,
    val request: ResolvedTtsRequest,
    val segments: List<TtsTextSegment>,
    val firstProviderRequest: ReadyTtsEngineRequest,
) : ReadyTts
