package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.videoPlayerChapters
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The chapters of what [player] plays: the source's [markers], and the file's own chapters where the engine reads them.
 */
@Composable
internal fun rememberVideoPlayerChapters(
    player: VideoPlayer,
    markers: List<PlaybackMarker>,
    durationMs: Long,
): List<VideoPlayerChapter> {
    val fileChapters by remember(player) {
        player.state.map { it.fileChapters }.distinctUntilChanged()
    }.collectAsState(player.state.value.fileChapters)
    return remember(markers, fileChapters, durationMs) { videoPlayerChapters(markers, fileChapters, durationMs) }
}
