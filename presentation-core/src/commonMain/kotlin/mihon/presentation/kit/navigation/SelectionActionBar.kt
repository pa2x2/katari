package mihon.presentation.kit.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.window.WindowWidth
import mihon.presentation.kit.window.currentWindowLayout

/**
 * The bar of actions on a selection. In a narrow window it is raised from the bottom edge above the navigation bar and
 * shares its width between the actions; from a medium one it floats as a pill centred over the content, only as wide
 * as its actions, so they don't spread across the screen. Fill it with [SelectionAction]s; show and hide it with the
 * selection.
 */
@Composable
fun SelectionActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    if (currentWindowLayout().width == WindowWidth.Compact) {
        Surface(
            modifier = modifier,
            container = SurfaceContainer.High,
            shape = KatariTheme.shapes.extraLargeTop,
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.m),
                content = content,
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                // Centred over the content rather than the window when a side dock floats beside it
                .padding(start = LocalDockClearance.current.start)
                .padding(KatariTheme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                container = SurfaceContainer.High,
                shape = KatariTheme.shapes.full,
                shadow = true,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                ) {
                    CompositionLocalProvider(LocalFloatingSelectionBar provides true) { content() }
                }
            }
        }
    }
}

/**
 * One action of a [SelectionActionBar]; the bar shares its width between them. With [showLabel] the action is
 * captioned; otherwise only [revealLabel] shows the caption, widening the action, such as briefly after a long press
 * tells what an icon does. [menu] holds a dropdown anchored to the action.
 */
@Composable
fun RowScope.SelectionAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    revealLabel: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
) {
    val weight by animateFloatAsState(
        targetValue = if (revealLabel) REVEALED_WEIGHT else 1f,
        label = "weight",
    )
    // A floating bar is only as wide as its actions, so each takes a fixed width rather than a share
    val share = if (LocalFloatingSelectionBar.current) {
        Modifier.width(FloatingActionWidth * weight)
    } else {
        Modifier.weight(weight)
    }
    Box(
        modifier = modifier
            .then(
                if (showLabel) {
                    Modifier.heightIn(min = KatariTheme.sizes.touchTarget)
                } else {
                    Modifier.size(KatariTheme.sizes.touchTarget)
                },
            )
            .then(share)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = title)
            if (showLabel) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        top = KatariTheme.spacing.xs,
                        start = KatariTheme.spacing.xxs,
                        end = KatariTheme.spacing.xxs,
                    ),
                    role = TextRole.Badge,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            } else {
                AnimatedVisibility(
                    visible = revealLabel,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Text(text = title, role = TextRole.Badge, overflow = TextOverflow.Visible, maxLines = 1)
                }
            }
        }
        menu?.invoke()
    }
}

private const val REVEALED_WEIGHT = 2f

private val FloatingActionWidth = 88.dp

private val LocalFloatingSelectionBar = staticCompositionLocalOf { false }
