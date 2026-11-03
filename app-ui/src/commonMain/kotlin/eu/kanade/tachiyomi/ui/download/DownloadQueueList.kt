package eu.kanade.tachiyomi.ui.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
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
    // The release animation starts from the finger, so the last dragged row stays held while it settles too
    var lastDraggedKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val listCoordinates = remember { ListCoordinatesHolder() }
    val draggingGroup by rememberUpdatedState(queue.find { it.key == dragScope })
    // Auto-scroll would otherwise carry a download held at its card's edge off screen
    val groupScrollState = remember(listState) {
        LimitedScrollableState(listState) { pixels ->
            draggingGroup?.let { listState.limitScrollToGroup(it, pixels) } ?: pixels
        }
    }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = contentPadding,
        scroller = rememberScroller(
            scrollableState = groupScrollState,
            pixelAmountProvider = { listState.layoutInfo.viewportSize.height * AUTO_SCROLL_STEP },
        ),
    ) { from, to ->
        viewModel.arrange(from.key as String, to.key as String)
    }
    val onDragStopped = {
        dragScope = null
        viewModel.submitArrangement()
    }
    val canMoveSources = queue.size > 1

    RowList(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates.value = it },
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
                    ) { isDragging ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, group.items.size),
                            modifier = Modifier.animateItem().then(
                                if (isDragging || download.key == lastDraggedKey) {
                                    Modifier.keepInGroup(
                                        lazyListState = listState,
                                        listCoordinates = listCoordinates,
                                        groupKey = group.key,
                                        lastItemKey = group.items.last().key,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                            lifted = isDragging,
                        ) {
                            DownloadQueueRow(
                                item = download,
                                isFirstInGroup = index == 0,
                                isLastInGroup = index == group.items.lastIndex,
                                handleModifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                        dragScope = group.key
                                        lastDraggedKey = download.key
                                    },
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

// Share of the list's height auto-scroll moves per step while a row is held at its edge
private const val AUTO_SCROLL_STEP = 0.05f
