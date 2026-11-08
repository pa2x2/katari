package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitles
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.player.VideoPlayer
import tachiyomi.domain.entry.model.PictureFraming

/**
 * The player's text and picture subtitles over the video, in the viewer's appearance, for a picture of [aspectRatio]
 * shown with [framing]. They move into the band under a fitted picture unless [keepSubtitlesInPicture], and stay
 * there while the controls come and go. While the subtitle editor is open, its own preview shows instead.
 */
@Composable
internal fun VideoPlayerSubtitles(
    player: VideoPlayer,
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    aspectRatio: Float?,
    framing: PictureFraming,
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
                aspectRatio = aspectRatio,
                framing = framing,
                appearance = appearance,
                keepInPicture = keepSubtitlesInPicture,
                gapPx = gapPx,
            )
        },
        modifier = modifier,
    )
}
