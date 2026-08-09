package mihon.tts.provider.android.connection

import android.app.Application
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEngineStopResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class AndroidTtsConnection(
    private val application: Application,
    private val enginePackage: String,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private val initializationMutex = Mutex()
    private val playbacks = ConcurrentHashMap<String, AndroidTtsPlayback>()
    private val utteranceIds = AtomicLong()

    @Volatile
    private var instance: TextToSpeech? = null

    suspend fun voices(): Set<Voice> = withInstance { it.voices.orEmpty() }

    suspend fun play(request: ResolvedTtsRequest): TtsEngineExecution = withInstance { tts ->
        if (request.text.length > TextToSpeech.getMaxSpeechInputLength()) {
            return@withInstance TtsEngineExecution.Failed("Android TTS input limit was exceeded")
        }
        val voice = tts.voices.orEmpty().singleOrNull { it.name == request.voice.id.value }
            ?: return@withInstance TtsEngineExecution.Failed("The selected Android TTS voice is unavailable")
        if (tts.setVoice(voice) != TextToSpeech.SUCCESS ||
            tts.setSpeechRate(request.parameters.speechRate) != TextToSpeech.SUCCESS ||
            tts.setPitch(request.parameters.pitch) != TextToSpeech.SUCCESS ||
            tts.setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES) != TextToSpeech.SUCCESS
        ) {
            return@withInstance TtsEngineExecution.Failed("Android TTS rejected the selected voice parameters")
        }

        val utteranceId = "$enginePackage:${utteranceIds.incrementAndGet()}"
        val playback = AndroidTtsPlayback(utteranceId, ::stop)
        playbacks[utteranceId] = playback
        val result = tts.speak(
            request.text,
            TextToSpeech.QUEUE_FLUSH,
            Bundle.EMPTY,
            utteranceId,
        )
        if (result != TextToSpeech.SUCCESS) {
            playbacks.remove(utteranceId)
            playback.failed("Android TTS rejected playback")
            TtsEngineExecution.Failed("Android TTS rejected playback")
        } else {
            TtsEngineExecution.Started(playback)
        }
    }

    suspend fun shutdown() {
        initializationMutex.withLock {
            withContext(mainDispatcher) {
                instance?.shutdown()
                instance = null
                playbacks.values.forEach(AndroidTtsPlayback::stopped)
                playbacks.clear()
            }
        }
    }

    private suspend fun stop(playback: AndroidTtsPlayback): TtsEngineStopResult {
        if (playbacks[playback.utteranceId] !== playback) return TtsEngineStopResult.AlreadyTerminal
        return try {
            val result = withContext(mainDispatcher) { instance?.stop() ?: TextToSpeech.ERROR }
            playbacks.remove(playback.utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                playback.stopped()
                TtsEngineStopResult.Stopped
            } else {
                TtsEngineStopResult.Failed("Android TTS could not stop playback")
            }
        } catch (_: RuntimeException) {
            TtsEngineStopResult.Failed("Android TTS could not stop playback")
        }
    }

    private suspend fun <T> withInstance(action: (TextToSpeech) -> T): T {
        val initialized = initializedInstance()
        return withContext(mainDispatcher) { action(initialized) }
    }

    private suspend fun initializedInstance(): TextToSpeech {
        instance?.let { return it }
        return initializationMutex.withLock {
            instance?.let { return@withLock it }
            val initialized = withTimeout(INITIALIZATION_TIMEOUT_MILLIS) {
                withContext(mainDispatcher) {
                    val status = CompletableDeferred<Int>()
                    val textToSpeech = TextToSpeech(
                        application,
                        { result -> status.complete(result) },
                        enginePackage,
                    )
                    var ready = false
                    try {
                        val result = status.await()
                        if (result != TextToSpeech.SUCCESS) {
                            throw AndroidTtsConnectionException("Android TTS initialization failed")
                        }
                        textToSpeech.setOnUtteranceProgressListener(ProgressListener())
                        ready = true
                        textToSpeech
                    } finally {
                        if (!ready) textToSpeech.shutdown()
                    }
                }
            }
            instance = initialized
            initialized
        }
    }

    private inner class ProgressListener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            playbacks[utteranceId]?.started()
        }

        override fun onDone(utteranceId: String) {
            playbacks.remove(utteranceId)?.completed()
        }

        @Deprecated("Required by the Android callback contract")
        override fun onError(utteranceId: String) {
            playbacks.remove(utteranceId)?.failed("Android TTS playback failed")
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            playbacks.remove(utteranceId)?.failed("Android TTS playback failed with code $errorCode")
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            playbacks.remove(utteranceId)?.stopped()
        }

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            playbacks[utteranceId]?.rangeStarted(start, end)
        }
    }

    private companion object {
        const val INITIALIZATION_TIMEOUT_MILLIS = 10_000L
        val SPEECH_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}

internal class AndroidTtsConnectionException(message: String) : RuntimeException(message)
