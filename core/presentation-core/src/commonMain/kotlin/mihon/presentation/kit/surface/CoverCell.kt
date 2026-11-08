package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/**
 * A cell of a cover grid around a cover and its title. [selected] fills the cell; its corners are the cover's plus the
 * cell's padding, so the fill stays concentric with the cover.
 */
@Composable
fun CoverCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        container = if (selected) SurfaceContainer.Selected else SurfaceContainer.None,
        shape = KatariTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.padding(KatariTheme.spacing.xs)) {
            content()
        }
    }
}
