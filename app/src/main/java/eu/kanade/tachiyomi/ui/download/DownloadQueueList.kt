package eu.kanade.tachiyomi.ui.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.util.plus

/**
 * The queue as one card per source. A download is dragged within its source's card; dragging a source's header folds
 * the cards away so whole sources can be reordered, as the queue is fetched in this order.
 */
@Composable
internal fun DownloadQueueList(
    queue: List<EntryDownloadQueueGroup>,
    viewModel: DownloadQueueViewModel,
    contentPadding: PaddingValues,
) {
    // What the current drag may swap with: the key of the dragged download's source, or SOURCES_DRAG.
    var dragScope by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, contentPadding) { from, to ->
        viewModel.arrange(from.key as String, to.key as String)
    }
    val onDragStopped = {
        dragScope = null
        viewModel.submitArrangement()
    }
    val canMoveSources = queue.size > 1

    ScrollbarLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        // Room for the Pause button over the end of the list.
        contentPadding = contentPadding + PaddingValues(
            top = KatariTheme.spacing.s,
            bottom = KatariTheme.sizes.touchTarget + KatariTheme.spacing.xxl,
        ),
    ) {
        queue.forEach { group ->
            item(key = group.key, contentType = "source") {
                ReorderableItem(
                    state = reorderableState,
                    key = group.key,
                    enabled = canMoveSources && (dragScope == null || dragScope == SOURCES_DRAG),
                ) {
                    RowGroupHeader(
                        text = group.sourceName,
                        modifier = Modifier.animateItem(),
                        actions = {
                            Text(
                                text = group.items.size.toString(),
                                role = TextRole.Label,
                                tone = ContentTone.Secondary,
                            )
                            if (canMoveSources) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.DragHandle,
                                    contentDescription = null,
                                    tone = ContentTone.Secondary,
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { dragScope = SOURCES_DRAG },
                                        onDragStopped = onDragStopped,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
            if (dragScope == SOURCES_DRAG) return@forEach
            group.items.forEachIndexed { index, download ->
                item(key = download.key, contentType = "download") {
                    ReorderableItem(
                        state = reorderableState,
                        key = download.key,
                        enabled = dragScope == null || dragScope == group.key,
                    ) {
                        RowGroupItem(
                            position = RowGroupPosition.of(index, group.items.size),
                            modifier = Modifier.animateItem(),
                        ) {
                            DownloadQueueRow(
                                item = download,
                                isFirstInGroup = index == 0,
                                isLastInGroup = index == group.items.lastIndex,
                                handleModifier = Modifier.draggableHandle(
                                    onDragStarted = { dragScope = group.key },
                                    onDragStopped = onDragStopped,
                                ),
                                onMove = { toTop -> viewModel.moveItem(download, toTop) },
                                onMoveSeries = { toTop -> viewModel.moveSeries(download, toTop) },
                                onCancel = { viewModel.cancel(download) },
                                onCancelSeries = { viewModel.cancelSeries(download) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SOURCES_DRAG = "sources"
