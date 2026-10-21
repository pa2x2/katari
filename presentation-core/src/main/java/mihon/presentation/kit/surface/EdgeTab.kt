package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** The screen edge an [EdgeTab] is tucked into. */
enum class TabEdge { Start, End }

/**
 * A small tab tucked into a screen edge over a full-screen view, standing in for controls moved out of the way and
 * bringing them back when tapped. Only its corners away from the edge are rounded.
 */
@Composable
fun EdgeTab(
    edge: TabEdge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.Chrome,
        shape = when (edge) {
            TabEdge.Start -> KatariTheme.shapes.extraLargeEnd
            TabEdge.End -> KatariTheme.shapes.extraLargeStart
        },
    ) {
        Box(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.xs, vertical = KatariTheme.spacing.m),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
