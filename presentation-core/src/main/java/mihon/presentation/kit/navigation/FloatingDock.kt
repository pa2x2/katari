package mihon.presentation.kit.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariSizes
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Lays [dock], typically a [FloatingDock], over [content] at the bottom edge. Content scrolls on under the dock: every
 * [Scaffold] inside adds the dock's height to its bottom padding and lifts its FAB and snackbars above it, for as long
 * as [dockVisible] holds.
 */
@Composable
fun FloatingDockHost(
    dockVisible: Boolean,
    dock: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalDockClearance provides if (dockVisible) DockClearance else 0.dp,
            content = content,
        )
        AnimatedVisibility(
            visible = dockVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            dock()
        }
    }
}

/**
 * The home destinations on a pill floating above the bottom edge, centred and only as wide as its items. Fill it with
 * [FloatingDockItem]s.
 */
@Composable
fun FloatingDock(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = KatariTheme.spacing.l)
            .padding(bottom = DockMargin),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            shape = KatariTheme.shapes.full,
            color = KatariTheme.colors.surfaceContainerHigh,
            contentColor = KatariTheme.colors.onSurface,
            shadowElevation = DockElevation,
        ) {
            Row(
                modifier = Modifier
                    .padding(DockPadding)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * One destination of a [FloatingDock]. Only the selected one shows its [label] beside the icon, on a tonal pill; it
 * gives up label width first when the dock runs out of room. [icon] is a slot so a destination can animate its icon or
 * carry a badge, and [menu] holds a dropdown anchored to the item.
 */
@Composable
fun RowScope.FloatingDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable () -> Unit)? = null,
) {
    val colors = KatariTheme.colors
    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
        label = "dockItemContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        label = "dockItemContent",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.then(if (selected) Modifier.weight(1f, fill = false) else Modifier)) {
        // The pill and its ripple sit behind the item rather than clipping it, so an icon's badge can overhang
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .clip(KatariTheme.shapes.full)
                .background(container)
                .indication(interactionSource, ripple()),
        )
        Row(
            modifier = Modifier
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = null,
                )
                .height(KatariTheme.sizes.touchTarget)
                .padding(horizontal = DockItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                icon()
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = KatariTheme.spacing.s),
                        role = TextRole.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (menu != null) {
            // The menu anchors to a box raised past the dock's padding, so it opens clear of the pill, not over its edge
            Box(modifier = Modifier.matchParentSize().offset(y = -(DockPadding + DockMenuGap))) { menu() }
        }
    }
}

/** How far a [Scaffold] keeps its content, FAB and snackbars above the bottom edge for a [FloatingDock] over it. */
internal val LocalDockClearance = compositionLocalOf { 0.dp }

private val DockPadding = KatariSpacing.s
private val DockItemPadding = 14.dp
private val DockMargin = KatariSpacing.m
private val DockElevation = 6.dp
private val DockMenuGap = KatariSpacing.xs

// The dock's height and margin, plus a gap so the last row doesn't end flush against it.
private val DockClearance: Dp = DockMargin + DockPadding * 2 + KatariSizes.touchTarget + KatariSpacing.s
