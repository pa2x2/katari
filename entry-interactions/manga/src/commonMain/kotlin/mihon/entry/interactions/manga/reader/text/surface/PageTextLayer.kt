package mihon.entry.interactions.manga.reader.text.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.source.PageImage
import mihon.presentation.kit.content.PageTextCover
import mihon.presentation.kit.content.PageTextMarks

/**
 * Covers the displayed page of a still [image] in [layout], keeping [surface] current and drawing the marks text
 * features set on it; [magnification] is how far the viewer magnifies the page on screen. [onChanged] is told when the
 * page starts or stops showing the image, once it is placed, so text features look at the visible pages again.
 */
@Composable
internal fun PageTextLayer(
    surface: ViewerPageTextSurface,
    image: PageImage.Still,
    layout: PageImageLayout,
    cropBorders: Boolean,
    magnification: () -> Float,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    val still = remember(image, layout, cropBorders) { ShownStill(image.size, layout, cropBorders) }
    DisposableEffect(surface) {
        onDispose {
            surface.shown = null
            surface.coordinates = null
            currentOnChanged()
        }
    }
    DisposableEffect(surface, still) {
        surface.shown = still
        // A page counts as visible only once placed; the first placement reports it otherwise
        if (surface.coordinates != null) currentOnChanged()
        onDispose {}
    }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier.onGloballyPositioned {
            val firstPlacement = surface.coordinates == null
            surface.coordinates = it
            boxSize = it.size
            if (firstPlacement) currentOnChanged()
        },
    ) {
        val decoration = surface.decoration ?: return@Box
        val size = decoration.imageSize
        PageTextMarks(
            regions = remember(decoration, still, boxSize) {
                decoration.regions.mapNotNull { still.inBox(it, size, boxSize) }
            },
            highlighted = remember(decoration, still, boxSize) {
                decoration.highlighted?.let { still.inBox(it, size, boxSize) }
            },
            covers = remember(decoration, still, boxSize) {
                decoration.overlays.mapNotNull { overlay ->
                    still.inBox(overlay.area, size, boxSize)?.let {
                        PageTextCover(it, overlay.text, overlay.background)
                    }
                }
            },
            magnification = magnification,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
