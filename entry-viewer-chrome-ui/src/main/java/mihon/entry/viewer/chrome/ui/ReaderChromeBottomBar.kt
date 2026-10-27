package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme

/** Shared reader bottom bar with feature-owned controls. */
@Composable
fun ReaderChromeBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        container = SurfaceContainer.Chrome,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = KatariTheme.spacing.s)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Reader chrome action whose full share of the bottom bar is touchable. [shareWidth] false keeps it at its own touch
 * target, for a row of controls that floats over the page instead of spanning the window.
 */
@Composable
fun RowScope.ReaderChromeBottomBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
    shareWidth: Boolean = true,
) {
    IconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = if (shareWidth) {
            modifier
                .weight(1f)
                .heightIn(min = KatariTheme.sizes.touchTarget + KatariTheme.spacing.s)
        } else {
            modifier
        },
        tone = tone,
    )
}
