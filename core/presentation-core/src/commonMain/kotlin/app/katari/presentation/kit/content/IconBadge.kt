package app.katari.presentation.kit.content

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme

/** A small icon on a round tonal disc, marking a step between items, such as an entry giving way to its replacement. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(KatariTheme.sizes.icon),
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.full,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(KatariTheme.spacing.xs))
    }
}
