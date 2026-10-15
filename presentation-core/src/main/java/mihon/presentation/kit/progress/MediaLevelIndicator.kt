package mihon.presentation.kit.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * A vertical meter laid over video or pages in the media colours, for a level a gesture is changing, such as volume
 * or brightness. [level] is 0..1, filled from the bottom.
 */
@Composable
fun MediaLevelIndicator(
    level: Float,
    modifier: Modifier = Modifier,
) {
    val shape = KatariTheme.shapes.full
    val colors = KatariTheme.colors
    Box(
        modifier = modifier
            .size(width = MediaLevelIndicatorWidth, height = MediaLevelIndicatorHeight)
            .background(colors.onMedia.copy(alpha = TRACK_ALPHA), shape),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(level.coerceIn(0f, 1f))
                .background(colors.onMedia, shape),
        )
    }
}

private val MediaLevelIndicatorWidth = 6.dp

private val MediaLevelIndicatorHeight = 120.dp

private const val TRACK_ALPHA = 0.28f
