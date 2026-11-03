package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.videoPlayerChapters
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.video.engine.spi.chapters.FileChapters

/**
 * The chapters of what [player] plays: the source's [markers], and the file's own chapters where the engine reads them.
 */
@Composable
internal fun rememberVideoPlayerChapters(
    player: Player,
    markers: List<PlaybackMarker>,
    durationMs: Long,
): List<VideoPlayerChapter> {
    val fileChapters by remember(player) {
        (player as? FileChapters)?.fileChapters ?: MutableStateFlow(emptyList())
    }.collectAsState()
    return remember(markers, fileChapters, durationMs) { videoPlayerChapters(markers, fileChapters, durationMs) }
}
