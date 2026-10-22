package eu.kanade.tachiyomi.ui.browse.feed.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.Edit
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.control.Switch
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

/** Reorders, enables, renames and removes feeds. A removal can be undone from the snackbar it shows. */
@Composable
internal fun ManageFeedsSheet(
    feeds: List<SourceFeed>,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    onReorder: (fromFeedId: String, toFeedId: String) -> Unit,
    onToggle: (feedId: String, enabled: Boolean) -> Unit,
    onRename: (SourceFeed) -> Unit,
    onRemove: (feedId: String) -> BrowseFeedService.RemovedFeed?,
    onRestore: (BrowseFeedService.RemovedFeed) -> Unit,
    onAddFeed: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val removedMessage = stringResource(MR.strings.browse_feed_removed)
    val undoLabel = stringResource(MR.strings.action_undo)
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        listState,
        PaddingValues(vertical = KatariTheme.spacing.s),
    ) { from, to ->
        onReorder(from.key as String, to.key as String)
    }

    val removeFeed: (String) -> Unit = { feedId ->
        onRemove(feedId)?.let { removed ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = removedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) onRestore(removed)
            }
        }
    }

    // Feeds whose source or preset is gone are left out until they come back.
    val shownFeeds = feeds.mapNotNull { feed ->
        val source = sourceFor(feed.sourceId) ?: return@mapNotNull null
        val preset = presetFor(feed) ?: return@mapNotNull null
        ShownFeed(feed, source, preset)
    }

    Sheet(onDismissRequest = onDismissRequest, snackbarHostState = snackbarHostState) {
        Column {
            SheetHeader(title = stringResource(MR.strings.browse_manage_feeds))
            ScrollbarLazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(vertical = KatariTheme.spacing.s),
            ) {
                itemsIndexed(items = shownFeeds, key = { _, shown -> shown.feed.id }) { index, shown ->
                    ReorderableItem(state = reorderableState, key = shown.feed.id) { isDragging ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, shownFeeds.size),
                            lifted = isDragging,
                        ) {
                            ManageFeedRow(
                                feed = shown.feed,
                                source = shown.source,
                                preset = shown.preset,
                                onToggle = { onToggle(shown.feed.id, it) },
                                onRename = { onRename(shown.feed) },
                                onRemove = { removeFeed(shown.feed.id) },
                            )
                        }
                    }
                }
            }
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.browse_feed_add),
                        icon = MaterialSymbols.Rounded.Add,
                        onClick = onAddFeed,
                    )
                }
            }
            RowGroupGap()
        }
    }
}

private class ShownFeed(val feed: SourceFeed, val source: Source, val preset: SourceFeedPreset)

@Composable
private fun ReorderableCollectionItemScope.ManageFeedRow(
    feed: SourceFeed,
    source: Source,
    preset: SourceFeedPreset,
    onToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    val label = rememberFeedLabel(feed, source, preset)
    SettingsRow(
        title = label.title,
        subtitle = label.subtitle,
        subtitleMaxLines = 1,
        leading = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.draggableHandle(),
                )
                SourceIcon(source = source, size = LogoSize.Compact)
            }
        },
        trailing = {
            Switch(checked = feed.enabled, onCheckedChange = onToggle)
            OverflowMenu { close ->
                DropdownMenuItem(
                    label = stringResource(MR.strings.browse_feed_rename),
                    icon = MaterialSymbols.Rounded.Edit,
                    onClick = {
                        close()
                        onRename()
                    },
                )
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_delete),
                    icon = MaterialSymbols.Rounded.Delete,
                    onClick = {
                        close()
                        onRemove()
                    },
                )
            }
        },
    )
}
