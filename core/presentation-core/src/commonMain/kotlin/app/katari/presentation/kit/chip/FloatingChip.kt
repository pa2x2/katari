package app.katari.presentation.kit.chip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * A chip floating over a list that moves through it, such as to items that just arrived or back to the top. [loading]
 * shows the wait indicator in place of [icon] while the move is being prepared.
 */
@Composable
fun FloatingChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.full,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall))
            } else {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(text = label, role = TextRole.Label)
        }
    }
}
