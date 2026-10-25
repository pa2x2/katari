package eu.kanade.tachiyomi.ui.video.player.engine

import androidx.media3.common.PlaybackException
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class VideoPlayerEnginePolicyTest {

    @Test
    fun `Auto never goes back to an engine that failed the stream`() {
        val builtIn = startEngineRun(override = null, mpv = MpvAvailability.Available)
        val mpv = signal(builtIn).shouldBeInstanceOf<MpvSignalOutcome.Switch>().run

        val backToBuiltIn = onEngineFailure(
            run = mpv,
            override = null,
            errorCode = PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            problem = unplayable("mpv"),
            mpv = MpvAvailability.Available,
        ).shouldBeInstanceOf<EngineFailureOutcome.Switch>().run
        backToBuiltIn.engine shouldBe VideoPlayerEngine.BuiltIn

        // The styled subtitles are still there, but mpv already failed.
        signal(backToBuiltIn) shouldBe MpvSignalOutcome.None
        onEngineFailure(
            run = backToBuiltIn,
            override = null,
            errorCode = PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            problem = unplayable("built-in"),
            mpv = MpvAvailability.Available,
        ) shouldBe EngineFailureOutcome.Show(
            EngineProblem.BothFailed(
                listOf(
                    EngineFailure(VideoPlayerEngine.Mpv, "mpv"),
                    EngineFailure(VideoPlayerEngine.BuiltIn, "built-in"),
                ),
            ),
        )
    }

    private fun signal(run: VideoPlayerEngineRun) = onMpvSignal(
        run = run,
        override = null,
        reason = EngineSwitchReason.StyledSubtitles,
        mpv = MpvAvailability.Available,
        hintShown = false,
    )

    private fun unplayable(details: String) = VideoPlayerProblem(VideoPlayerProblem.Cause.Unplayable, details)
}
