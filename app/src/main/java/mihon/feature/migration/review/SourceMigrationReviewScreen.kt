package mihon.feature.migration.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import mihon.feature.migration.review.components.SourceMigrationDiscoveryGroupCard
import mihon.feature.migration.review.components.SourceMigrationReviewGroupCard
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.shouldExpandFAB

class SourceMigrationReviewScreen(
    private val sessionId: SourceMigrationSessionId,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourceMigrationReviewScreenModel(sessionId) }
        val state by screenModel.state.collectAsState()
        val session = state.session
        val discoveryActive = session?.stage in DISCOVERY_STAGES
        val lazyListState = rememberLazyListState()
        var candidateItemId by rememberSaveable { mutableStateOf<Long?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(
                        if (discoveryActive) {
                            MR.strings.sourceMigration_findingReplacements
                        } else {
                            MR.strings.sourceMigrationReview_title
                        },
                    ),
                    subtitle = session?.let {
                        stringResource(
                            MR.strings.sourceMigrationReview_subtitle,
                            state.originSourceName,
                            it.items.size,
                        )
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        when (session?.stage) {
                            SourceMigrationSessionStage.DISCOVERY_QUEUED,
                            SourceMigrationSessionStage.DISCOVERING,
                            -> IconButton(onClick = screenModel::pauseDiscovery) {
                                Icon(
                                    imageVector = Icons.Outlined.Pause,
                                    contentDescription = stringResource(MR.strings.action_pause),
                                )
                            }
                            SourceMigrationSessionStage.DISCOVERY_PAUSED -> {
                                IconButton(onClick = screenModel::resumeDiscovery) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(MR.strings.action_resume),
                                    )
                                }
                            }
                            SourceMigrationSessionStage.EXECUTION_QUEUED,
                            SourceMigrationSessionStage.EXECUTING,
                            -> IconButton(onClick = screenModel::pauseExecution) {
                                Icon(
                                    imageVector = Icons.Outlined.Pause,
                                    contentDescription = stringResource(MR.strings.action_pause),
                                )
                            }
                            SourceMigrationSessionStage.EXECUTION_PAUSED -> {
                                IconButton(onClick = screenModel::resumeExecution) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(MR.strings.action_resume),
                                    )
                                }
                            }
                            else -> Unit
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = {
                        Text(
                            stringResource(
                                MR.strings.sourceMigrationReview_migrateCount,
                                session?.includedReadyCount ?: 0,
                            ),
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                        )
                    },
                    onClick = screenModel::startExecution,
                    expanded = lazyListState.shouldExpandFAB(),
                    modifier = Modifier.animateFloatingActionButton(
                        visible = session?.stage == SourceMigrationSessionStage.REVIEW_REQUIRED &&
                            session.includedReadyCount > 0,
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
        ) { contentPadding ->
            when {
                !state.isLoaded -> LoadingScreen()
                session == null -> EmptyScreen(MR.strings.internal_error)
                else -> SourceMigrationReviewContent(
                    state = state,
                    lazyListState = lazyListState,
                    contentPadding = contentPadding,
                    onFilterChange = screenModel::setFilter,
                    onIncludedChange = screenModel::setIncluded,
                    onToggleGroup = screenModel::toggleGroup,
                    onTargetClick = { sourceEntryId -> candidateItemId = sourceEntryId },
                )
            }
        }

        val candidateItem = session?.items?.firstOrNull { item -> item.sourceEntryId == candidateItemId }
        candidateItem?.let { item ->
            SourceMigrationCandidateSheet(
                sessionId = sessionId,
                item = item,
                onDismissRequest = { candidateItemId = null },
            )
        }
    }
}

