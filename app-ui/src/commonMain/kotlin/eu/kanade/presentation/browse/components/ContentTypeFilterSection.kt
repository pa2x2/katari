package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.surface.Pill
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
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
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
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
    onClick: () -> Unit,
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
            FilterSummaryPill(stringResource(MR.strings.browse_filter_all_content_types), onClick)
        } else {
            EntryType.entries
                .filter(filter.entryTypes::contains)
                .forEach { entryType ->
                    FilterSummaryPill(stringResource(entryType.entryTypePresentation().displayNameLabel), onClick)
                }
            if (filter.includeUnspecified) {
                FilterSummaryPill(stringResource(MR.strings.browse_filter_not_specified), onClick)
            }
        }
        trailingContent()
    }
}

/**
 * One part of a filter summary, such as a content type or "3 languages"; every part of the summary looks alike and
 * opens the filter with [onClick].
 */
@Composable
fun FilterSummaryPill(text: String, onClick: () -> Unit) {
    Pill(text = text, container = SurfaceContainer.SecondaryContainer, role = TextRole.Label, onClick = onClick)
}
