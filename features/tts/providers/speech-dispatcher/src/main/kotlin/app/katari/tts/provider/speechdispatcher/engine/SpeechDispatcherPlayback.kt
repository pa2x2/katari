package app.katari.tts.provider.speechdispatcher.engine

import app.katari.tts.spi.engine.TtsEnginePlayback
import app.katari.tts.spi.engine.TtsEnginePlaybackEvent
import app.katari.tts.spi.engine.TtsEngineStopResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean

/** One spoken message, driven by Speech Dispatcher's events for it. */
internal class SpeechDispatcherPlayback(
    private val cancel: suspend (SpeechDispatcherPlayback) -> TtsEngineStopResult,
) : TtsEnginePlayback {
    private val channel = Channel<TtsEnginePlaybackEvent>(Channel.UNLIMITED)
    private val terminal = AtomicBoolean()

    override val events: Flow<TtsEnginePlaybackEvent> = channel.receiveAsFlow()

    val isTerminal: Boolean
        get() = terminal.get()

    override suspend fun stop(): TtsEngineStopResult {
        if (isTerminal) return TtsEngineStopResult.AlreadyTerminal
        return cancel(this)
    }

    fun started() {
        if (!isTerminal) channel.trySend(TtsEnginePlaybackEvent.Started)
    }

    fun completed() = finish(TtsEnginePlaybackEvent.Completed)

    fun stopped() = finish(TtsEnginePlaybackEvent.Stopped)

    fun failed(message: String) = finish(TtsEnginePlaybackEvent.Failed(message))

    private fun finish(event: TtsEnginePlaybackEvent) {
        if (terminal.compareAndSet(false, true)) {
            channel.trySend(event)
            channel.close()
        }
    }
}