@Composable
private fun SourceMigrationReviewContent(
    state: SourceMigrationReviewState,
    lazyListState: LazyListState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onFilterChange: (SourceMigrationReviewFilter) -> Unit,
    onIncludedChange: (sourceEntryId: Long, included: Boolean) -> Unit,
    onToggleGroup: (groupId: Long) -> Unit,
    onTargetClick: (sourceEntryId: Long) -> Unit,
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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
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
                SourceMigrationProgress(
                    label = stringResource(
                        MR.strings.sourceMigrationReview_migrationProgress,
                        finished,
                        session.items.count { it.included },
                    ),
                    completed = finished,
                    total = session.items.count { it.included },
                )
            }
        }

        item(key = "filters") {
            if (discoveryActive) {
                SourceMigrationDiscoveryFilters(
                    state = state,
                    onFilterChange = onFilterChange,
                )
            } else {
                SourceMigrationReviewFilters(
                    state = state,
                    onFilterChange = onFilterChange,
                )
            }
        }

        if (discoveryActive) {
            item(key = "replacements-header") {
                ListGroupHeader(
                    text = stringResource(MR.strings.sourceMigrationReview_replacementsHeader),
                )
            }
        }

        items(
            items = state.visibleGroups,
            key = SourceMigrationReviewGroup::id,
        ) { group ->
            if (discoveryActive) {
                SourceMigrationDiscoveryGroupCard(
                    group = group,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            } else {
                SourceMigrationReviewGroupCard(
                    group = group,
                    onIncludedChange = onIncludedChange,
                    onToggleGroup = onToggleGroup,
                    onTargetClick = onTargetClick,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }
        }

        if (state.visibleGroups.isEmpty()) {
            item(key = "empty-filter") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.padding.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(MR.strings.empty_screen),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceMigrationDiscoveryFilters(
    state: SourceMigrationReviewState,
    onFilterChange: (SourceMigrationReviewFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.ALL,
            onClick = { onFilterChange(SourceMigrationReviewFilter.ALL) },
            label = stringResource(
                MR.strings.sourceMigrationReview_filterAll,
                state.session?.items?.size ?: 0,
            ),
        )
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.FOUND,
            onClick = { onFilterChange(SourceMigrationReviewFilter.FOUND) },
            label = stringResource(MR.strings.sourceMigrationReview_filterFound, state.foundCount),
        )
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.SEARCHING,
            onClick = { onFilterChange(SourceMigrationReviewFilter.SEARCHING) },
            label = stringResource(MR.strings.sourceMigrationReview_filterSearching, state.searchingCount),
        )
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.NO_MATCH,
            onClick = { onFilterChange(SourceMigrationReviewFilter.NO_MATCH) },
            label = stringResource(MR.strings.sourceMigrationReview_filterNoMatch, state.noMatchCount),
        )
    }
}

@Composable
private fun SourceMigrationReviewFilters(
    state: SourceMigrationReviewState,
    onFilterChange: (SourceMigrationReviewFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.ALL,
            onClick = { onFilterChange(SourceMigrationReviewFilter.ALL) },
            label = stringResource(
                MR.strings.sourceMigrationReview_filterAll,
                state.session?.items?.size ?: 0,
            ),
        )
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.NEEDS_REVIEW,
            onClick = { onFilterChange(SourceMigrationReviewFilter.NEEDS_REVIEW) },
            label = stringResource(MR.strings.sourceMigrationReview_filterNeedsReview, state.needsReviewCount),
        )
        SourceMigrationFilterChip(
            selected = state.filter == SourceMigrationReviewFilter.NO_MATCH,
            onClick = { onFilterChange(SourceMigrationReviewFilter.NO_MATCH) },
            label = stringResource(MR.strings.sourceMigrationReview_filterNoMatch, state.noMatchCount),
        )
    }
}

@Composable
private fun SourceMigrationFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun SourceMigrationDiscoveryProgress(
    completed: Int,
    total: Int,
    paused: Boolean,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(MR.strings.sourceMigration_findingReplacements),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            MR.strings.sourceMigrationReview_discoveryChecked,
                            completed,
                            total,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val statusColor = if (paused) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                }
                BadgeGroup {
                    Badge(
                        text = stringResource(
                            if (paused) {
                                MR.strings.sourceMigration_discoveryPaused
                            } else {
                                MR.strings.sourceMigrationReview_running
                            },
                        ),
                        color = statusColor,
                        textColor = contentColorFor(statusColor),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else completed.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(MR.strings.sourceMigrationReview_discoveryBackground),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SourceMigrationProgress(
    label: String,
    completed: Int,
    total: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completed.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val DISCOVERY_STAGES = setOf(
    SourceMigrationSessionStage.DISCOVERY_QUEUED,
    SourceMigrationSessionStage.DISCOVERING,
    SourceMigrationSessionStage.DISCOVERY_PAUSED,
)
