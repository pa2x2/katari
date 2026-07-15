package eu.kanade.presentation.updates

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DotSeparatorText
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryChapterDownloadIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.entry.partialProgressLabel
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import mihon.entry.interactions.EntryDownloadState
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.entry.model.EntryCover as EntryCoverData

internal fun LazyListScope.updatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "updates-lastUpdated") {
        Box(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

internal fun <T> LazyListScope.updatesUiItems(
    uiModels: List<UpdatesUiModel<T>>,
    itemKey: (T) -> String,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is UpdatesUiModel.Header -> "header"
                is UpdatesUiModel.Item -> "item"
            }
        },
        key = {
            when (it) {
                is UpdatesUiModel.Header -> "updatesHeader-${it.date}"
                is UpdatesUiModel.Item -> itemKey(it.item)
            }
        },
    ) { item ->
        when (item) {
            is UpdatesUiModel.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    text = relativeDateText(item.date),
                )
            }

            is UpdatesUiModel.Item -> itemContent(item.item)
        }
    }
}

internal fun LazyListScope.unifiedUpdatesUiItems(
    uiModels: List<UpdatesUiModel<UpdatesItem>>,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    updatesUiItems(
        uiModels = uiModels,
        itemKey = { "updates-${it.update.key.type.name}-${it.update.key.id}" },
    ) { updatesItem ->
        UnifiedUpdatesUiItem(
            modifier = Modifier.animateItemFastScroll(),
            item = updatesItem,
            selectionMode = selectionMode,
            onUpdateSelected = onUpdateSelected,
            onClickCover = onClickCover,
            onClickUpdate = onClickUpdate,
            onDownloadChapter = onDownloadChapter,
        )
    }
}

@Composable
fun UpdatesBaseUiItem(
    title: String,
    coverData: EntryCoverData,
    selected: Boolean,
    read: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickCover: (() -> Unit)? = null,
    subtitle: @Composable RowScope.(Float) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = if (read) DISABLED_ALPHA else 1f

    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Square(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = coverData,
            onClick = onClickCover,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                subtitle(textAlpha)
            }
        }

        trailing?.invoke()
    }
}

@Composable
internal fun UnifiedUpdatesUiItem(
    item: UpdatesItem,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val update = item.update
    UpdatesBaseUiItem(
        title = item.visibleEntryTitle,
        coverData = item.visibleCoverData,
        selected = item.selected,
        read = update.consumed,
        onClick = {
            when {
                selectionMode -> onUpdateSelected(item, !item.selected, false)
                else -> onClickUpdate(item)
            }
        },
        onLongClick = {
            onUpdateSelected(item, !item.selected, true)
        },
        modifier = modifier,
        onClickCover = { onClickCover(item) }.takeIf { !selectionMode },
        subtitle = { textAlpha ->
            var textHeight by remember { mutableIntStateOf(0) }

            update.entryType.InlineEntryTypeIndicator(
                modifier = Modifier
                    .padding(end = 4.dp),
            )

            if (!update.consumed) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = stringResource(
                        update.entryType.entryTypePresentation().unconsumedIndicatorLabel,
                    ),
                    modifier = Modifier
                        .height(8.dp)
                        .padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (update is UpdateItem.EntryUpdate && update.update.bookmark) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                    modifier = Modifier
                        .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            val subtitleText = when (update) {
                is UpdateItem.EntryUpdate -> update.update.chapterName
            }
            Text(
                text = subtitleText,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textHeight = it.size.height },
                modifier = Modifier.weight(weight = 1f, fill = false),
            )

            val readProgress = update.update.progressPosition
                .takeIf { !update.update.read }
                ?.let { update.entryType.partialProgressLabel(it) }
            if (readProgress != null) {
                DotSeparatorText()
                Text(
                    text = readProgress,
                    maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = if (update is UpdateItem.EntryUpdate && !selectionMode && item.downloadSupported) {
            {
                EntryChapterDownloadIndicator(
                    enabled = true,
                    modifier = Modifier.padding(start = 4.dp),
                    downloadStateProvider = item.downloadStateProvider,
                    downloadProgressProvider = item.downloadProgressProvider,
                    onClick = { onDownloadChapter(listOf(item), it) },
                )
            }
        } else {
            null
        },
    )
}

@Composable
internal fun ChapterUpdatesUiItem(
    title: String,
    subtitle: String,
    coverData: EntryCoverData,
    selected: Boolean,
    read: Boolean,
    bookmark: Boolean,
    readProgress: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadChapter: ((ChapterDownloadAction) -> Unit)? = null,
    downloadStateProvider: (() -> EntryDownloadState)? = null,
    downloadProgressProvider: (() -> Int)? = null,
    modifier: Modifier = Modifier,
) {
    UpdatesBaseUiItem(
        title = title,
        coverData = coverData,
        selected = selected,
        read = read,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        onClickCover = onClickCover,
        subtitle = { textAlpha ->
            var textHeight by remember { mutableIntStateOf(0) }
            if (!read) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = stringResource(MR.strings.action_filter_unconsumed),
                    modifier = Modifier
                        .height(8.dp)
                        .padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (bookmark) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                    modifier = Modifier
                        .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = subtitle,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textHeight = it.size.height },
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            if (readProgress != null) {
                DotSeparatorText()
                Text(
                    text = readProgress,
                    maxLines = 1,
                    color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = if (downloadStateProvider != null && downloadProgressProvider != null) {
            {
                EntryChapterDownloadIndicator(
                    enabled = onDownloadChapter != null,
                    modifier = Modifier.padding(start = 4.dp),
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = onDownloadChapter,
                )
            }
        } else {
            null
        },
    )
}
