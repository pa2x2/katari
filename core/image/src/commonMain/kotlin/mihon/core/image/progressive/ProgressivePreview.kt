package mihon.core.image.progressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import mihon.core.image.PlatformBitmap
import mihon.core.image.asImageBitmap

/**
 * What a screen shows of a page that arrives progressively: the frame to draw so far, or null, and whether a finished
 * replayable animation keeps playing instead of the decoded page.
 */
class ProgressivePreview(val image: ImageBitmap?, val keepsAnimation: Boolean)

/** What [state] has decoded so far, with a finished animation played through its frames. */
@Composable
fun rememberProgressivePreview(state: ProgressiveImageState?): ProgressivePreview {
    val bitmap = rememberProgressiveEntryPreviewBitmap(state)
    val keepsAnimation = state?.animation?.let { it.isComplete && it.isReplayable } == true
    return ProgressivePreview(image = bitmap?.asImageBitmap(), keepsAnimation = keepsAnimation)
}

@Composable
private fun rememberProgressiveEntryPreviewBitmap(state: ProgressiveImageState?): PlatformBitmap? {
    val animation = state?.animation
    val still = (state?.visual as? ProgressiveImageVisual.Still)?.bitmap
    val latestFrame = (state?.visual as? ProgressiveImageVisual.AnimationFrame)?.bitmap
    val firstGeneration = animation?.frames?.firstOrNull()?.generation
    var framePosition by remember(firstGeneration) { mutableIntStateOf(0) }
    var completedPlays by remember(firstGeneration) { mutableIntStateOf(0) }
    val latestAnimation by rememberUpdatedState(animation)

    LaunchedEffect(firstGeneration) {
        if (firstGeneration == null) return@LaunchedEffect
        framePosition = 0
        completedPlays = 0
        while (true) {
            val current = latestAnimation ?: return@LaunchedEffect
            val frame = current.frames.getOrNull(framePosition) ?: return@LaunchedEffect
            delay(frame.frame.durationMillis.coerceAtLeast(MINIMUM_FRAME_DURATION_MILLIS))

            var available = latestAnimation ?: return@LaunchedEffect
            if (framePosition >= available.frames.lastIndex && !available.isComplete) {
                snapshotFlow { latestAnimation }
                    .first { updated ->
                        updated == null ||
                            updated.frames.lastIndex > framePosition ||
                            updated.isComplete
                    }
                available = latestAnimation ?: return@LaunchedEffect
            }

            when {
                framePosition < available.frames.lastIndex -> framePosition++
                !available.isComplete || !available.isReplayable -> return@LaunchedEffect
                available.loopCount == 0 || completedPlays + 1 < (available.loopCount ?: 1) -> {
                    completedPlays++
                    framePosition = 0
                }
                else -> return@LaunchedEffect
            }
        }
    }

    return animation?.frames?.getOrNull(framePosition)?.bitmap ?: latestFrame ?: still
}

private const val MINIMUM_FRAME_DURATION_MILLIS = 10L
