package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceViewModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.Numbers
import mihon.icons.materialsymbols.rounded.SortByAlpha
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun MigrateSourceScreen(
    state: MigrateSourceViewModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onClickActiveSession: (MigrateSourceViewModel.ActiveSession) -> Unit,
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
    activeSessions: List<MigrateSourceViewModel.ActiveSession>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onClickActiveSession: (MigrateSourceViewModel.ActiveSession) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s),
    ) {
        if (activeSessions.isNotEmpty()) {
            item(key = "active-migrations-header") {
                SectionHeader(stringResource(MR.strings.sourceMigration_activeHeader))
            }
            items(
                items = activeSessions,
                key = { session -> "active-migration-${session.id.value}" },
            ) { session ->
                SettingsRow(
                    title = session.sourceName,
                    subtitle = sourceMigrationStageLabel(session.stage),
                    onClick = { onClickActiveSession(session) },
                )
            }
        }

        if (list.isNotEmpty()) {
            stickyHeader(key = STICKY_HEADER_KEY_PREFIX) {
                Surface(container = SurfaceContainer.Background) {
                    SectionHeader(
                        text = stringResource(MR.strings.migration_selection_prompt),
                        actions = {
                            when (sortingMode) {
                                SetMigrateSorting.Mode.ALPHABETICAL -> IconButton(
                                    icon = MaterialSymbols.Rounded.SortByAlpha,
                                    contentDescription = stringResource(MR.strings.action_sort_alpha),
                                    onClick = onToggleSortingMode,
                                )
                                SetMigrateSorting.Mode.TOTAL -> IconButton(
                                    icon = MaterialSymbols.Rounded.Numbers,
                                    contentDescription = stringResource(MR.strings.action_sort_count),
                                    onClick = onToggleSortingMode,
                                )
                            }
                            when (sortingDirection) {
                                SetMigrateSorting.Direction.ASCENDING -> IconButton(
                                    icon = MaterialSymbols.Rounded.ArrowUpward,
                                    contentDescription = stringResource(MR.strings.action_asc),
                                    onClick = onToggleSortingDirection,
                                )
                                SetMigrateSorting.Direction.DESCENDING -> IconButton(
                                    icon = MaterialSymbols.Rounded.ArrowDownward,
                                    contentDescription = stringResource(MR.strings.action_desc),
                                    onClick = onToggleSortingDirection,
                                )
                            }
                        },
                    )
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
        title = source.name.ifBlank { source.id.toString() },
        showLanguageInContent = source.lang != "",
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        below = if (source.isStub) {
            {
                Text(
                    text = stringResource(MR.strings.not_installed),
                    role = TextRole.Supporting,
                    tone = ContentTone.Error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            null
        },
        action = {
            BadgeGroup {
                Badge(text = "$count")
            }
        },
    )
}
