package mihon.feature.migration.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.feature.migration.list.components.MigrationListItem
import mihon.feature.migration.list.components.MigrationListItemAction
import mihon.feature.migration.list.components.MigrationListItemResult
import mihon.feature.migration.list.models.MigratingEntry
import mihon.feature.migration.list.models.MigrationMergeContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.CopyAll
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
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
        val rows = remember(items) { migrationListRows(items) }
        val positions = remember(rows) { RowGroupPosition.ofRuns(rows) { it is MigrationListRow.Entry } }
        RowList(fastScroll = true, contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s)) {
            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                when (row) {
                    is MigrationListRow.Gap -> RowGroupGap()
                    is MigrationListRow.MergeHeader -> RowGroupHeader(
                        text = row.context.rootTitle,
                        modifier = Modifier.animateItem(),
                        trailing = {
                            Pill(text = stringResource(MR.strings.merge_members_count, row.context.memberCount))
                        },
                    )
                    is MigrationListRow.Entry -> RowGroupItem(
                        position = requireNotNull(positions[index]),
                        modifier = Modifier.animateItem(),
                    ) {
                        MigrationListEntryRow(
                            item = row.item,
                            onItemClick = onItemClick,
                            onSearchManually = onSearchManually,
                            onSkip = onSkip,
                            onMigrate = onMigrate,
                            onCopy = onCopy,
                        )
                    }
                }
            }
        }
    }
}

/** An entry beside the match found for it, with what can be done with the pair. */
@Composable
private fun MigrationListEntryRow(
    item: MigratingEntry,
    onItemClick: (Entry, MigrationMergeContext?) -> Unit,
    onSearchManually: (MigratingEntry) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = KatariTheme.spacing.s,
                end = KatariTheme.spacing.xs,
                top = KatariTheme.spacing.s,
                bottom = KatariTheme.spacing.s,
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

/**
 * The list laid out in row group cards: entries that stand alone share a card, and the members of a merged entry
 * get a card of their own under its title.
 */
private sealed interface MigrationListRow {
    val key: Any

    data class Entry(val item: MigratingEntry) : MigrationListRow {
        override val key: Any = item.entry.id
    }

    data class MergeHeader(val context: MigrationMergeContext) : MigrationListRow {
        override val key: Any = "merge-${context.rootEntryId}"
    }

    data class Gap(override val key: Any) : MigrationListRow
}

private fun migrationListRows(items: List<MigratingEntry>): List<MigrationListRow> = buildList {
    var previousMerged = false
    items.groupBy { it.mergeContext?.rootEntryId ?: -it.entry.id }.values.forEach { group ->
        val mergeContext = group.first().mergeContext
        if (isNotEmpty() && (mergeContext != null || previousMerged)) {
            add(MigrationListRow.Gap("gap-${group.first().entry.id}"))
        }
        mergeContext?.let { add(MigrationListRow.MergeHeader(it)) }
        group.forEach { add(MigrationListRow.Entry(it)) }
        previousMerged = mergeContext != null
    }
}
