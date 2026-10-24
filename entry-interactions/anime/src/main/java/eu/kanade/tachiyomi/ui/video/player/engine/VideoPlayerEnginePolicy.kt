package eu.kanade.tachiyomi.ui.video.player.engine

import androidx.media3.common.PlaybackException
import eu.kanade.tachiyomi.ui.video.player.PARSING_AND_DECODING_ERROR_CODES
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import tachiyomi.domain.entry.model.PlayerEngineOverride

/*
 * How Auto picks the engine. Every stream starts on the built-in player unless the title says otherwise, or the source
 * gives the stream mpv options and mpv is available. Auto moves to mpv when the built-in player fails to parse or decode
 * the stream, or when it plays without something mpv could show.
 * A parsing or decoding error in mpv sends Auto back to the built-in player, unless that one failed already: neither
 * engine is ever tried twice for a stream, so the two can't take turns failing.
 */

internal fun startEngineRun(
    override: PlayerEngineOverride?,
    mpv: MpvAvailability,
    sourceAsksMpv: Boolean = false,
): VideoPlayerEngineRun {
    val engine = override?.engine?.takeIf { it != VideoPlayerEngine.Mpv || mpv == MpvAvailability.Available }
    return when {
        engine != null -> VideoPlayerEngineRun(engine)
        override == null && sourceAsksMpv && mpv == MpvAvailability.Available ->
            VideoPlayerEngineRun(VideoPlayerEngine.Mpv, EngineSwitchReason.SourceAsks)
        else -> VideoPlayerEngineRun(VideoPlayerEngine.BuiltIn)
    }
}

internal sealed interface EngineFailureOutcome {
    /** Plays on in another engine, from where the failed one stopped. */
    data class Switch(val run: VideoPlayerEngineRun) : EngineFailureOutcome

    data class Show(val problem: EngineProblem) : EngineFailureOutcome
}

/** What the problem screen shows when playback can't go on by itself. */
internal sealed interface EngineProblem {
    /** Not an engine's doing, or there is nothing to try instead: the problem screen as for any other error. */
    data class Playback(val problem: VideoPlayerProblem) : EngineProblem

    data class BothFailed(val failures: List<EngineFailure>) : EngineProblem

    /** The engine the title's override picks failed; [other] might play the stream. */
    data class OverrideFailed(val failures: List<EngineFailure>, val other: VideoPlayerEngine) : EngineProblem

    /** The built-in player failed and Auto would use mpv, but its add-on has to be installed or updated first. */
    data class NeedsAddon(val failures: List<EngineFailure>, val update: Boolean) : EngineProblem
}

/** [errorCode] is the player's [PlaybackException.errorCode], and [problem] what it maps to. */
internal fun onEngineFailure(
    run: VideoPlayerEngineRun,
    override: PlayerEngineOverride?,
    errorCode: Int,
    problem: VideoPlayerProblem,
    mpv: MpvAvailability,
): EngineFailureOutcome {
    if (errorCode !in PARSING_AND_DECODING_ERROR_CODES) {
        return EngineFailureOutcome.Show(EngineProblem.Playback(problem))
    }
    val failures = run.failures + EngineFailure(run.engine, problem.details)
    val other = run.engine.other
    val otherPlayable = other == VideoPlayerEngine.BuiltIn || mpv == MpvAvailability.Available
    val shown = when {
        run.failed(other) -> EngineProblem.BothFailed(failures)
        // The override's engine failed; one that couldn't be used for it falls through to Auto.
        override?.engine == run.engine && !run.tryOnce -> if (otherPlayable) {
            EngineProblem.OverrideFailed(failures, other)
        } else {
            EngineProblem.Playback(problem)
        }
        otherPlayable -> return EngineFailureOutcome.Switch(
            VideoPlayerEngineRun(
                engine = other,
                reason = when (other) {
                    VideoPlayerEngine.Mpv -> EngineSwitchReason.BuiltInFailed
                    VideoPlayerEngine.BuiltIn -> EngineSwitchReason.MpvFailed
                },
                failures = failures,
            ),
        )
        mpv.installable -> EngineProblem.NeedsAddon(failures, update = mpv == MpvAvailability.NeedsUpdate)
        else -> EngineProblem.Playback(problem)
    }
    return EngineFailureOutcome.Show(shown)
}

internal sealed interface MpvSignalOutcome {
    data class Switch(val run: VideoPlayerEngineRun) : MpvSignalOutcome

    /** mpv would be better, but its add-on is missing: say so once per title. */
    data class Hint(val reason: EngineSwitchReason) : MpvSignalOutcome

    data object None : MpvSignalOutcome
}

/** The built-in player plays, but without something mpv could show, for the [reason] given. */
internal fun onMpvSignal(
    run: VideoPlayerEngineRun,
    override: PlayerEngineOverride?,
    reason: EngineSwitchReason,
    mpv: MpvAvailability,
    hintShown: Boolean,
): MpvSignalOutcome = when {
    run.engine == VideoPlayerEngine.Mpv || override != null || run.tryOnce || run.failed(VideoPlayerEngine.Mpv) ->
        MpvSignalOutcome.None
    mpv == MpvAvailability.Available -> MpvSignalOutcome.Switch(
        VideoPlayerEngineRun(VideoPlayerEngine.Mpv, reason, run.failures),
    )
    mpv.installable && !hintShown && reason.hint != null -> MpvSignalOutcome.Hint(reason)
    else -> MpvSignalOutcome.None
}
