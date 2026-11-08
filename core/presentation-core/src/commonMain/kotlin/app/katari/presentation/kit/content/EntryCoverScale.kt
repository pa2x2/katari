package app.katari.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.katari.presentation.kit.window.WindowWidth
import app.katari.presentation.kit.window.currentWindowLayout

/**
 * How much entry covers grow in the current window. The sizes in [LibraryMetrics], [BrowseMetrics] and the cover grids
 * name a cover at phone size; a desktop or a big tablet shows them larger, so covers stay large beside its screen
 * rather than many small ones filling it. Phones and foldables, folded or unfolded, keep those sizes.
 */
@Composable
@ReadOnlyComposable
fun entryCoverScale(): Float = when (currentWindowLayout().width) {
    WindowWidth.Compact, WindowWidth.Medium, WindowWidth.Expanded -> 1f
    WindowWidth.Large -> 1.25f
    WindowWidth.ExtraLarge -> 1.5f
}
