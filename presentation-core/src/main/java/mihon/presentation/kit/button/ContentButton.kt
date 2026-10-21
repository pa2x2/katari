package mihon.presentation.kit.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * A borderless button around content the other buttons cannot hold, such as an icon with a label over video, an
 * animated indicator or a two-line choice. It shows only its press feedback, rounded to a pill; size and padding
 * belong to [content].
 */
@Composable
fun ContentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.None,
        shape = KatariTheme.shapes.full,
        enabled = enabled,
        content = content,
    )
}
