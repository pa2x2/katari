package mihon.presentation.kit.list

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.saket.swipe.SwipeableActionsBox
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor

/**
 * What swiping a row far enough does. [icon] shows under the row as it slides away; [isUndo] marks an action that
 * reverses the row's state, such as marking a read chapter unread.
 */
@Immutable
class SwipeAction(
    val icon: ImageVector,
    val onSwipe: () -> Unit,
    val isUndo: Boolean = false,
)

/** A row that runs [start] when swiped from its start edge and [end] when swiped from its end edge. */
@Composable
fun SwipeActionsRow(
    start: SwipeAction?,
    end: SwipeAction?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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

private val SwipeThreshold = 56.dp
