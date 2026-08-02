package eu.kanade.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun MigrateSourceScreen(
    state: MigrateSourceScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onClickActiveSession: (MigrateSourceScreenModel.ActiveSession) -> Unit,
    onToggleSortingDirection: () -> Unit,
    onToggleSortingMode: () -> Unit,
) {
    val context = LocalContext.current
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.information_empty_library,
            modifier = Modifier.padding(contentPadding),
        )
        else ->
            MigrateSourceList(
                list = state.items,
                activeSessions = state.activeSessions,
                contentPadding = contentPadding,
                onClickItem = onClickItem,
                onClickActiveSession = onClickActiveSession,
                onLongClickItem = { source ->
                    val sourceId = source.id.toString()
                    context.copyToClipboard(sourceId, sourceId)
                },
                sortingMode = state.sortingMode,
                onToggleSortingMode = onToggleSortingMode,
                sortingDirection = state.sortingDirection,
                onToggleSortingDirection = onToggleSortingDirection,
            )
    }
}

@Composable
private fun MigrateSourceList(
    list: List<Pair<Source, Long>>,
    activeSessions: List<MigrateSourceScreenModel.ActiveSession>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onClickActiveSession: (MigrateSourceScreenModel.ActiveSession) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (activeSessions.isNotEmpty()) {
            item(key = "active-migrations-header") {
                Text(
                    text = stringResource(MR.strings.sourceMigration_activeHeader),
                    style = MaterialTheme.typography.header,
                    modifier = Modifier.padding(MaterialTheme.padding.medium),
                )
            }
            items(
                items = activeSessions,
                key = { session -> "active-migration-${session.id.value}" },
            ) { session ->
                ListItem(
                    modifier = Modifier.clickable { onClickActiveSession(session) },
                    supportingContent = {
                        Text(text = sourceMigrationStageLabel(session.stage))
                    },
                    trailingContent = {
                        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                ) {
                    Text(
                        text = session.sourceName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (list.isNotEmpty()) {
            stickyHeader(key = STICKY_HEADER_KEY_PREFIX) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = MaterialTheme.padding.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(MR.strings.migration_selection_prompt),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.header,
                    )

                    IconButton(onClick = onToggleSortingMode) {
                        when (sortingMode) {
                            SetMigrateSorting.Mode.ALPHABETICAL -> Icon(
                                Icons.Outlined.SortByAlpha,
                                contentDescription = stringResource(MR.strings.action_sort_alpha),
                            )
                            SetMigrateSorting.Mode.TOTAL -> Icon(
                                Icons.Outlined.Numbers,
                                contentDescription = stringResource(MR.strings.action_sort_count),
                            )
                        }
                    }
                    IconButton(onClick = onToggleSortingDirection) {
                        when (sortingDirection) {
                            SetMigrateSorting.Direction.ASCENDING -> Icon(
                                Icons.Outlined.ArrowUpward,
                                contentDescription = stringResource(MR.strings.action_asc),
                            )
                            SetMigrateSorting.Direction.DESCENDING -> Icon(
                                Icons.Outlined.ArrowDownward,
                                contentDescription = stringResource(MR.strings.action_desc),
                            )
                        }
                    }
                }
            }

            items(
                items = list,
                key = { (source, _) -> "migrate-${source.id}" },
            ) { (source, count) ->
                MigrateSourceItem(
                    modifier = Modifier.animateItem(),
                    source = source,
                    count = count,
                    onClickItem = { onClickItem(source) },
                    onLongClickItem = { onLongClickItem(source) },
                )
            }
        }
    }
}

@Composable
private fun sourceMigrationStageLabel(stage: SourceMigrationSessionStage): String {
    return stringResource(
        when (stage) {
            SourceMigrationSessionStage.DISCOVERY_QUEUED,
            SourceMigrationSessionStage.DISCOVERING,
            -> MR.strings.sourceMigration_findingReplacements
            SourceMigrationSessionStage.DISCOVERY_PAUSED -> MR.strings.sourceMigration_discoveryPaused
            SourceMigrationSessionStage.REVIEW_REQUIRED -> MR.strings.sourceMigration_readyToReview
            SourceMigrationSessionStage.EXECUTION_QUEUED,
            SourceMigrationSessionStage.EXECUTING,
            -> MR.strings.sourceMigration_migrating
            SourceMigrationSessionStage.EXECUTION_PAUSED -> MR.strings.sourceMigration_executionPaused
            SourceMigrationSessionStage.DRAFT -> MR.strings.sourceMigration_preparing
            SourceMigrationSessionStage.COMPLETED -> MR.strings.completed
            SourceMigrationSessionStage.CANCELLED -> MR.strings.cancelled
            SourceMigrationSessionStage.FAILED -> MR.strings.sourceMigration_failed
        },
    )
}

@Composable
private fun MigrateSourceItem(
    source: Source,
    count: Long,
    onClickItem: () -> Unit,
    onLongClickItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = source.lang != "",
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { SourceIcon(source = source) },
        action = {
            BadgeGroup {
                Badge(text = "$count")
            }
        },
        content = { _, sourceLangString ->
            Column(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .weight(1f),
            ) {
                Text(
                    text = source.name.ifBlank { source.id.toString() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sourceLangString != null) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = sourceLangString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (source.isStub) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = stringResource(MR.strings.not_installed),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}
