package mihon.presentation.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme

/**
 * A scrolling row of options drawn as one connected group of toggle buttons, such as the library's content types. The
 * selected option rounds out into a pill and carries a check. The row scrolls the selected option into view when it
 * changes elsewhere, such as by swiping the pages it picks between.
 */
@Composable
fun <T> ConnectedToggleRow(
    options: List<T>,
    key: (T) -> Any,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = options.indexOfFirst(selected)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    val colors = KatariTheme.colors
    val toggleColors = ToggleButtonDefaults.colors(
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurfaceVariant,
        checkedContainerColor = colors.secondaryContainer,
        checkedContentColor = colors.onSecondaryContainer,
    )
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = KatariTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        itemsIndexed(options, key = { _, option -> key(option) }) { index, option ->
            val checked = selected(option)
            ToggleButton(
                checked = checked,
                onCheckedChange = { onClick(option) },
                shapes = connectedShapes(index, options.size),
                colors = toggleColors,
                icon = if (checked) {
                    {
                        Icon(
                            MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            Modifier.size(KatariTheme.sizes.iconSmall),
                        )
                    }
                } else {
                    null
                },
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

@Composable
private fun connectedShapes(index: Int, count: Int): ToggleButtonShapes = when {
    count == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}
