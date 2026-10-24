package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Tracks
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.engine.EngineFailureOutcome
import eu.kanade.tachiyomi.ui.video.player.engine.EngineProblem
import eu.kanade.tachiyomi.ui.video.player.engine.EngineSwitchReason
import eu.kanade.tachiyomi.ui.video.player.engine.MpvAvailability
import eu.kanade.tachiyomi.ui.video.player.engine.MpvSignalOutcome
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngineRun
import eu.kanade.tachiyomi.ui.video.player.engine.availability
import eu.kanade.tachiyomi.ui.video.player.engine.engine
import eu.kanade.tachiyomi.ui.video.player.engine.mpvSignal
import eu.kanade.tachiyomi.ui.video.player.engine.onEngineFailure
import eu.kanade.tachiyomi.ui.video.player.engine.onMpvSignal
import eu.kanade.tachiyomi.ui.video.player.engine.startEngineRun
import eu.kanade.tachiyomi.ui.video.player.server.allowsServerFallback
import eu.kanade.tachiyomi.ui.video.player.toProblem
import kotlinx.coroutines.delay
import tachiyomi.domain.entry.model.PlayerEngineOverride

/** What the player says over the video about its engine; each fades on its own. */
internal sealed interface EngineNotice {
    val durationMs: Long

    /** mpv would play the title better, but its add-on is missing. */
    data class Hint(val reason: EngineSwitchReason) : EngineNotice {
        override val durationMs: Long get() = LONG_NOTICE_MS
    }

    /** [engine] plays this episode only; the notice offers Auto for the title. */
    data class TriedOnce(val engine: VideoPlayerEngine) : EngineNotice {
        override val durationMs: Long get() = LONG_NOTICE_MS
    }

    data class PlayingInMpvFrom(val positionMs: Long) : EngineNotice {
        override val durationMs: Long get() = SHORT_NOTICE_MS
    }
}

/**
 * The watched player's engine for the screen: the run the current player plays in, and what follows when it fails or
 * plays without something mpv could show. A switch goes through the view model, which starts a new player at the same
 * position; [problem] is the current player's, and the screen clears it with each new player.
 */
@Stable
internal class WatchedPlayerEngine(private val viewModel: VideoPlayerViewModel) {
    var run by mutableStateOf(VideoPlayerEngineRun(VideoPlayerEngine.BuiltIn))
        private set
    var mpv by mutableStateOf(MpvAvailability.NotInstalled)
        private set
    private var override by mutableStateOf<PlayerEngineOverride?>(null)
    private var hintShown by mutableStateOf(false)
    private var sourceAsksMpv by mutableStateOf(false)

    var problem by mutableStateOf<EngineProblem?>(null)
    var notice by mutableStateOf<EngineNotice?>(null)
    private var pendingHint by mutableStateOf<EngineSwitchReason?>(null)

    /** [beforeFirstFrame]: the player failed before showing anything, so another server may still play the episode. */
    fun onError(error: PlaybackException, positionMs: Long, beforeFirstFrame: Boolean) {
        val outcome = onEngineFailure(run, override, error.errorCode, error.toProblem(), mpv)
        if (beforeFirstFrame && outcome.allowsServerFallback(error.errorCode) && viewModel.tryNextServer(positionMs)) {
            return
        }
        when (outcome) {
            is EngineFailureOutcome.Switch -> viewModel.switchEngine(outcome.run, positionMs)
            is EngineFailureOutcome.Show -> problem = outcome.problem
        }
    }

    fun onTracks(tracks: Tracks, positionMs: Long) {
        if (run.engine != VideoPlayerEngine.BuiltIn || problem != null) return
        val reason = EngineSwitchReason.SourceAsks.takeIf { sourceAsksMpv } ?: tracks.mpvSignal() ?: return
        when (val outcome = onMpvSignal(run, override, reason, mpv, hintShown)) {
            is MpvSignalOutcome.Switch -> viewModel.switchEngine(outcome.run, positionMs)
            is MpvSignalOutcome.Hint -> if (pendingHint == null && notice == null) pendingHint = outcome.reason
            MpvSignalOutcome.None -> Unit
        }
    }

