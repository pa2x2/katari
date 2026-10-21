package mihon.presentation.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * Whether an item is part of the selection, shown on the item while selecting, such as at a cover's corner. [onMedia]
 * draws the empty mark so it reads over artwork.
 */
@Composable
fun SelectionMark(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onMedia: Boolean = false,
) {
    val colors = KatariTheme.colors
    val shape = KatariTheme.shapes.full
    if (selected) {
        Surface(
            modifier = modifier.size(KatariTheme.sizes.icon),
            container = SurfaceContainer.Primary,
            shape = shape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(KatariTheme.sizes.icon)
                .clip(shape)
                .background(if (onMedia) colors.mediaScrim else Color.Transparent)
                .border(KatariTheme.sizes.selectedBorder, if (onMedia) colors.onMedia else colors.outline, shape),
        )
    }
}
