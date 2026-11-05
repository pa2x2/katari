package mihon.presentation.kit.list

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.saket.swipe.SwipeableActionsBox
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.pointer.LocalWindowHasMouse
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import tachiyomi.presentation.core.components.material.IconButtonTokens

/**
 * What swiping a row far enough does. [icon] shows under the row as it slides away; [isUndo] marks an action that
 * reverses the row's state, such as marking a read chapter unread. [label] names it where it is a button instead.
 */
@Immutable
class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val onSwipe: () -> Unit,
    val isUndo: Boolean = false,
)

/**
 * A row that runs [start] when swiped from its start edge and [end] when swiped from its end edge. A window worked with
 * a mouse has nothing to swipe with, so there the row shows them as buttons before its trailing controls while the
 * pointer is over it.
 */
@Composable
fun SwipeActionsRow(
    start: SwipeAction?,
    end: SwipeAction?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalWindowHasMouse.current) {
        val hover = remember { MutableInteractionSource() }
        val hovered by hover.collectIsHoveredAsState()
        val actions = listOfNotNull(start, end)
        Box(modifier = modifier.hoverable(hover)) {
            CompositionLocalProvider(
                LocalRowHoverActions provides RowHoverActions(actions, shown = hovered && actions.isNotEmpty()),
                content = content,
            )
        }
        return
    }

    val colors = KatariTheme.colors
    val toLibrary = { action: SwipeAction ->
        me.saket.swipe.SwipeAction(
            icon = {
                CompositionLocalProvider(LocalContentColor provides colors.onPrimaryContainer) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(KatariTheme.spacing.l),
                    )
                }
            },
            background = colors.primaryContainer,
            onSwipe = action.onSwipe,
            isUndo = action.isUndo,
        )
    }
    SwipeableActionsBox(
        modifier = modifier.clipToBounds(),
        startActions = listOfNotNull(start?.let(toLibrary)),
        endActions = listOfNotNull(end?.let(toLibrary)),
        swipeThreshold = SwipeThreshold,
        backgroundUntilSwipeThreshold = colors.surfaceContainerLowest,
        content = { content() },
    )
}

/** The swipe actions of the [SwipeActionsRow] around a row, as buttons, [shown] while the pointer is over it. */
@Immutable
internal class RowHoverActions(val actions: List<SwipeAction>, val shown: Boolean)

/** Null outside a [SwipeActionsRow] in a window worked with a mouse. */
internal val LocalRowHoverActions = staticCompositionLocalOf<RowHoverActions?> { null }

/**
 * The enclosing [SwipeActionsRow]'s actions as buttons, for a row to place among its trailing controls.
 *
 * They come and go with the pointer, so they must never be what sets the row's height. They only ever face a pointer,
 * which needs no touch target, so they take the footprint of the row's own controls, such as a
 * [DownloadIndicator][mihon.presentation.kit.button.DownloadIndicator], instead of a touch target that would make the
 * row taller while hovered.
 */
@Composable
internal fun RowHoverActionButtons() {
    val hoverActions = LocalRowHoverActions.current ?: return
    if (!hoverActions.shown) return
    hoverActions.actions.forEach { action ->
        IconButton(
            icon = action.icon,
            contentDescription = action.label,
            onClick = action.onSwipe,
            modifier = Modifier.size(IconButtonTokens.StateLayerSize),
            tone = ContentTone.Secondary,
        )
    }
}

private val SwipeThreshold = 56.dp
