package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.LocalVideoPlayerClipboard

/** The player's content: loading, the watch screen once a stream is ready, or the error that stopped it. */
@Composable
internal fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel,
    host: VideoPlayerScreenHost,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        host.onHideSystemUi()
    }
    val resolvedOrientation by host.settings.orientation.state.collectAsState()
    val orientation = host.visit.orientationOverride ?: resolvedOrientation.effectiveValue
    val videoAspectRatio = host.visit.videoAspectRatio
    val isInPictureInPictureMode = host.pictureInPicture.isInPictureInPictureMode
    LaunchedEffect(orientation, videoAspectRatio, isInPictureInPictureMode) {
        // Picture-in-picture windows take the video's shape on their own.
        if (!isInPictureInPictureMode) host.orientation?.request(orientation, videoAspectRatio)
    }
    CompositionLocalProvider(LocalVideoPlayerClipboard provides host.clipboard) {
        when (val current = state) {
            is VideoPlayerViewModel.State.Loading -> VideoPlayerStartupLoading(
                current = current,
                viewModel = viewModel,
                host = host,
            )
            is VideoPlayerViewModel.State.Ready -> {
                VideoPlayerWatchScreen(
                    current = current,
                    viewModel = viewModel,
                    host = host,
                )
            }
            is VideoPlayerViewModel.State.Error -> VideoPlayerStartupProblem(
                current = current,
                viewModel = viewModel,
                host = host,
            )
        }
    }
}
