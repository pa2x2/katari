package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A group of mutually exclusive options shown as icon-and-label tiles, such as reading modes. [wideTiles] fits
 * fewer, wider tiles per line for options whose labels are long.
 */
@Composable
fun <T> IconChoiceGrid(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: (T) -> ImageVector,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    wideTiles: Boolean = false,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (wideTiles) WideTileMinWidth else TileMinWidth),
        modifier = modifier.padding(
            start = LocalRowInset.current,
            end = LocalRowInset.current,
            bottom = KatariTheme.spacing.s,
        ),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        items(options) { option ->
            FilledIconToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KatariTheme.sizes.touchTarget),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KatariTheme.spacing.s),
                ) {
                    Icon(icon(option), contentDescription = null)
                    Text(label(option), role = TextRole.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val TileMinWidth = 128.dp
private val WideTileMinWidth = 200.dp
