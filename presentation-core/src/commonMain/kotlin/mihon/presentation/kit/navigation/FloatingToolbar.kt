package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * Lays [toolbar], typically a [FloatingToolbar], over the bottom of [content], [bottomPadding] above the bottom edge,
 * such as above the home dock. [content] receives the bottom padding that scrolls its last items clear of the toolbar.
 */
@Composable
fun FloatingToolbarBox(
    bottomPadding: Dp,
    toolbar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val gap = KatariTheme.spacing.s
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val toolbarPlaceables = subcompose(FloatingToolbarSlot.Toolbar, toolbar).map { it.measure(loose) }
        val toolbarHeight = toolbarPlaceables.maxOfOrNull { it.height } ?: 0
        val toolbarBottom = (bottomPadding + gap).roundToPx()
        val contentPadding = PaddingValues(bottom = bottomPadding + gap * 2 + toolbarHeight.toDp())
        val contentPlaceables = subcompose(FloatingToolbarSlot.Content) { content(contentPadding) }
            .map { it.measure(constraints) }
        val width = contentPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = contentPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
        layout(width, height) {
            contentPlaceables.forEach { it.place(0, 0) }
            toolbarPlaceables.forEach { placeable ->
                val x = Alignment.CenterHorizontally.align(placeable.width, width, layoutDirection)
                placeable.place(x, height - toolbarBottom - placeable.height)
            }
        }
    }
}

/** A pill of controls floating over content, such as a feed's switcher beside its view buttons. */
@Composable
fun FloatingToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.padding(horizontal = KatariTheme.spacing.m),
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.full,
        shadow = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private enum class FloatingToolbarSlot { Toolbar, Content }
