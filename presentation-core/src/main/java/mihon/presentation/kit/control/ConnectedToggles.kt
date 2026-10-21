package mihon.presentation.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = KatariTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        itemsIndexed(options, key = { _, option -> key(option) }) { index, option ->
            val checked = selected(option)
            ConnectedToggle(
                checked = checked,
                onCheckedChange = { onClick(option) },
                index = index,
                count = options.size,
                label = label(option),
                icon = MaterialSymbols.Rounded.Check.takeIf { checked },
            )
        }
    }
}

/**
 * A few options drawn as one connected group that sits inline with other controls, such as a search's Pinned / All
 * next to its own filter chip. Unlike [ConnectedToggleRow] it doesn't scroll, so keep it to options that fit. None may
 * be [selected] while something outside the group is in effect, such as a catalogue's filtered results.
 */
@Composable
fun <T> ConnectedToggleGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    onClick: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = selected(option)
            ConnectedToggle(
                checked = checked,
                onCheckedChange = { onClick(option) },
                index = index,
                count = options.size,
                label = label(option),
                icon = MaterialSymbols.Rounded.Check.takeIf { checked },
            )
        }
    }
}

/** One option of a connected group: [index] of [count] picks which corners it rounds. */
@Composable
internal fun ConnectedToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = connectedShapes(index, count),
        colors = connectedToggleColors(),
        icon = icon?.let { { Icon(it, contentDescription = null, Modifier.size(KatariTheme.sizes.iconSmall)) } },
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun connectedToggleColors(): ToggleButtonColors {
    val colors = KatariTheme.colors
    return ToggleButtonDefaults.colors(
        containerColor = colors.surfaceContainerHighest,
        contentColor = colors.onSurfaceVariant,
        checkedContainerColor = colors.secondaryContainer,
        checkedContentColor = colors.onSecondaryContainer,
    )
}

@Composable
private fun connectedShapes(index: Int, count: Int): ToggleButtonShapes = when {
    count == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}
