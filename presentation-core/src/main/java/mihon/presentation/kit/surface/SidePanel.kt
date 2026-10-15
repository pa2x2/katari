package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * A card docked to the side of the screen over video or pages, such as a player's episode list. It keeps the look of
 * [FloatingPanel]; its width stays wide enough for list rows on a phone without spreading across a tablet. Placement,
 * height and the shade behind it belong to the caller.
 */
@Composable
fun SidePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = modifier.widthIn(min = SidePanelMinWidth, max = SidePanelMaxWidth),
        shape = KatariTheme.shapes.extraLarge,
        color = KatariTheme.colors.surfaceContainerHigh,
        contentColor = KatariTheme.colors.onSurface,
        shadowElevation = SidePanelShadow,
        content = content,
    )
}

private val SidePanelMinWidth = 280.dp

private val SidePanelMaxWidth = 340.dp

private val SidePanelShadow = 8.dp
