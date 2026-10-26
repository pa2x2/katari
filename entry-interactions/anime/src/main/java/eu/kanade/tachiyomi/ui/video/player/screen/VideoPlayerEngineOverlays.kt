package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.SourceChoiceAction
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerAddonProblemOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerBothEnginesFailedOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerHeadingText
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerNoticePill
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverrideFailedOverlay
import eu.kanade.tachiyomi.ui.video.player.components.settingsSection
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerEpisodeLine
import eu.kanade.tachiyomi.ui.video.player.engine.EngineProblem
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngine
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import eu.kanade.tachiyomi.ui.video.player.recovery.sourceChoice
import mihon.entry.interactions.source.EntryChildWebViewAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The problem screen for the current player's failure, with the engine's ways out where it has any. */
@Composable
internal fun VideoPlayerEngineProblem(
    problem: EngineProblem,
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    engine: WatchedPlayerEngine,
    modifier: Modifier = Modifier,
) {
    val retry = { viewModel.retryCurrentPlayback(ui.playbackSnapshot.positionMs) }
    val sourceChoice = current.playback.playbackData.sourceChoice()?.let { choice ->
        SourceChoiceAction(choice) { actions.openSettings(choice.settingsSection) }
    }
    val heading = VideoPlayerHeadingText(
        videoTitle = current.entryTitle,
        episodeName = videoPlayerEpisodeLine(current.memberTitle, current.chapterName),
    )
    when (problem) {
        is EngineProblem.Playback -> VideoPlayerErrorOverlay(
            problem = problem.problem,
            onBack = host.onClose,
            onRetry = retry,
            onOpenWebView = current.childWebView?.let { webView ->
                {
                    host.onOpenWebView(
                        EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                        webView,
                        current.entryTitle,
                    )
                }
            },
            sourceChoice = sourceChoice,
            heading = heading,
            modifier = modifier,
        )
        is EngineProblem.BothFailed -> VideoPlayerBothEnginesFailedOverlay(
            problem = problem,
            onBack = host.onClose,
            sourceChoice = sourceChoice,
            heading = heading,
            onRetry = retry,
            modifier = modifier,
        )
        is EngineProblem.OverrideFailed -> VideoPlayerOverrideFailedOverlay(
            problem = problem,
            onBack = host.onClose,
            onTryOther = { engine.tryOther(problem, ui.playbackSnapshot.positionMs) },
            sourceChoice = sourceChoice,
            heading = heading,
            onRetry = retry,
            modifier = modifier,
        )
        is EngineProblem.NeedsAddon -> VideoPlayerAddonProblemOverlay(
            problem = problem,
            addon = host.mpvAddon,
            installation = host.mpvAddonInstallation,
            onBack = host.onClose,
            sourceChoice = sourceChoice,
            heading = heading,
            onInstalled = { engine.addonInstalled(problem, ui.playbackSnapshot.positionMs) },
            modifier = modifier,
        )
    }
}

@Composable
internal fun VideoPlayerEngineNotice(
    notice: EngineNotice,
    engine: WatchedPlayerEngine,
    host: VideoPlayerScreenHost,
    positionMs: () -> Long,
    modifier: Modifier = Modifier,
) {
    val dismiss = { engine.notice = null }
    when (notice) {
        is EngineNotice.Hint -> VideoPlayerNoticePill(
            label = stringResource(checkNotNull(notice.reason.hint)),
            modifier = modifier,
            actionLabel = stringResource(MR.strings.anime_player_engine_get),
            onAction = {
                dismiss()
                host.onOpenAddonSettings()
            },
            onDismiss = dismiss,
        )
        is EngineNotice.TriedOnce -> VideoPlayerNoticePill(
            label = stringResource(
                when (notice.engine) {
                    VideoPlayerEngine.Mpv -> MR.strings.anime_player_engine_tried_mpv
                    VideoPlayerEngine.BuiltIn -> MR.strings.anime_player_engine_tried_built_in
                },
            ),
            modifier = modifier,
            actionLabel = stringResource(MR.strings.anime_player_engine_use_auto),
            onAction = { engine.choose(null, positionMs()) },
            onDismiss = dismiss,
        )
        is EngineNotice.PlayingInMpvFrom -> VideoPlayerNoticePill(
            label = stringResource(
                MR.strings.anime_player_engine_playing_mpv_from,
                formatPlaybackTimestamp(notice.positionMs),
            ),
            modifier = modifier,
        )
    }
}
