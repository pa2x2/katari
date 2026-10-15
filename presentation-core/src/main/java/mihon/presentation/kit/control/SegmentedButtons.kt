package mihon.presentation.kit.control

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme

/**
 * A row of mutually exclusive options. [icon] draws an icon before each label in place of the selection check, such as
 * the arrows of a sort direction. [description] is read out instead of a label that is abbreviated to fit, such as
 * "Descending" for "Desc".
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
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val spoken = description?.invoke(option)
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier,
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                enabled = enabled,
                icon = if (icon != null) {
                    { Icon(icon(option), contentDescription = null, Modifier.size(KatariTheme.sizes.iconSmall)) }
                } else {
                    { SegmentedButtonDefaults.Icon(isSelected) }
                },
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}

/** A row of options that can each be on or off. */
@Composable
fun <T> MultiSegmentedButtons(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T, Boolean) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MultiChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, option ->
            val checked = option in selected
            SegmentedButton(
                checked = checked,
                onCheckedChange = { onToggle(option, it) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                enabled = enabled,
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}
