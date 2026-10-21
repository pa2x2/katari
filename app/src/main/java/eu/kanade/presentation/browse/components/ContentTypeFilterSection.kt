package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ContentTypeFilterSection(
    filter: ContentTypeFilter,
    onShowAll: () -> Unit,
    onToggleContentType: (EntryType) -> Unit,
    onToggleUnspecified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FilterSectionHeading(
            title = stringResource(MR.strings.browse_filter_content_types),
            summary = stringResource(MR.strings.browse_filter_content_types_summary),
        )
        FlowRow(
            modifier = Modifier.padding(
                start = KatariTheme.spacing.rowInset,
                end = KatariTheme.spacing.rowInset,
                top = KatariTheme.spacing.s,
                bottom = KatariTheme.spacing.s,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            FilterChip(
                label = stringResource(MR.strings.all),
                selected = !filter.isActive,
                onClick = onShowAll,
            )
            EntryType.entries.forEach { entryType ->
                FilterChip(
                    label = stringResource(entryType.entryTypePresentation().displayNameLabel),
                    selected = entryType in filter.entryTypes,
                    onClick = { onToggleContentType(entryType) },
                )
            }
            FilterChip(
                label = stringResource(MR.strings.browse_filter_not_specified),
                selected = filter.includeUnspecified,
                onClick = onToggleUnspecified,
            )
        }
    }
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
                horizontal = KatariTheme.spacing.rowInset,
                vertical = KatariTheme.spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        if (!filter.isActive) {
            FilterSummaryPill(stringResource(MR.strings.browse_filter_all_content_types))
        } else {
            EntryType.entries
                .filter(filter.entryTypes::contains)
                .forEach { entryType ->
                    FilterSummaryPill(stringResource(entryType.entryTypePresentation().displayNameLabel))
                }
            if (filter.includeUnspecified) {
                FilterSummaryPill(stringResource(MR.strings.browse_filter_not_specified))
            }
        }
        trailingContent()
    }
}

/** One part of a filter summary, such as a content type or "3 languages"; every part of the summary looks alike. */
@Composable
internal fun FilterSummaryPill(text: String) {
    Pill(text = text, container = SurfaceContainer.SecondaryContainer, role = TextRole.Label)
}
