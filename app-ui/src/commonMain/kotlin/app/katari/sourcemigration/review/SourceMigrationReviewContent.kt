package app.katari.sourcemigration.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.sourcemigration.review.components.SourceMigrationDiscoveryMapping
import app.katari.sourcemigration.review.components.SourceMigrationDiscoveryMergedGroup
import app.katari.sourcemigration.review.components.SourceMigrationDiscoveryProgress
import app.katari.sourcemigration.review.components.SourceMigrationExecutionProgress
import app.katari.sourcemigration.review.components.SourceMigrationReviewFilters
import app.katari.sourcemigration.review.components.SourceMigrationReviewMapping
import app.katari.sourcemigration.review.components.SourceMigrationReviewMergedGroup
import app.katari.sourcemigration.session.model.SourceMigrationItemState
import app.katari.sourcemigration.session.model.SourceMigrationSessionStage
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The session's groups in cards under its progress and filters, laid out for the search while it runs and for review
 * once it has finished.
 */
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
    val positions = remember(state.visibleGroups) {
        RowGroupPosition.ofRuns(state.visibleGroups) { group -> group.memberCount == 1 }
    }

    RowList(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
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
                RowGroupHeader(text = stringResource(MR.strings.sourceMigrationReview_replacementsHeader))
            }
        } else {
            item(key = "groups-gap") { RowGroupGap() }
        }

        // Standalone entries share a card; a merged entry's members get a card of their own under its title.
        itemsIndexed(
            items = state.visibleGroups,
            key = { _, group -> group.id },
        ) { index, group ->
            val position = positions[index]
            Column(Modifier.animateItem()) {
                if (index > 0 && position != RowGroupPosition.Middle && position != RowGroupPosition.Last) {
                    RowGroupGap()
                }
                if (position == null) {
                    if (discoveryActive) {
                        SourceMigrationDiscoveryMergedGroup(
                            group = group,
                            onSourceDetailsClick = onSourceDetailsClick,
                            onTargetDetailsClick = onTargetDetailsClick,
                            onTargetClick = onTargetClick,
                        )
                    } else {
                        SourceMigrationReviewMergedGroup(
                            group = group,
                            onIncludedChange = onIncludedChange,
                            onToggleGroup = onToggleGroup,
                            onTargetClick = onTargetClick,
                            onSourceDetailsClick = onSourceDetailsClick,
                            onTargetDetailsClick = onTargetDetailsClick,
                        )
                    }
                } else {
                    RowGroupItem(position = position) {
                        Column {
                            group.mappings.forEach { mapping ->
                                if (discoveryActive) {
                                    SourceMigrationDiscoveryMapping(
                                        mapping = mapping,
                                        sourceRole = stringResource(MR.strings.sourceMigrationReview_current),
                                        targetRole = null,
                                        onSourceDetailsClick = onSourceDetailsClick,
                                        onTargetDetailsClick = onTargetDetailsClick,
                                        onTargetClick = onTargetClick,
                                    )
                                } else {
                                    SourceMigrationReviewMapping(
                                        mapping = mapping,
                                        onIncludedChange = onIncludedChange,
                                        onTargetClick = onTargetClick,
                                        onSourceDetailsClick = onSourceDetailsClick,
                                        onTargetDetailsClick = onTargetDetailsClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.visibleGroups.isEmpty()) {
            item(key = "empty-filter") {
                NoResults(message = stringResource(MR.strings.empty_screen))
            }
        }
    }
}
