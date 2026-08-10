package mihon.tts.ui.settings

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.playback.TtsStopReason
import mihon.tts.api.playback.TtsStopResult
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPreviewPlaybackControllerTest {

    @Test
    fun `late stop from replaced preview cannot detach the current session`() = runTest {
        val first = BlockingStopSession()
        val second = BlockingStopSession()
        val feature = QueuedSessionTtsFeature(listOf(first, second))
        var state: TtsPreviewState = TtsPreviewState.Idle
        val controller = TtsPreviewPlaybackController(
            feature = feature,
            scope = backgroundScope,
            currentState = { state },
            updateState = { state = it },
        )

        controller.toggle(ENGLISH)
        runCurrent()
        controller.toggle(ENGLISH)
        runCurrent()
        controller.toggle(ENGLISH)
        runCurrent()

        first.releaseStop.complete(Unit)
        runCurrent()
        controller.toggle(ENGLISH)
        runCurrent()

        second.stopCount shouldBe 1
        second.releaseStop.complete(Unit)
        runCurrent()
        controller.close()
    }

    private class QueuedSessionTtsFeature(
        sessions: List<TtsPlaybackSession>,
    ) : TtsFeature {
        private val preparationFeature = TestTtsFeature()
        private val remainingSessions = ArrayDeque(sessions)

        override suspend fun prepare(request: TtsRequest): TtsPreparation = preparationFeature.prepare(request)

        override suspend fun play(ready: ReadyTts): TtsPlaybackStart {
            return TtsPlaybackStart.Started(remainingSessions.removeFirst())
        }
    }

    private class BlockingStopSession : TtsPlaybackSession {
        private val mutableState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Speaking())
        override val state: StateFlow<TtsPlaybackState> = mutableState
        val releaseStop = CompletableDeferred<Unit>()
        var stopCount = 0
            private set

        override suspend fun stop(): TtsStopResult {
            stopCount += 1
            releaseStop.await()
            mutableState.value = TtsPlaybackState.Stopped(TtsStopReason.Requested)
            return TtsStopResult.Stopped
        }
    }
}
