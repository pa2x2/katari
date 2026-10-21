package mihon.presentation.kit.surface

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.KatariTheme

/** How a card separates itself from the background. */
enum class CardStyle { Filled, Elevated, Outlined }

/**
 * A rounded container for one item or group. Pad its content with KatariTheme.spacing. [container] fills it with
 * another role than [style]'s, such as a notice in the primary container or a card raised a step above a sheet.
 * [highlighted] outlines it in the primary colour, such as the spot a dragged item would land in.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.Filled,
    container: SurfaceContainer? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill = container ?: when (style) {
        CardStyle.Filled -> SurfaceContainer.Highest
        CardStyle.Elevated -> SurfaceContainer.Low
        CardStyle.Outlined -> SurfaceContainer.Surface
    }
    val shape = KatariTheme.shapes.medium
    val outline = if (highlighted) {
        modifier.border(KatariTheme.sizes.selectedBorder, KatariTheme.colors.primary, shape)
    } else {
        modifier
    }
    val body: @Composable () -> Unit = { Column(content = content) }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = outline,
            container = fill,
            shape = shape,
            outlined = style == CardStyle.Outlined,
            shadow = style == CardStyle.Elevated,
            onLongClick = onLongClick,
            enabled = enabled,
            content = body,
        )
    } else {
        Surface(
            modifier = outline,
            container = fill,
            shape = shape,
            outlined = style == CardStyle.Outlined,
            shadow = style == CardStyle.Elevated,
            content = body,
        )
    }
}
