package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerProblem
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEpisodeChangeError
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerGestureGuide
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSpeedBoostPill
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSwitchingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.videoPlayerSafeContentPadding
import eu.kanade.tachiyomi.ui.video.player.videoPlaybackSpeedLabel
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/**
 * What the watch screen shows over the video about its state: loading until the first frame, the held speed boost,
 * episode and source switching, a failed episode change, a playback error and the first-run gesture guide.
 * [temporarySpeedBoost] is the boosted speed while the viewer holds it, otherwise null.
 */
@Composable
internal fun BoxScope.VideoPlayerStatusOverlays(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    playerProblem: VideoPlayerProblem?,
    temporarySpeedBoost: Float?,
    isInPictureInPictureMode: Boolean,
    endScreenVisible: Boolean,
    seekStepSeconds: Int,
) {
    val context = LocalContext.current
    if (ui.startupOverlayVisible && !isInPictureInPictureMode && !ui.subtitleEditorVisible) {
        VideoPlayerLoadingOverlay(
            videoTitle = current.entryTitle,
            episodeName = current.chapterName,
            onBack = host.onClose,
            modifier = Modifier.fillMaxSize(),
        )
    }

    val episodeChange = current.episodeChange
    val switchingLabel = when {
        episodeChange is VideoPlayerViewModel.EpisodeChange.Loading ->
            context.stringResource(MR.strings.anime_playback_loading_episode, episodeChange.episodeName)
        current.isSourceSwitching -> context.stringResource(MR.strings.anime_playback_switching_source)
        else -> null
    }
    if (temporarySpeedBoost != null && !isInPictureInPictureMode) {
        VideoPlayerSpeedBoostPill(
            speedLabel = videoPlaybackSpeedLabel(temporarySpeedBoost),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
    if (switchingLabel != null && !isInPictureInPictureMode && !ui.subtitleEditorVisible) {
        VideoPlayerSwitchingOverlay(
            label = switchingLabel,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (
        episodeChange is VideoPlayerViewModel.EpisodeChange.Failed &&
        !isInPictureInPictureMode &&
        !ui.subtitleEditorVisible
    ) {
        VideoPlayerEpisodeChangeError(
            episodeName = episodeChange.episodeName,
            message = context.stringResource(episodeChange.problem.cause.title),
            onDismiss = viewModel::dismissEpisodeChange,
            onOpenWebView = episodeChange.webView?.let { webView ->
                {
                    host.onOpenWebView(
                        EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                        webView,
                        current.entryTitle,
                    )
                }
            },
            onRetry = viewModel::retryEpisodeChange,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .videoPlayerSafeContentPadding(includeTop = true)
                .padding(
                    top = KatariTheme.sizes.touchTarget + KatariTheme.spacing.xl,
                    start = KatariTheme.spacing.l,
                    end = KatariTheme.spacing.l,
                ),
        )
    }

    val shownPlayerProblem = playerProblem
    if (shownPlayerProblem != null && !isInPictureInPictureMode && !ui.subtitleEditorVisible) {
        VideoPlayerErrorOverlay(
            problem = shownPlayerProblem,
            onBack = host.onClose,
            onRetry = {
                viewModel.retryCurrentPlayback(ui.playbackSnapshot.positionMs)
            },
            onOpenWebView = current.childWebView?.let { webView ->
                {
                    host.onOpenWebView(
                        EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                        webView,
                        current.entryTitle,
                    )
                }
            },
            onChangeSource = { actions.openSettings(null) },
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (
        host.visit.gestureGuideVisible &&
        !ui.startupOverlayVisible &&
        !endScreenVisible &&
        !isInPictureInPictureMode &&
        !ui.subtitleEditorVisible
    ) {
        VideoPlayerGestureGuide(
            seekStepSeconds = seekStepSeconds,
            onDismiss = {
                host.visit.gestureGuideVisible = false
                host.preferences.gestureGuideShown.set(true)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
