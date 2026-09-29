package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An on/off or include/exclude option drawn as a chip.
 *
 * Tapping a tri-state option cycles not used → included → excluded; long-pressing excludes it directly. Excluded
 * options use the error colors and a struck-through label so they never read as included.
 */
@Composable
internal fun FilterOptionChip(filter: EntryFilter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is EntryFilter.CheckBox -> FilterOptionChip(
            label = filter.name,
            state = if (filter.state) FilterOptionState.Included else FilterOptionState.NotUsed,
            onClick = {
                filter.state = !filter.state
                onUpdate()
            },
            onLongClick = null,
        )
        is EntryFilter.TriState -> FilterOptionChip(
            label = filter.name,
            state = filter.optionState,
            onClick = {
                filter.state = filter.optionState.next().triStateValue
                onUpdate()
            },
            onLongClick = {
                filter.state = EntryFilter.TriState.STATE_EXCLUDE
                onUpdate()
            },
        )
        else -> Unit
    }
}

@Composable
private fun FilterOptionChip(
    label: String,
    state: FilterOptionState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val description = state.description()
    Surface(
        shape = FilterChipDefaults.shape,
        color = when (state) {
            FilterOptionState.NotUsed -> Color.Transparent
            FilterOptionState.Included -> colors.secondaryContainer
            FilterOptionState.Excluded -> colors.errorContainer
        },
        contentColor = when (state) {
            FilterOptionState.NotUsed -> colors.onSurfaceVariant
            FilterOptionState.Included -> colors.onSecondaryContainer
            FilterOptionState.Excluded -> colors.onErrorContainer
        },
        border = if (state == FilterOptionState.NotUsed) BorderStroke(1.dp, colors.outline) else null,
        modifier = Modifier.minimumInteractiveComponentSize(),
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Checkbox,
                    onLongClickLabel = onLongClick?.let { stringResource(MR.strings.filter_exclude) },
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .semantics {
                    stateDescription = description
                    toggleableState = when (state) {
                        FilterOptionState.NotUsed -> ToggleableState.Off
                        FilterOptionState.Included -> ToggleableState.On
                        FilterOptionState.Excluded -> ToggleableState.Indeterminate
                    }
                }
                .heightIn(min = FilterChipDefaults.Height)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state) {
                FilterOptionState.NotUsed -> Unit
                FilterOptionState.Included -> Icon(
                    Icons.Outlined.Check,
                    null,
                    Modifier.size(FilterChipDefaults.IconSize),
                )
                FilterOptionState.Excluded -> Icon(
                    Icons.Outlined.Remove,
                    null,
                    Modifier.size(FilterChipDefaults.IconSize),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textDecoration = if (state == FilterOptionState.Excluded) TextDecoration.LineThrough else null,
            )
        }
    }
}
