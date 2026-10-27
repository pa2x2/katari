package mihon.presentation.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check

/**
 * A row of mutually exclusive options, drawn as one connected group of equally wide toggle buttons like
 * [ConnectedToggleRow]. [icon] draws an icon before each label in place of the selection check, such as the arrows of
 * a sort direction. [description] is read out instead of a label that is abbreviated to fit, such as "Descending" for
 * "Desc". Past four options a segment is too narrow for the check beside its label, so the selected one is marked by
 * its fill alone, such as a daily goal's six durations.
 */
@Composable
fun <T> SegmentedButtons(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ((T) -> ImageVector)? = null,
    description: (@Composable (T) -> String)? = null,
) {
    val showCheck = options.size <= MAX_CHECKED_OPTIONS
    ConnectedRow(modifier) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val spoken = description?.invoke(option)
            ConnectedToggle(
                checked = isSelected,
                onCheckedChange = { onSelect(option) },
                index = index,
                count = options.size,
                label = label(option),
                icon = icon?.invoke(option) ?: MaterialSymbols.Rounded.Check.takeIf { isSelected && showCheck },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .then(if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier),
            )
        }
    }
}

/** A row of options that can each be on or off, drawn like [SegmentedButtons]. */
@Composable
fun <T> MultiSegmentedButtons(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T, Boolean) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ConnectedRow(modifier) {
        options.forEachIndexed { index, option ->
            val checked = option in selected
            ConnectedToggle(
                checked = checked,
                onCheckedChange = { onToggle(option, it) },
                index = index,
                count = options.size,
                label = label(option),
                icon = MaterialSymbols.Rounded.Check.takeIf { checked },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// As wide as the widest option needs for every option, unless the caller stretches the row
@Composable
private fun ConnectedRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .selectableGroup()
            .width(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        content = content,
    )
}

private const val MAX_CHECKED_OPTIONS = 4
