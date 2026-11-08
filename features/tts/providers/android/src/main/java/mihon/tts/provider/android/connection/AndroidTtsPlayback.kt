package mihon.tts.provider.android.connection

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import mihon.tts.api.playback.TtsTextRange
import mihon.tts.spi.engine.TtsEnginePlayback
import mihon.tts.spi.engine.TtsEnginePlaybackEvent
import mihon.tts.spi.engine.TtsEngineStopResult
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidTtsPlayback(
    val utteranceId: String,
    private val stopProvider: suspend (AndroidTtsPlayback) -> TtsEngineStopResult,
) : TtsEnginePlayback {
    private val channel = Channel<TtsEnginePlaybackEvent>(Channel.UNLIMITED)
    private val terminal = AtomicBoolean()

    override val events: Flow<TtsEnginePlaybackEvent> = channel.receiveAsFlow()

    override suspend fun stop(): TtsEngineStopResult = stopProvider(this)

    fun started() {
        if (!terminal.get()) channel.trySend(TtsEnginePlaybackEvent.Started)
    }

    fun rangeStarted(start: Int, end: Int) {
        if (!terminal.get() && start >= 0 && end > start) {
            channel.trySend(TtsEnginePlaybackEvent.RangeStarted(TtsTextRange(start, end)))
        }
    }

    fun completed() = finish(TtsEnginePlaybackEvent.Completed)

    fun stopped() = finish(TtsEnginePlaybackEvent.Stopped)

    fun failed(message: String?) = finish(TtsEnginePlaybackEvent.Failed(message))

    private fun finish(event: TtsEnginePlaybackEvent) {
        if (terminal.compareAndSet(false, true)) {
            channel.trySend(event)
            channel.close()
        }
    }
}
