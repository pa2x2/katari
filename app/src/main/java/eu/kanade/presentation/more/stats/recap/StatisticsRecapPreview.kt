package eu.kanade.presentation.more.stats.recap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.min

/**
 * Shows the recap card as large as the space allows, centered. The card keeps its fixed size for the shared image;
 * only its on-screen drawing is scaled, so what's captured from inside the card doesn't change.
 */
@Composable
internal fun StatisticsRecapPreview(
    modifier: Modifier = Modifier,
    card: @Composable () -> Unit,
) {
    Layout(content = card, modifier = modifier) { measurables, constraints ->
        val placeable = measurables.single().measure(Constraints())
        val scale = min(
            constraints.maxWidth / placeable.width.toFloat(),
            constraints.maxHeight / placeable.height.toFloat(),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Scaling happens around the card's center, so centering its unscaled box centers the scaled one.
            placeable.placeWithLayer(
                x = (constraints.maxWidth - placeable.width) / 2,
                y = (constraints.maxHeight - placeable.height) / 2,
            ) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
}
