package mihon.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import mihon.presentation.kit.theme.KatariTheme

/** What artwork from data, such as a cover or an extension's icon, shows while it loads. */
@Composable
fun rememberArtworkPlaceholder(): Painter {
    val color = KatariTheme.colors.placeholder
    return remember(color) { ColorPainter(color) }
}
