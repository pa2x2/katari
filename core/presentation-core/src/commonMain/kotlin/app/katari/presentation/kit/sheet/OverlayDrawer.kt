package app.katari.presentation.kit.sheet

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
import androidx.compose.ui.graphics.graphicsLayer
import app.katari.presentation.kit.dialog.LIGHT_DIM_FRACTION
import app.katari.presentation.kit.surface.SidePanel
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.surface.background
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A modal side panel docked to the end of a reader or player, drawn in the viewer's own window so the system bars stay
 * hidden. Tapping beside it dismisses it, and [backdrop] says whether the viewer behind is shaded meanwhile. Its content
 * is disposed while hidden.
 */
@Composable
fun OverlayDrawer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    drawerModifier: Modifier = Modifier,
    backdrop: SheetBackdrop = SheetBackdrop.Dimmed,
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
            // The backdrop is the viewer, not a control: a hover or press shade across all of it would mark the
            // whole viewer as a button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (backdrop) {
                            SheetBackdrop.Dimmed -> Modifier.background(SurfaceContainer.Scrim)
                            SheetBackdrop.Light ->
                                Modifier
                                    .graphicsLayer { alpha = LIGHT_DIM_FRACTION }
                                    .background(SurfaceContainer.Scrim)
                            SheetBackdrop.Clear -> Modifier
                        },
                    )
                    .clickable(interactionSource = null, indication = null, onClick = onDismissRequest),
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
