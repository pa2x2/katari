package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MAX_CHIP_OPTIONS = 5
private const val MAX_CHIP_LABEL_LENGTH = 16

/**
 * A single-choice filter: a row of chips when every option fits at a glance, otherwise a dropdown field that hides
 * the options behind the current value.
 */
@Composable
internal fun FilterSelectControl(
    label: String,
    options: Array<out Any?>,
    selectedIndex: Int,
    isError: Boolean,
    onSelect: (Int) -> Unit,
) {
    val labels = options.map { it.toString() }
    if (labels.size in 2..MAX_CHIP_OPTIONS && labels.all { it.length <= MAX_CHIP_LABEL_LENGTH }) {
        FilterChoiceChips(label, labels, selectedIndex, isError, onSelect)
    } else {
        FilterSelectField(label, labels, selectedIndex, isError, onSelect)
    }
}

@Composable
private fun FilterChoiceChips(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    isError: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = FilterSheetInsets.Horizontal, vertical = FilterSheetInsets.FieldVertical),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(index) },
                    label = { Text(option) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** A single-choice filter presented as a dropdown field. */
@Composable
private fun FilterSelectField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    isError: Boolean,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.filterField().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(text = label) },
            value = options.getOrNull(selectedIndex).orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
