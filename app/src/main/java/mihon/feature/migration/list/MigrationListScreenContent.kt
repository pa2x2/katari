package mihon.feature.migration.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.feature.migration.list.components.MigrationListItem
import mihon.feature.migration.list.components.MigrationListItemAction
import mihon.feature.migration.list.components.MigrationListItemResult
import mihon.feature.migration.list.components.MigrationProposalGroupHeader
import mihon.feature.migration.list.models.MigratingEntry
import mihon.feature.migration.list.models.MigrationMergeContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.CopyAll
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun MigrationListScreenContent(
    items: List<MigratingEntry>,
    migrationComplete: Boolean,
    finishedCount: Int,
    onItemClick: (Entry, MigrationMergeContext?) -> Unit,
    onSearchManually: (MigratingEntry) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
    openMigrationDialog: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (items.isNotEmpty()) {
                    stringResource(MR.strings.migrationListScreenTitleWithProgress, finishedCount, items.size)
                } else {
                    stringResource(MR.strings.migrationListScreenTitle)
                },
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_copyActionLabel),
                                icon = if (items.size == 1) {
                                    MaterialSymbols.Rounded.ContentCopy
                                } else {
                                    MaterialSymbols.Rounded.CopyAll
                                },
                                onClick = { openMigrationDialog(true) },
                                enabled = migrationComplete,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_migrateActionLabel),
                                icon = if (items.size == 1) {
                                    MaterialSymbols.Rounded.Check
                                } else {
                                    MaterialSymbols.Rounded.DoneAll
                                },
                                onClick = { openMigrationDialog(false) },
                                enabled = migrationComplete,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        FastScrollLazyColumn(contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s)) {
            val groupedItems = items.groupBy { item ->
                item.mergeContext?.rootEntryId ?: -item.entry.id
            }
            groupedItems.values.forEach { groupItems ->
                groupItems.first().mergeContext?.let { mergeContext ->
                    item(key = "merge-${mergeContext.rootEntryId}") {
                        MigrationProposalGroupHeader(mergeContext)
                    }
                }
                items(groupItems, key = { it.entry.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(
                                start = KatariTheme.spacing.l,
                                end = KatariTheme.spacing.s,
                            )
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MigrationListItem(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.Top)
                                .fillMaxHeight(),
                            entry = item.entry,
                            source = item.source,
                            chapterCount = item.chapterCount,
                            latestChapter = item.latestChapter,
                            mergeContext = item.mergeContext,
                            onClick = { onItemClick(item.entry, item.mergeContext) },
                        )

                        Icon(
                            imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.weight(0.2f),
                        )

                        val result by item.searchResult.collectAsState()
                        MigrationListItemResult(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.Top)
                                .fillMaxHeight(),
                            result = result,
                            onItemClick = onItemClick,
                        )

                        MigrationListItemAction(
                            modifier = Modifier.weight(0.2f),
                            result = result,
                            onSearchManually = { onSearchManually(item) },
                            onSkip = { onSkip(item.entry.id) },
                            onMigrate = { onMigrate(item.entry.id) },
                            onCopy = { onCopy(item.entry.id) },
                        )
                    }
                }
            }
        }
    }
}
