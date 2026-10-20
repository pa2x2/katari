package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * Where a row sits in a group of rows drawn as one card. Lazy lists compose every row as its own item, so the card is
 * drawn slice by slice: the outer rows round their outer corners and every row after the first draws the divider
 * above it.
 */
enum class RowGroupPosition {
    Alone,
    First,
    Middle,
    Last,
    ;

    companion object {
        fun of(index: Int, count: Int): RowGroupPosition = when {
            count == 1 -> Alone
            index == 0 -> First
            index == count - 1 -> Last
            else -> Middle
        }
    }
}

/**
 * One row's slice of a row group card. The rows inside keep their own padding against the card's edge rather than the
 * screen's. [selected] fills the slice, such as the open category in a two-pane settings list.
 */
@Composable
fun RowGroupItem(
    position: RowGroupPosition,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val large = MaterialTheme.shapes.large
    val square = CornerSize(0)
    val shape = when (position) {
        RowGroupPosition.Alone -> large
        RowGroupPosition.First -> large.copy(bottomStart = square, bottomEnd = square)
        RowGroupPosition.Middle -> KatariTheme.shapes.none
        RowGroupPosition.Last -> large.copy(topStart = square, topEnd = square)
    }
    Surface(
        modifier = modifier.padding(horizontal = KatariTheme.spacing.m),
        container = if (selected) SurfaceContainer.Selected else SurfaceContainer.Default,
        shape = shape,
    ) {
        Column {
            if (position == RowGroupPosition.Middle || position == RowGroupPosition.Last) {
                HorizontalDivider()
            }
            CompositionLocalProvider(
                LocalRowInset provides KatariTheme.spacing.l,
                LocalInRowGroup provides true,
                content = content,
            )
        }
    }
}

/** The header above a row group card, lined up with the text inside the card rather than the card's edge. */
@Composable
fun RowGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalRowInset provides KatariTheme.spacing.m + KatariTheme.spacing.l) {
        SectionHeader(text = text, modifier = modifier, trailing = trailing, actions = actions)
    }
}

/** Rows inside a row group card set their icon on a tonal tile, as the card already separates them from the screen. */
internal val LocalInRowGroup = staticCompositionLocalOf { false }

@Composable
internal fun RowIconTile(icon: ImageVector) {
    Surface(container = SurfaceContainer.SecondaryContainer, shape = KatariTheme.shapes.small) {
        Box(modifier = Modifier.size(IconTileSize), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null)
        }
    }
}

private val IconTileSize = 40.dp
