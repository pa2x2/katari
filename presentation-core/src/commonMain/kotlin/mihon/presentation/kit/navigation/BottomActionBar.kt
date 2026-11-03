package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * A bar raised from the bottom edge above the navigation bar, holding a prompt and its actions in a row, such as
 * saving or discarding unsaved changes.
 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.extraLargeTop,
        shadow = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
