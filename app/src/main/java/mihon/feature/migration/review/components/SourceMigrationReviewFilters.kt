package mihon.feature.migration.review.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.feature.migration.review.SourceMigrationReviewFilter
import mihon.feature.migration.review.SourceMigrationReviewState
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The filter chips over the session's groups, each with its count. While replacements are still searched for, the
 * chips follow the search (found, searching); afterwards they follow the review (needs review).
 */
@Composable
internal fun SourceMigrationReviewFilters(
    state: SourceMigrationReviewState,
    discoveryActive: Boolean,
    onFilterChange: (SourceMigrationReviewFilter) -> Unit,
) {
    val filters = if (discoveryActive) DISCOVERY_FILTERS else REVIEW_FILTERS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        filters.forEach { filter ->
            FilterChip(
                label = filterLabel(filter, state),
                selected = state.filter == filter,
                onClick = { onFilterChange(filter) },
            )
        }
    }
}

@Composable
private fun filterLabel(
    filter: SourceMigrationReviewFilter,
    state: SourceMigrationReviewState,
): String = when (filter) {
    SourceMigrationReviewFilter.ALL -> stringResource(
        MR.strings.sourceMigrationReview_filterAll,
        state.session?.items?.size ?: 0,
    )
    SourceMigrationReviewFilter.READY -> stringResource(MR.strings.sourceMigrationReview_filterReady, state.readyCount)
    SourceMigrationReviewFilter.FOUND -> stringResource(MR.strings.sourceMigrationReview_filterFound, state.foundCount)
    SourceMigrationReviewFilter.SEARCHING -> stringResource(
        MR.strings.sourceMigrationReview_filterSearching,
        state.searchingCount,
    )
    SourceMigrationReviewFilter.NEEDS_REVIEW -> stringResource(
        MR.strings.sourceMigrationReview_filterNeedsReview,
        state.needsReviewCount,
    )
    SourceMigrationReviewFilter.NO_MATCH -> stringResource(
        MR.strings.sourceMigrationReview_filterNoMatch,
        state.noMatchCount,
    )
}

private val DISCOVERY_FILTERS = listOf(
    SourceMigrationReviewFilter.ALL,
    SourceMigrationReviewFilter.READY,
    SourceMigrationReviewFilter.FOUND,
    SourceMigrationReviewFilter.SEARCHING,
    SourceMigrationReviewFilter.NO_MATCH,
)

private val REVIEW_FILTERS = listOf(
    SourceMigrationReviewFilter.ALL,
    SourceMigrationReviewFilter.READY,
    SourceMigrationReviewFilter.NEEDS_REVIEW,
    SourceMigrationReviewFilter.NO_MATCH,
)
