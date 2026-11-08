package app.katari.tts.provider.speechdispatcher.engine

import app.katari.tts.provider.speechdispatcher.protocol.SsipEvent
import app.katari.tts.provider.speechdispatcher.protocol.SsipEventKind
import app.katari.tts.spi.engine.TtsEnginePlaybackEvent
import app.katari.tts.spi.engine.TtsEngineStopResult
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SpeechDispatcherMessagesTest {
    @Test
    fun `events that beat the queued reply reach the playback once it is added`() = runTest {
        val messages = SpeechDispatcherMessages()
        val playback = SpeechDispatcherPlayback(cancel = { TtsEngineStopResult.Stopped })

        messages.receive(SsipEvent(3, SsipEventKind.Begin))
        messages.receive(SsipEvent(3, SsipEventKind.End))
        messages.add(3, playback)

        playback.events.toList() shouldBe listOf(TtsEnginePlaybackEvent.Started, TtsEnginePlaybackEvent.Completed)
    }

    @Test
    fun `a late event for a finished message reaches no newer playback`() = runTest {
        val messages = SpeechDispatcherMessages()
        val stopped = SpeechDispatcherPlayback(cancel = { TtsEngineStopResult.Stopped })
        val next = SpeechDispatcherPlayback(cancel = { TtsEngineStopResult.Stopped })
        messages.add(3, stopped)
        messages.remove(stopped)

        messages.receive(SsipEvent(3, SsipEventKind.Canceled))
        messages.add(4, next)
        messages.receive(SsipEvent(4, SsipEventKind.End))

        next.events.toList() shouldBe listOf(TtsEnginePlaybackEvent.Completed)
    }
}
