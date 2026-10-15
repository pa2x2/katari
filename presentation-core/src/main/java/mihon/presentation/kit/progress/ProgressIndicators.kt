package mihon.presentation.kit.progress

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** A progress bar; without [progress] it runs indeterminately. [progress] is 0..1. */
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    if (progress == null) {
        androidx.compose.material3.LinearProgressIndicator(modifier)
    } else {
        androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = modifier)
    }
}

/** How much room a circular indicator takes. [Inline] fits next to text and inside buttons and rows. */
enum class ProgressSize { Inline, Default }

/**
 * A progress ring; without [progress] it spins indeterminately. [progress] is 0..1. [onContainer] marks a ring on a
 * filled container, whose tones would hide the default track; its track is then a tint of the ring's colour.
 */
@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    size: ProgressSize = ProgressSize.Default,
    onContainer: Boolean = false,
) {
    val sized = if (size == ProgressSize.Inline) modifier.size(KatariTheme.sizes.icon) else modifier
    if (progress == null) {
        androidx.compose.material3.CircularProgressIndicator(sized)
    } else if (onContainer) {
        androidx.compose.material3.CircularProgressIndicator(
            progress = progress,
            modifier = sized,
            trackColor = KatariTheme.colors.primary.copy(alpha = ON_CONTAINER_TRACK_ALPHA),
            gapSize = KatariTheme.sizes.none,
        )
    } else {
        androidx.compose.material3.CircularProgressIndicator(progress = progress, modifier = sized)
    }
}

private const val ON_CONTAINER_TRACK_ALPHA = 0.24f

/** The expressive wavy bar used for long-running work such as downloads. */
@Composable
fun WavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
) {
    if (circular) {
        CircularWavyProgressIndicator(progress = progress, modifier = modifier)
    } else {
        LinearWavyProgressIndicator(progress = progress, modifier = modifier)
    }
}
