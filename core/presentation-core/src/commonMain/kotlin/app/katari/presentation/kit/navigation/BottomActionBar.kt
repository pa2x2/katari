package app.katari.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.currentWindowLayout

/**
 * A bar raised from the bottom edge above the navigation bar, holding a prompt and its actions in a row, such as
 * saving or discarding unsaved changes. Its fill runs under the whole window while its contents line up with the
 * screen's frame; in a short window it keeps less room around them. A screen's main actions aren't put here by hand:
 * the [Scaffold] places its `actions`.
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    Surface(
        modifier = modifier,
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.extraLargeTop,
        shadow = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(AppBarInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(
                    horizontal = spacing.l,
                    vertical = if (currentWindowLayout().isShort) spacing.xs else spacing.s,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
