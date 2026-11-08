package mihon.presentation.kit.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariSizes
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** The items of a [FloatingDock], laid out along its [DockPlacement]; [FloatingDockItem] fills either. */
sealed interface DockScope

internal class BottomDockScope(val row: RowScope) : DockScope

internal class SideDockScope(val column: ColumnScope) : DockScope

/**
 * One destination of a [FloatingDock]. Only the selected one shows its [label] on a tonal pill: beside the icon at the
 * bottom, where it gives up label width first when the dock runs out of room, and under the icon at the side. [icon] is
 * a slot so a destination can animate its icon or carry a badge, and [menu] holds a dropdown anchored to the item.
 */
@Composable
fun DockScope.FloatingDockItem(
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
    val selectable = Modifier.selectable(
        selected = selected,
        onClick = onClick,
        role = Role.Tab,
        interactionSource = interactionSource,
        indication = null,
    )
    val scopeModifier = when (this) {
        is BottomDockScope -> with(row) { if (selected) Modifier.weight(1f, fill = false) else Modifier }
        is SideDockScope -> Modifier
    }
    Box(modifier = modifier.then(scopeModifier)) {
        // The pill and its ripple sit behind the item rather than clipping it, so an icon's badge can overhang
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .clip(KatariTheme.shapes.full)
                .background(container)
                .indication(interactionSource, ripple()),
        )
        CompositionLocalProvider(LocalContentColor provides content) {
            when (this@FloatingDockItem) {
                is BottomDockScope -> Row(
                    modifier = selectable
                        .height(KatariTheme.sizes.touchTarget)
                        .padding(horizontal = DockItemPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                is SideDockScope -> Column(
                    modifier = selectable
                        .width(SideDockItemWidth)
                        .heightIn(min = KatariTheme.sizes.touchTarget)
                        .padding(vertical = KatariTheme.spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    icon()
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier
                                .padding(horizontal = KatariTheme.spacing.xs)
                                .padding(top = KatariTheme.spacing.xs),
                            role = TextRole.Label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        if (menu != null) {
            // The menu anchors to a box moved past the dock's padding, so it opens clear of the pill, not over its edge:
            // above a bottom dock, and beside a side dock from the item's top, flipping to end there when there's no room
            val menuOffset = DockPadding + DockMenuGap
            val anchor = when (this@FloatingDockItem) {
                is BottomDockScope ->
                    Modifier
                        .matchParentSize()
                        .offset(y = -menuOffset)
                is SideDockScope ->
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = menuOffset)
                        .size(0.dp)
            }
            Box(modifier = anchor) { menu() }
        }
    }
}

private val DockItemPadding = 14.dp
private val DockMenuGap = KatariSpacing.xs

/** Wide enough for a short label such as "Library" under the icon; longer ones end in an ellipsis. */
internal val SideDockItemWidth: Dp = KatariSizes.touchTarget + KatariSpacing.l
