package eu.kanade.tachiyomi.ui.video.player.screen

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerScreenOrientationEffect
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import mihon.entry.interactions.source.EntryChildWebViewAction

/** The player activity's content: loading, the watch screen once a stream is ready, or the error that stopped it. */
@Composable
internal fun VideoPlayerScreen(
    activity: Activity,
    viewModel: VideoPlayerViewModel,
    host: VideoPlayerScreenHost,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        host.onHideSystemUi()
    }
    val resolvedOrientation by host.settings.orientation.state.collectAsState()
    VideoPlayerScreenOrientationEffect(
        activity = activity,
        orientation = host.visit.orientationOverride ?: resolvedOrientation.effectiveValue,
        videoAspectRatio = host.visit.videoAspectRatio,
        isInPictureInPictureMode = host.pictureInPicture.isInPictureInPictureMode,
    )
    when (val current = state) {
        is VideoPlayerViewModel.State.Loading -> {
            VideoPlayerLoadingOverlay(
                videoTitle = current.title,
                episodeName = current.episodeName,
                onBack = host.onClose,
                modifier = Modifier.fillMaxSize(),
            )
        }
        is VideoPlayerViewModel.State.Ready -> {
            VideoPlayerWatchScreen(
                current = current,
                viewModel = viewModel,
                host = host,
            )
        }
        is VideoPlayerViewModel.State.Error -> {
            VideoPlayerErrorOverlay(
                problem = current.problem,
                onBack = host.onClose,
                onRetry = viewModel::retry,
                onOpenWebView = current.webView?.let { webView ->
                    {
                        host.onOpenWebView(
                            EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                            webView,
                            current.title.orEmpty(),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
