package eu.kanade.tachiyomi.ui.video.player.screen

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.applySubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerTouchTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.setVideoPlayerTouchGestures
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.fillArgb
import mihon.presentation.kit.theme.KatariTheme

/**
 * The video: a Media3 player view without its own controller, with the subtitles in the viewer's appearance and the
 * player's touch gestures. While the subtitle editor is open the view shows the editor's draft, hidden behind the
 * editor's own preview.
 */
@Composable
@OptIn(markerClass = [UnstableApi::class])
internal fun VideoPlayerSurface(
    player: Player,
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    touchTarget: VideoPlayerTouchTarget,
    modifier: Modifier = Modifier,
) {
    val mediaArgb = SurfaceContainer.Media.fillArgb()
    val sideGestureEdgeExclusionPx = with(LocalDensity.current) { KatariTheme.sizes.touchTarget.toPx() }
    AndroidView(
        modifier = modifier,
        factory = { androidContext ->
            PlayerView(androidContext).apply {
                this.player = player
                setKeepContentOnPlayerReset(true)
                useController = false
                setEnableComposeSurfaceSyncWorkaround(true)
                setShutterBackgroundColor(mediaArgb)
                setBackgroundColor(mediaArgb)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isClickable = true
                isFocusable = true
            }
        },
        update = { playerView ->
            playerView.player = player
            playerView.setKeepContentOnPlayerReset(true)
            playerView.useController = false
            playerView.applySubtitleAppearance(
                appearance = if (ui.subtitleEditorVisible) {
                    ui.subtitleEditorDraft
                } else {
                    current.playback.subtitleAppearance
                },
                editorVisible = ui.subtitleEditorVisible,
            )
            playerView.setVideoPlayerTouchGestures(
                target = touchTarget,
                edgeExclusionPx = sideGestureEdgeExclusionPx,
            )
        },
    )
}
