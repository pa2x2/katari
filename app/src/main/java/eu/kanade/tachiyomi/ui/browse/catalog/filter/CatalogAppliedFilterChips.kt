package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeKind
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeLabel
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.description
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.displayText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The applied filter values of the current search, each removable on its own.
 *
 * Tapping a chip opens the filter sheet; its close icon resets that value and re-runs the search.
 */
@Composable
internal fun CatalogAppliedFilterChips(
    labels: List<FilterChangeLabel>,
    onOpenFilters: () -> Unit,
    onRemove: (FilterChangeLabel) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        labels.forEach { label -> AppliedFilterChip(label, onOpenFilters, onRemove) }
    }
}

@Composable
private fun AppliedFilterChip(
    label: FilterChangeLabel,
    onOpenFilters: () -> Unit,
    onRemove: (FilterChangeLabel) -> Unit,
) {
    val excluded = label.kind == FilterChangeKind.Excluded
    val description = label.description()
    val removeDescription = stringResource(MR.strings.action_remove)
    InputChip(
        selected = true,
        onClick = onOpenFilters,
        modifier = Modifier.semantics { contentDescription = description },
        label = {
            Text(
                text = label.displayText(),
                textDecoration = if (excluded) TextDecoration.LineThrough else null,
            )
        },
        leadingIcon = when (label.kind) {
            FilterChangeKind.Included -> Icons.Outlined.Check
            FilterChangeKind.Excluded -> Icons.Outlined.Remove
            FilterChangeKind.Ascending -> Icons.Outlined.ArrowUpward
            FilterChangeKind.Descending -> Icons.Outlined.ArrowDownward
            FilterChangeKind.Value, FilterChangeKind.SelectedCount -> null
        }?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(InputChipDefaults.IconSize)) }
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = removeDescription,
                modifier = Modifier
                    .size(InputChipDefaults.IconSize)
                    .clip(CircleShape)
                    .clickable { onRemove(label) },
            )
        },
        colors = if (excluded) {
            InputChipDefaults.inputChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            InputChipDefaults.inputChipColors()
        },
    )
}
