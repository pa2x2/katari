package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Previews where a mouse hovers over the timeline once it has rested on the bar for a moment, so a pointer passing
 * over the bar on its way to the buttons shows nothing; from then on the preview follows it until it leaves.
 */
@Composable
internal fun VideoPlayerTimelineHoverEffect(ui: VideoPlayerWatchUiState) {
    val onTimeline = ui.timelinePointerMs != null
    LaunchedEffect(onTimeline) {
        if (!onTimeline) {
            ui.timelineHoverMs = null
            return@LaunchedEffect
        }
        delay(HOVER_REST)
        snapshotFlow { ui.timelinePointerMs }.collect { ui.timelineHoverMs = it }
    }
}

private val HOVER_REST = 200.milliseconds
