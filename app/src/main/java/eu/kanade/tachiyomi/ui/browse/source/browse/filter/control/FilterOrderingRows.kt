package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A sort filter: the current option as a row value that opens the option menu, and an always visible direction
 * control, so neither the choice nor its direction is hidden or changed silently.
 */
@Composable
internal fun FilterSortRow(filter: EntryFilter.Sort, onUpdate: () -> Unit) {
    val selection = filter.state
    FilterMenuRow(
        icon = Icons.AutoMirrored.Outlined.Sort,
        label = filter.name,
        value = selection?.let { filter.values.getOrNull(it.index) } ?: stringResource(MR.strings.filter_sort_unset),
        options = filter.values.toList(),
        selectedIndex = selection?.index,
        onSelect = { index ->
            filter.state = EntryFilter.Sort.Selection(index, selection?.ascending ?: true)
            onUpdate()
        },
    ) {
        FilterSortDirection(ascending = selection?.ascending, enabled = selection != null) { ascending ->
            filter.state = selection?.copy(ascending = ascending)
            onUpdate()
        }
    }
}

/** A single-choice ordering filter drawn in the same row style as [FilterSortRow]. */
@Composable
internal fun FilterOrderingSelectRow(filter: EntryFilter.Select<*>, onUpdate: () -> Unit) {
    FilterMenuRow(
        icon = Icons.Outlined.Tune,
        label = filter.name,
        value = filter.values.getOrNull(filter.state)?.toString().orEmpty(),
        options = filter.values.map { it.toString() },
        selectedIndex = filter.state,
        onSelect = {
            filter.state = it
            onUpdate()
        },
    )
}

@Composable
private fun FilterMenuRow(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = FilterSheetInsets.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(start = FilterSheetInsets.Horizontal, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        leadingIcon = {
                            if (index == selectedIndex) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            } else {
                                Spacer(Modifier.width(24.dp))
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun FilterSortDirection(ascending: Boolean?, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val descendingDescription = stringResource(MR.strings.filter_sort_descending)
    val ascendingDescription = stringResource(MR.strings.filter_sort_ascending)
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = ascending == false,
            onClick = { onChange(false) },
            enabled = enabled,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, Modifier.size(18.dp)) },
            modifier = Modifier.semantics { contentDescription = descendingDescription },
        ) {
            Text(stringResource(MR.strings.filter_sort_desc))
        }
        SegmentedButton(
            selected = ascending == true,
            onClick = { onChange(true) },
            enabled = enabled,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null, Modifier.size(18.dp)) },
            modifier = Modifier.semantics { contentDescription = ascendingDescription },
        ) {
            Text(stringResource(MR.strings.filter_sort_asc))
        }
    }
}
