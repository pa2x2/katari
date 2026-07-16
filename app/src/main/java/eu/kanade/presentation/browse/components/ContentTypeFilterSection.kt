package eu.kanade.presentation.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ContentTypeFilterSection(
    filter: ContentTypeFilter,
    onShowAll: () -> Unit,
    onToggleContentType: (EntryType) -> Unit,
    onToggleUnspecified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium,
        ),
    ) {
        Text(
            text = stringResource(MR.strings.browse_filter_content_types),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(MR.strings.browse_filter_content_types_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall),
        )
        FlowRow(
            modifier = Modifier.padding(top = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            FilterChip(
                selected = !filter.isActive,
                onClick = onShowAll,
                label = { Text(stringResource(MR.strings.all)) },
            )
            EntryType.entries.forEach { entryType ->
                ContentTypeFilterChip(
                    label = stringResource(entryType.entryTypePresentation().displayNameLabel),
                    selected = entryType in filter.entryTypes,
                    onClick = { onToggleContentType(entryType) },
                )
            }
            ContentTypeFilterChip(
                label = stringResource(MR.strings.browse_filter_not_specified),
                selected = filter.includeUnspecified,
                onClick = onToggleUnspecified,
            )
        }
    }
}

@Composable
private fun ContentTypeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = selected.takeIf { it }?.let {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
fun ContentTypeFilterSummary(
    filter: ContentTypeFilter,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        if (!filter.isActive) {
            ContentTypeFilterSummaryChip(stringResource(MR.strings.browse_filter_all_content_types))
        } else {
            EntryType.entries
                .filter(filter.entryTypes::contains)
                .forEach { entryType ->
                    ContentTypeFilterSummaryChip(
                        stringResource(entryType.entryTypePresentation().displayNameLabel),
                    )
                }
            if (filter.includeUnspecified) {
                ContentTypeFilterSummaryChip(stringResource(MR.strings.browse_filter_not_specified))
            }
        }
        trailingContent()
    }
}

@Composable
private fun ContentTypeFilterSummaryChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.padding.small,
                vertical = MaterialTheme.padding.extraSmall,
            ),
        )
    }
}
