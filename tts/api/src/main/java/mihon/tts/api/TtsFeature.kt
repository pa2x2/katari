package mihon.tts.api

import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest

interface TtsFeature {
    suspend fun prepare(request: TtsRequest): TtsPreparation

    suspend fun play(ready: ReadyTts): TtsPlaybackStart
}
