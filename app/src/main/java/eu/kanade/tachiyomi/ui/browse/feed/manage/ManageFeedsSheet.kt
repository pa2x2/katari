package eu.kanade.tachiyomi.ui.browse.feed.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabelText
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
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
        PaddingValues(vertical = MaterialTheme.padding.small),
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

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Box {
            Column(modifier = Modifier.padding(top = MaterialTheme.padding.medium)) {
                Text(
                    text = stringResource(MR.strings.browse_manage_feeds),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
                ScrollbarLazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = MaterialTheme.padding.small),
                ) {
                    items(items = feeds, key = { it.id }) { feed ->
                        val source = sourceFor(feed.sourceId) ?: return@items
                        val preset = presetFor(feed) ?: return@items
                        ReorderableItem(state = reorderableState, key = feed.id) {
                            ManageFeedRow(
                                feed = feed,
                                source = source,
                                preset = preset,
                                onToggle = { onToggle(feed.id, it) },
                                onRename = { onRename(feed) },
                                onRemove = { removeFeed(feed.id) },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddFeed)
                        .padding(horizontal = MaterialTheme.padding.medium, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(MR.strings.browse_feed_add),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.ManageFeedRow(
    feed: SourceFeed,
    source: Source,
    preset: SourceFeedPreset,
    onToggle: (Boolean) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.medium, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = null,
            modifier = Modifier.draggableHandle(),
        )
        SourceIcon(
            source = source,
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        FeedLabelText(
            label = rememberFeedLabel(feed, source, preset),
            modifier = Modifier.weight(1f),
        )
        Switch(checked = feed.enabled, onCheckedChange = onToggle)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(MR.strings.label_more),
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.browse_feed_rename)) },
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_delete)) },
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRemove()
                    },
                )
            }
        }
    }
}
