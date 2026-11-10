package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tells a click on a video picture from the first click of a double-click. A click acts only once [timeoutMs] has
 * passed without a second press within [slop] of it, so a double-click never plays or pauses on its way to full screen.
 * The wait runs in [scope], which ends it with the pointer input.
 */
internal class PictureClicks(private val scope: CoroutineScope, private val timeoutMs: Long, private val slop: Float) {
    private var waiting: Job? = null
    private var at = Offset.Zero

    /** A click let go at [position]: [onClick] runs unless a press makes it a double-click first. */
    fun clicked(position: Offset, onClick: () -> Unit) {
        at = position
        waiting = scope.launch {
            delay(timeoutMs)
            onClick()
        }
    }

    /** Whether a press at [position] completes a double-click, in which case the click before it never acts. */
    fun completesDoubleClick(position: Offset): Boolean {
        val click = waiting?.takeIf { it.isActive } ?: return false
        if ((position - at).getDistance() > slop) return false
        click.cancel()
        waiting = null
        return true
    }
}
