package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter

/** A boolean filter option. */
@Composable
internal fun FilterCheckboxRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    FilterToggleRow(onClick = onToggle) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A filter option that cycles between ignored, included, and excluded. */
@Composable
internal fun FilterTriStateRow(label: String, state: Int, onCycle: () -> Unit) {
    FilterToggleRow(onClick = onCycle) {
        Icon(
            imageVector = when (state) {
                EntryFilter.TriState.STATE_INCLUDE -> Icons.Rounded.CheckBox
                EntryFilter.TriState.STATE_EXCLUDE -> Icons.Rounded.DisabledByDefault
                else -> Icons.Rounded.CheckBoxOutlineBlank
            },
            contentDescription = null,
            tint = if (state == EntryFilter.TriState.STATE_IGNORE) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilterToggleRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FilterSheetInsets.Horizontal, vertical = FilterSheetInsets.RowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}
