package mihon.tts.runtime.playback

import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.runtime.request.TtsTextSegment
import mihon.tts.spi.engine.ReadyTtsEngineRequest
import mihon.tts.spi.engine.TtsEngine

internal class RuntimeReadyTts(
    val owner: Any,
    val engine: TtsEngine,
    val request: ResolvedTtsRequest,
    val segments: List<TtsTextSegment>,
    val firstProviderRequest: ReadyTtsEngineRequest,
) : ReadyTts
