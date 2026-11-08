package app.katari.tts.api

import app.katari.tts.api.playback.TtsPlaybackStart
import app.katari.tts.api.preparation.ReadyTts
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.request.TtsRequest

interface TtsFeature {
    suspend fun prepare(request: TtsRequest): TtsPreparation

    suspend fun play(ready: ReadyTts): TtsPlaybackStart
}
