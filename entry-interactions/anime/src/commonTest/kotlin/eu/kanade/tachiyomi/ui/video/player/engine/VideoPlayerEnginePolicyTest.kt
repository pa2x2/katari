package eu.kanade.tachiyomi.ui.video.player.engine

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mihon.video.engine.spi.player.VideoPlayerError
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.PlayerEngineOverride

class VideoPlayerEnginePolicyTest {

    @Test
    fun `Auto never goes back to an engine that failed the stream`() {
        val builtIn = startEngineRun(override = null, mpv = MpvAvailability.Available)
        val mpv = signal(builtIn).shouldBeInstanceOf<MpvSignalOutcome.Switch>().run

        val backToBuiltIn = onEngineFailure(
            run = mpv,
            override = null,
            category = VideoPlayerError.Category.Unplayable,
            problem = unplayable("mpv"),
            mpv = MpvAvailability.Available,
        ).shouldBeInstanceOf<EngineFailureOutcome.Switch>().run
        backToBuiltIn.engine shouldBe VideoPlayerEngine.BuiltIn

        // The styled subtitles are still there, but mpv already failed.
        signal(backToBuiltIn) shouldBe MpvSignalOutcome.None
        onEngineFailure(
            run = backToBuiltIn,
            override = null,
            category = VideoPlayerError.Category.Unplayable,
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

    @Test
    fun `without an mpv add-on, nothing offers another engine or the add-on`() {
        // A title set to mpv on Android keeps the setting on desktop, whose one engine plays it.
        val run =
            startEngineRun(override = PlayerEngineOverride.MPV, mpv = MpvAvailability.Absent, sourceAsksMpv = true)
        run shouldBe VideoPlayerEngineRun(VideoPlayerEngine.BuiltIn)

        val problem = unplayable("built-in")
        onEngineFailure(
            run = run,
            override = null,
            category = VideoPlayerError.Category.Unplayable,
            problem = problem,
            mpv = MpvAvailability.Absent,
        ) shouldBe EngineFailureOutcome.Show(EngineProblem.Playback(problem))
        onMpvSignal(
            run = run,
            override = null,
            reason = EngineSwitchReason.StyledSubtitles,
            mpv = MpvAvailability.Absent,
            hintShown = false,
        ) shouldBe MpvSignalOutcome.None
        mpvTrackPick(run, override = null, mpv = MpvAvailability.Absent) shouldBe MpvTrackPick.Unavailable
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
