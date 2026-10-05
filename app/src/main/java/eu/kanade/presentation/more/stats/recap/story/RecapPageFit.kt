package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.min

/**
 * Shows a page as large as the space allows, centered, with rounded corners. The page keeps its fixed size for the
 * shared image; only its on-screen drawing is scaled and clipped, so what's captured from inside it doesn't change.
 */
@Composable
internal fun RecapPageFit(
    modifier: Modifier = Modifier,
    page: @Composable () -> Unit,
) {
    Layout(content = page, modifier = modifier) { measurables, constraints ->
        val placeable = measurables.single().measure(Constraints())
        val scale = min(
            constraints.maxWidth / placeable.width.toFloat(),
            constraints.maxHeight / placeable.height.toFloat(),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Scaling happens around the page's center, so centering its unscaled box centers the scaled one.
            placeable.placeWithLayer(
                x = (constraints.maxWidth - placeable.width) / 2,
                y = (constraints.maxHeight - placeable.height) / 2,
            ) {
                scaleX = scale
                scaleY = scale
                shape = PageShape
                clip = true
            }
        }
    }
}

private val PageShape = RoundedCornerShape(percent = 4)
