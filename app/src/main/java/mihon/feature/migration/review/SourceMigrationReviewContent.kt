package mihon.feature.migration.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.feature.migration.review.components.SourceMigrationDiscoveryGroupCard
import mihon.feature.migration.review.components.SourceMigrationDiscoveryProgress
import mihon.feature.migration.review.components.SourceMigrationExecutionProgress
import mihon.feature.migration.review.components.SourceMigrationReviewFilters
import mihon.feature.migration.review.components.SourceMigrationReviewGroupCard
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The session's groups as cards under its progress and filters: discovery cards during the search, review after. */
@Composable
internal fun SourceMigrationReviewContent(
    state: SourceMigrationReviewState,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onFilterChange: (SourceMigrationReviewFilter) -> Unit,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onToggleGroup: (groupId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
    onSourceDetailsClick: (entryId: Long) -> Unit,
    onTargetDetailsClick: (entryId: Long) -> Unit,
) {
    val session = requireNotNull(state.session)
    val discoveryActive = session.stage in setOf(
        SourceMigrationSessionStage.DISCOVERY_QUEUED,
        SourceMigrationSessionStage.DISCOVERING,
        SourceMigrationSessionStage.DISCOVERY_PAUSED,
    )
    val executionActive = session.stage in setOf(
        SourceMigrationSessionStage.EXECUTION_QUEUED,
        SourceMigrationSessionStage.EXECUTING,
        SourceMigrationSessionStage.EXECUTION_PAUSED,
    )
    val cardModifier = Modifier.padding(horizontal = KatariTheme.spacing.contentInset)

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        if (discoveryActive) {
            item(key = "discovery-progress") {
                val finished = session.items.count { item ->
                    item.state != SourceMigrationItemState.DISCOVERY_QUEUED &&
                        item.state != SourceMigrationItemState.DISCOVERING
                }
                SourceMigrationDiscoveryProgress(
                    completed = finished,
                    total = session.items.size,
                    paused = session.stage == SourceMigrationSessionStage.DISCOVERY_PAUSED,
                    modifier = cardModifier,
                )
            }
        }

        if (executionActive) {
            item(key = "execution-progress") {
                val finished = session.items.count { item ->
                    item.state == SourceMigrationItemState.APPLIED ||
                        item.state == SourceMigrationItemState.APPLIED_INCOMPLETE ||
                        item.state == SourceMigrationItemState.EXECUTION_FAILED
                }
                SourceMigrationExecutionProgress(
                    completed = finished,
                    total = session.items.count { it.included },
                )
            }
        }

        item(key = "filters") {
            SourceMigrationReviewFilters(
                state = state,
                discoveryActive = discoveryActive,
                onFilterChange = onFilterChange,
            )
        }

        if (discoveryActive) {
            item(key = "replacements-header") {
                SectionHeader(text = stringResource(MR.strings.sourceMigrationReview_replacementsHeader))
            }
        }

        items(
            items = state.visibleGroups,
            key = SourceMigrationReviewGroup::id,
        ) { group ->
            if (discoveryActive) {
                SourceMigrationDiscoveryGroupCard(
                    group = group,
                    onSourceDetailsClick = onSourceDetailsClick,
                    onTargetDetailsClick = onTargetDetailsClick,
                    onTargetClick = onTargetClick,
                    modifier = cardModifier,
                )
            } else {
                SourceMigrationReviewGroupCard(
                    group = group,
                    onIncludedChange = onIncludedChange,
                    onToggleGroup = onToggleGroup,
                    onTargetClick = onTargetClick,
                    onSourceDetailsClick = onSourceDetailsClick,
                    onTargetDetailsClick = onTargetDetailsClick,
                    modifier = cardModifier,
                )
            }
        }

        if (state.visibleGroups.isEmpty()) {
            item(key = "empty-filter") {
                NoResults(message = stringResource(MR.strings.empty_screen))
            }
        }
    }
}
