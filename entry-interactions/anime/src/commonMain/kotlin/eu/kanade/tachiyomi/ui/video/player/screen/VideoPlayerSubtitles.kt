package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerInputTarget
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitles
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The player's text and picture subtitles over the video, in the viewer's appearance, for the picture framed as
 * [inputTarget] shows it. They move into the band under a fitted picture unless [keepSubtitlesInPicture], and rise
 * above the controls' bottom row while it shows. While the subtitle editor is open, its own preview shows instead.
 */
@Composable
internal fun VideoPlayerSubtitles(
    player: VideoPlayer,
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    inputTarget: VideoPlayerInputTarget,
    keepSubtitlesInPicture: Boolean,
    modifier: Modifier = Modifier,
) {
    if (ui.subtitleEditorVisible) return
    val cuesFlow = remember(player) { player.state.map { it.cues }.distinctUntilChanged() }
    val cues by cuesFlow.collectAsState(initial = player.state.value.cues)
    val gapPx = with(LocalDensity.current) { KatariTheme.spacing.s.toPx() }
    val appearance = current.playback.subtitleAppearance
    VideoSubtitles(
        cues = cues,
        appearance = appearance,
        layout = { width, height ->
            videoSubtitleLayout(
                playerWidth = width,
                playerHeight = height,
                aspectRatio = inputTarget.videoAspectRatio,
                framing = inputTarget.pictureFraming,
                appearance = appearance,
                keepInPicture = keepSubtitlesInPicture,
                controls = ui.controlsFootprint,
                gapPx = gapPx,
            )
        },
        modifier = modifier,
    )
}
