package eu.kanade.tachiyomi.ui.video.player.screen

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.applySubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerPinchGesture
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerTouchTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.setVideoPlayerTouchGestures
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPicturePlacer
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.fillArgb
import mihon.presentation.kit.theme.KatariTheme

/**
 * The video: a Media3 player view without its own controller, with the subtitles in the viewer's appearance, the
 * picture framed as [touchTarget] shows it, and the player's touch gestures. The subtitles move into the band under a
 * fitted picture unless [keepSubtitlesInPicture], and rise above the controls' bottom row while it shows. While the
 * subtitle editor is open the view shows the editor's draft, hidden behind the editor's own preview.
 *
 * [player] is what the view controls, and [enginePlayer] the engine's player inside it. The framing follows
 * [touchTarget] outside composition, so a pinch's every move reaches the view without composing it again.
 */
@Composable
@OptIn(markerClass = [UnstableApi::class])
internal fun VideoPlayerSurface(
    player: Player,
    enginePlayer: Player,
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    touchTarget: VideoPlayerTouchTarget,
    keepSubtitlesInPicture: Boolean,
    modifier: Modifier = Modifier,
) {
    val mediaArgb = SurfaceContainer.Media.fillArgb()
    val sideGestureEdgeExclusionPx = with(LocalDensity.current) { KatariTheme.sizes.touchTarget.toPx() }
    val subtitleGapPx = with(LocalDensity.current) { KatariTheme.spacing.s.toPx() }
    val placer = remember { VideoPicturePlacer() }
    val pinchGesture = remember(touchTarget) { VideoPlayerPinchGesture(touchTarget) }
    LaunchedEffect(placer, touchTarget, enginePlayer) {
        snapshotFlow { touchTarget.pictureFraming to touchTarget.videoAspectRatio }
            .collect { (framing, aspectRatio) -> placer.show(framing, aspectRatio, enginePlayer) }
    }
    AndroidView(
        modifier = modifier,
        factory = { androidContext ->
            PlayerView(androidContext).apply {
                placer.attach(this)
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
            val appearance = if (ui.subtitleEditorVisible) {
                ui.subtitleEditorDraft
            } else {
                current.playback.subtitleAppearance
            }
            playerView.applySubtitleAppearance(appearance = appearance, editorVisible = ui.subtitleEditorVisible)
            placer.showSubtitles(
                appearance = appearance,
                keepInPicture = if (ui.subtitleEditorVisible) {
                    ui.subtitleEditorKeepsInPicture
                } else {
                    keepSubtitlesInPicture
                },
                controls = ui.controlsFootprint,
                gapPx = subtitleGapPx,
            )
            playerView.setVideoPlayerTouchGestures(
                target = touchTarget,
                pinchGesture = pinchGesture,
                edgeExclusionPx = sideGestureEdgeExclusionPx,
            )
        },
    )
}
