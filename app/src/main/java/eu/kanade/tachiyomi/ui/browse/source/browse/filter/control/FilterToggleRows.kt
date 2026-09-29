package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.IndeterminateCheckBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter

/** A boolean filter option. */
@Composable
internal fun FilterCheckboxRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .toggleRowPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A filter option that cycles between not used, included, and excluded, announcing which one it is. */
@Composable
internal fun FilterTriStateRow(filter: EntryFilter.TriState, onUpdate: () -> Unit) {
    val state = filter.optionState
    val description = state.description()
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) {
                filter.state = state.next().triStateValue
                onUpdate()
            }
            .semantics {
                stateDescription = description
                toggleableState = when (state) {
                    FilterOptionState.NotUsed -> ToggleableState.Off
                    FilterOptionState.Included -> ToggleableState.On
                    FilterOptionState.Excluded -> ToggleableState.Indeterminate
                }
            }
            .toggleRowPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = when (state) {
                FilterOptionState.NotUsed -> Icons.Rounded.CheckBoxOutlineBlank
                FilterOptionState.Included -> Icons.Rounded.CheckBox
                FilterOptionState.Excluded -> Icons.Rounded.IndeterminateCheckBox
            },
            contentDescription = null,
            tint = when (state) {
                FilterOptionState.NotUsed -> colors.onSurfaceVariant
                FilterOptionState.Included -> colors.primary
                FilterOptionState.Excluded -> colors.error
            },
        )
        Text(
            text = filter.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state == FilterOptionState.Excluded) colors.error else colors.onSurface,
            textDecoration = if (state == FilterOptionState.Excluded) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (state != FilterOptionState.NotUsed) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = if (state == FilterOptionState.Excluded) colors.error else colors.primary,
            )
        }
    }
}

private fun Modifier.toggleRowPadding(): Modifier =
    padding(horizontal = FilterSheetInsets.Horizontal, vertical = FilterSheetInsets.RowVertical)
