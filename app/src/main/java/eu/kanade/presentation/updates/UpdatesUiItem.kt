package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryChapterDownloadIndicator
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.entry.partialProgressLabel
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChapterMarks
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.unifiedUpdatesUiItems(
    uiModels: List<UpdatesUiModel<UpdatesItem>>,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onGroupSelected: (List<UpdatesItem>, Boolean) -> Unit,
    onToggleGroupExpanded: (String) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    isOpenApplicable: (UpdatesItem) -> Boolean,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    translateStatusOf: (UpdatesItem) -> EntryTranslateStatus?,
    isTranslateApplicable: (UpdatesItem) -> Boolean,
    onTranslateChapter: (UpdatesItem, ChapterTranslateAction) -> Unit,
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is UpdatesUiModel.Header -> "header"
                is UpdatesUiModel.Item -> "item"
                is UpdatesUiModel.Group -> "group"
            }
        },
        key = {
            when (it) {
                is UpdatesUiModel.Header -> "updatesHeader-${it.date}"
                is UpdatesUiModel.Item -> "updates-${it.item.update.key.type.name}-${it.item.update.key.id}"
                is UpdatesUiModel.Group -> "updatesGroup-${it.key}"
            }
        },
    ) { model ->
        when (model) {
            is UpdatesUiModel.Header -> {
                SectionHeader(
                    text = relativeDateText(model.date),
                    modifier = Modifier.animateItem(),
                )
            }
            is UpdatesUiModel.Item -> UnifiedUpdatesUiItem(
                modifier = Modifier.animateItem(),
                item = model.item,
                selectionMode = selectionMode,
                onUpdateSelected = onUpdateSelected,
                onClickCover = onClickCover,
                isOpenApplicable = isOpenApplicable,
                onClickUpdate = onClickUpdate,
                onDownloadChapter = onDownloadChapter,
                translateStatusOf = translateStatusOf,
                isTranslateApplicable = isTranslateApplicable,
                onTranslateChapter = onTranslateChapter,
            )
            is UpdatesUiModel.Group -> UpdatesGroupUiItem(
                modifier = Modifier.animateItem(),
                group = model,
                selectionMode = selectionMode,
                onToggleExpanded = { onToggleGroupExpanded(model.key) },
                onGroupSelected = onGroupSelected,
                onClickCover = onClickCover,
                onDownloadChapter = onDownloadChapter,
            )
        }
    }
}

@Composable
internal fun UnifiedUpdatesUiItem(
    item: UpdatesItem,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    isOpenApplicable: (UpdatesItem) -> Boolean,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    translateStatusOf: (UpdatesItem) -> EntryTranslateStatus?,
    isTranslateApplicable: (UpdatesItem) -> Boolean,
    onTranslateChapter: (UpdatesItem, ChapterTranslateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val update = item.update
    UpdatesBaseUiItem(
        title = item.visibleEntryTitle,
        memberTitle = item.memberTitle,
        coverData = item.visibleCoverData,
        selected = item.selected,
        read = update.consumed,
        onClick = {
            when {
                selectionMode -> onUpdateSelected(item, !item.selected, false)
                isOpenApplicable(item) -> onClickUpdate(item)
            }
        },
        onLongClick = {
            onUpdateSelected(item, !item.selected, true)
        },
        modifier = modifier,
        onClickCover = { onClickCover(item) }.takeIf { !selectionMode },
        subtitle = { tone ->
            update.entryType.InlineEntryTypeIndicator()
            ChapterMarks(
                unread = !update.consumed,
                bookmark = update is UpdateItem.EntryUpdate && update.update.bookmark,
                unreadLabel = stringResource(update.entryType.entryTypePresentation().unconsumedIndicatorLabel),
                role = TextRole.Supporting,
            )
            val subtitleText = when (update) {
                is UpdateItem.EntryUpdate -> update.update.chapterName
            }
            Text(
                text = subtitleText,
                modifier = Modifier.weight(weight = 1f, fill = false),
                role = TextRole.Supporting,
                tone = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val readProgress = update.update.progressPosition
                .takeIf { !update.update.read }
                ?.let { update.entryType.partialProgressLabel(it) }
            if (readProgress != null) {
                Text(
                    text = "• $readProgress",
                    role = TextRole.Supporting,
                    tone = ContentTone.Disabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = if (update is UpdateItem.EntryUpdate && !selectionMode && item.downloadAvailable) {
            {
                EntryChapterDownloadIndicator(
                    enabled = true,
                    modifier = Modifier.padding(start = KatariTheme.spacing.xs),
                    downloadStateProvider = item.downloadStateProvider,
                    downloadProgressProvider = item.downloadProgressProvider,
                    onClick = { onDownloadChapter(listOf(item), it) },
                    translateStatusProvider = { translateStatusOf(item) },
                    onTranslateClick = { action: ChapterTranslateAction -> onTranslateChapter(item, action) }
                        .takeIf { isTranslateApplicable(item) },
                )
            }
        } else {
            null
        },
    )
}
