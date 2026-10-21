package mihon.entry.viewer.chrome.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.SidePanel
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.KatariTheme

/**
 * A modal side panel docked to the end of a reader or player, drawn in the viewer's own window so the system bars stay
 * hidden. Tapping the shade beside it dismisses it. Its content is disposed while hidden.
 */
@Composable
fun OverlayDrawer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    drawerModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceContainer.Scrim)
                    .clickable(onClick = onDismissRequest),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.94f)
                .then(drawerModifier)
                .padding(end = spacing.m, top = spacing.m, bottom = spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                SidePanel(modifier = Modifier.fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize(), content = content)
                }
            }
        }
    }
}
