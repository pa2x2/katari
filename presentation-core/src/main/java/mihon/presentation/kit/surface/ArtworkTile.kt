package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.theme.KatariSizes
import mihon.presentation.kit.theme.KatariTheme

/** Where an [ArtworkTile] sits, which sets its size. */
enum class ArtworkTileSize(val side: Dp) {
    /** Leading a card in a list of choices. */
    Card(KatariSizes.artwork),

    /** At the top of a details sheet or setup screen. */
    Header(KatariSizes.artworkLarge),
}

/**
 * A rounded tile holding an engine's or provider's artwork, such as an application icon or a bundled glyph. [content]
 * insets the image itself, since an application icon already carries its own padding and a glyph does not.
 */
@Composable
fun ArtworkTile(
    size: ArtworkTileSize,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.size(size.side),
        container = SurfaceContainer.Highest,
        shape = KatariTheme.shapes.large,
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}
