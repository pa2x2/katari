package app.katari.tts.ui.settings

import app.katari.tts.api.playback.TtsPlaybackSession
import app.katari.tts.api.playback.TtsPlaybackState
import app.katari.tts.api.playback.TtsStopReason
import app.katari.tts.api.playback.TtsStopResult
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPreviewPlaybackControllerTest {

    @Test
    fun `late stop from replaced preview cannot detach the current session`() = runTest {
        val first = BlockingStopSession()
        val second = BlockingStopSession()
        val feature = TestTtsFeature(listOf(first, second))
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
