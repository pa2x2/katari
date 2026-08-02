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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
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
import mihon.feature.migration.review.components.SourceMigrationReviewGroupCard
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class SourceMigrationReviewScreen(
    private val sessionId: SourceMigrationSessionId,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourceMigrationReviewScreenModel(sessionId) }
        val state by screenModel.state.collectAsState()
        val session = state.session
        var candidateItemId by rememberSaveable { mutableStateOf<Long?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.sourceMigrationReview_title),
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
                if (session?.stage == SourceMigrationSessionStage.REVIEW_REQUIRED &&
                    session.includedReadyCount > 0
                ) {
                    SmallExtendedFloatingActionButton(
                        text = {
                            Text(
                                stringResource(
                                    MR.strings.sourceMigrationReview_migrateCount,
                                    session.includedReadyCount,
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
                    )
                }
            },
        ) { contentPadding ->
            when {
                !state.isLoaded -> LoadingScreen()
                session == null -> EmptyScreen(MR.strings.internal_error)
                else -> SourceMigrationReviewContent(
                    state = state,
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
                SourceMigrationProgress(
                    label = stringResource(
                        MR.strings.sourceMigrationReview_findingProgress,
                        finished,
                        session.items.size,
                    ),
                    completed = finished,
                    total = session.items.size,
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

        if (!discoveryActive) {
            item(key = "filters") {
                SourceMigrationReviewFilters(
                    state = state,
                    onFilterChange = onFilterChange,
                )
            }
        }

        if (!discoveryActive) {
            items(
                items = state.visibleGroups,
                key = SourceMigrationReviewGroup::id,
            ) { group ->
                SourceMigrationReviewGroupCard(
                    group = group,
                    onIncludedChange = onIncludedChange,
                    onToggleGroup = onToggleGroup,
                    onTargetClick = onTargetClick,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }
        }

        if (!discoveryActive && state.visibleGroups.isEmpty()) {
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
        FilterChip(
            selected = state.filter == SourceMigrationReviewFilter.ALL,
            onClick = { onFilterChange(SourceMigrationReviewFilter.ALL) },
            label = {
                Text(
                    stringResource(
                        MR.strings.sourceMigrationReview_filterAll,
                        state.session?.items?.size ?: 0,
                    ),
                )
            },
        )
        FilterChip(
            selected = state.filter == SourceMigrationReviewFilter.NEEDS_REVIEW,
            onClick = { onFilterChange(SourceMigrationReviewFilter.NEEDS_REVIEW) },
            label = {
                Text(stringResource(MR.strings.sourceMigrationReview_filterNeedsReview, state.needsReviewCount))
            },
        )
        FilterChip(
            selected = state.filter == SourceMigrationReviewFilter.NO_MATCH,
            onClick = { onFilterChange(SourceMigrationReviewFilter.NO_MATCH) },
            label = {
                Text(stringResource(MR.strings.sourceMigrationReview_filterNoMatch, state.noMatchCount))
            },
        )
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
