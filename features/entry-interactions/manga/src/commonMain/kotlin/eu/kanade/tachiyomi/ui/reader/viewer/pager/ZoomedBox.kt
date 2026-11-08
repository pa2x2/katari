package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import mihon.entry.interactions.manga.reader.image.tiles.PageImageView
import kotlin.math.roundToInt

/**
 * Lays the page out at its fitted size and moves it as [zoom] says without laying it out again, so a pinch redraws
 * the tiles it has at a new scale until sharper ones are decoded.
 */
@Composable
internal fun ZoomedBox(zoom: PagerPageZoom, content: @Composable () -> Unit) {
    Box(
        Modifier.layout { measurable, constraints ->
            val width = (zoom.pageSize.width * zoom.fittedScale).roundToInt()
            val height = (zoom.pageSize.height * zoom.fittedScale).roundToInt()
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeWithLayer(0, 0) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    val magnification = zoom.scale / zoom.fittedScale
                    scaleX = magnification
                    scaleY = magnification
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                }
            }
        },
        // The page fills the fitted size; a Box would otherwise let content without a size of its own collapse
        propagateMinConstraints = true,
    ) {
        content()
    }
}

/** The part of the fitted page on screen, in the fitted page's pixels, and how far it is magnified beyond fitting. */
internal fun PagerPageZoom.tileView(): PageImageView {
    val magnification = scale / fittedScale
    val width = (pageSize.width * fittedScale).roundToInt()
    val height = (pageSize.height * fittedScale).roundToInt()
    val left = (-offset.x / magnification).roundToInt().coerceIn(0, width)
    val top = (-offset.y / magnification).roundToInt().coerceIn(0, height)
    val right = ((slotSize.width - offset.x) / magnification).roundToInt().coerceIn(0, width)
    val bottom = ((slotSize.height - offset.y) / magnification).roundToInt().coerceIn(0, height)
    return PageImageView(IntRect(left, top, right, bottom), magnification)
}