    /** The viewer's pick in the Player row: an engine for the title, or null for Auto, which keeps what plays. */
    fun choose(choice: PlayerEngineOverride?, positionMs: Long) {
        val current = run
        val next = when {
            choice == null -> current.copy(reason = current.autoReason(), tryOnce = false)
            choice.engine == current.engine -> current.copy(reason = null, tryOnce = false)
            else -> startEngineRun(choice, mpv).copy(failures = current.failures)
        }
        viewModel.chooseEngine(choice, next, restart = next.engine != current.engine, positionMs)
        if (notice is EngineNotice.TriedOnce) notice = null
    }

    /** "Try mpv" or "Try built-in" after the override's engine failed: this episode only. */
    fun tryOther(problem: EngineProblem.OverrideFailed, positionMs: Long) {
        viewModel.switchEngine(
            VideoPlayerEngineRun(problem.other, failures = problem.failures, tryOnce = true),
            positionMs,
        )
        notice = EngineNotice.TriedOnce(problem.other)
    }

    fun addonInstalled(problem: EngineProblem.NeedsAddon, positionMs: Long) {
        viewModel.switchEngine(
            VideoPlayerEngineRun(VideoPlayerEngine.Mpv, EngineSwitchReason.BuiltInFailed, problem.failures),
            positionMs,
        )
        notice = EngineNotice.PlayingInMpvFrom(positionMs)
    }

    internal fun update(
        run: VideoPlayerEngineRun,
        mpv: MpvAvailability,
        override: PlayerEngineOverride?,
        hintShown: Boolean,
        sourceAsksMpv: Boolean,
    ) {
        this.run = run
        this.mpv = mpv
        this.override = override
        this.hintShown = hintShown
        this.sourceAsksMpv = sourceAsksMpv
    }

    /** Shows a pending hint a few seconds in, marks the title as hinted, and fades each notice. */
    @Composable
    fun NoticeEffects() {
        LaunchedEffect(pendingHint) {
            val reason = pendingHint ?: return@LaunchedEffect
            delay(HINT_DELAY_MS)
            pendingHint = null
            notice = EngineNotice.Hint(reason)
            // Shown is enough: dismissing or ignoring it both mean it doesn't come back for this title.
            viewModel.markMpvHintShown()
        }
        LaunchedEffect(notice) {
            val shown = notice ?: return@LaunchedEffect
            delay(shown.durationMs)
            if (notice == shown) notice = null
        }
    }
}

/**
 * The engine state for [current]'s episode. A player that has no run of its own yet starts with the title's choice as
 * the add-on was when the player was made, keyed like the player itself.
 */
@Composable
internal fun rememberWatchedPlayerEngine(
    current: VideoPlayerViewModel.State.Ready,
    subtitlePayloadKey: String,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
): WatchedPlayerEngine {
    val addonState by host.mpvAddon.state.collectAsState()
    val mpv = host.mpvAddon.availability(addonState)
    val override = current.playback.engineOverride
    val sourceAsksMpv = current.stream.mpvOptions.isNotEmpty()
    val startRun = remember(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
        startEngineRun(override, mpv, sourceAsksMpv)
    }
    val engine = remember(current.chapterId) { WatchedPlayerEngine(viewModel) }
    // Like rememberUpdatedState: the player's listener reads these after composition.
    engine.update(
        run = current.engineRun ?: startRun,
        mpv = mpv,
        override = override,
        hintShown = current.playback.mpvHintShown,
        sourceAsksMpv = sourceAsksMpv,
    )
    return engine
}

private fun VideoPlayerEngineRun.autoReason(): EngineSwitchReason? = reason ?: when {
    engine == VideoPlayerEngine.Mpv && failed(VideoPlayerEngine.BuiltIn) -> EngineSwitchReason.BuiltInFailed
    engine == VideoPlayerEngine.BuiltIn && failed(VideoPlayerEngine.Mpv) -> EngineSwitchReason.MpvFailed
    else -> null
}

private const val HINT_DELAY_MS = 3_000L
private const val LONG_NOTICE_MS = 8_000L
private const val SHORT_NOTICE_MS = 4_000L
