package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
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
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
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
    val positions = RowGroupPosition.ofRuns(uiModels) { it !is UpdatesUiModel.Header }
    // The group's row names the member only when all its updates come from one; otherwise each update names its own.
    val groupsSpanningMembers = uiModels
        .filterIsInstance<UpdatesUiModel.Group<UpdatesItem>>()
        .filter { group -> group.items.map { it.memberTitle }.distinct().size > 1 }
        .mapTo(mutableSetOf()) { it.key }
    itemsIndexed(
        items = uiModels,
        contentType = { _, model ->
            when (model) {
                is UpdatesUiModel.Header -> "header"
                is UpdatesUiModel.Item -> "item"
                is UpdatesUiModel.Group -> "group"
                is UpdatesUiModel.GroupMember -> "groupMember"
            }
        },
        key = { _, model ->
            when (model) {
                is UpdatesUiModel.Header -> "updatesHeader-${model.date}"
                is UpdatesUiModel.Item -> "updates-${model.item.update.key.type.name}-${model.item.update.key.id}"
                is UpdatesUiModel.Group -> "updatesGroup-${model.key}"
                is UpdatesUiModel.GroupMember ->
                    "updatesGroupMember-${model.item.update.key.type.name}-${model.item.update.key.id}"
            }
        },
    ) { index, model ->
        val position = positions[index] ?: RowGroupPosition.Alone
        when (model) {
            is UpdatesUiModel.Header -> {
                RowGroupHeader(
                    text = relativeDateText(model.date),
                    modifier = Modifier.animateItem(),
                )
            }
            is UpdatesUiModel.Item -> RowGroupItem(position = position, modifier = Modifier.animateItem()) {
                UnifiedUpdatesUiItem(
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
            }
            is UpdatesUiModel.GroupMember -> RowGroupItem(
                position = position,
                modifier = Modifier.animateItem(),
                continuesAbove = true,
            ) {
                UnifiedUpdatesUiItem(
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
                    nested = true,
                    showMemberTitle = model.group in groupsSpanningMembers,
                )
            }
            is UpdatesUiModel.Group -> RowGroupItem(position = position, modifier = Modifier.animateItem()) {
                UpdatesGroupUiItem(
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
    nested: Boolean = false,
    showMemberTitle: Boolean = true,
) {
    val update = item.update
    // A nested row's chapter is the row's only line, so it reads as body text rather than a detail.
    val chapterRole = if (nested) TextRole.Body else TextRole.Supporting
    UpdatesBaseUiItem(
        title = item.visibleEntryTitle,
        memberTitle = item.memberTitle.takeIf { showMemberTitle },
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
            if (!nested) update.entryType.InlineEntryTypeIndicator()
            ChapterMarks(
                unread = !update.consumed,
                bookmark = update is UpdateItem.EntryUpdate && update.update.bookmark,
                unreadLabel = stringResource(update.entryType.entryTypePresentation().unconsumedIndicatorLabel),
                role = chapterRole,
            )
            val subtitleText = when (update) {
                is UpdateItem.EntryUpdate -> update.update.chapterName
            }
            Text(
                text = subtitleText,
                modifier = Modifier.weight(weight = 1f, fill = false),
                role = chapterRole,
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
        nested = nested,
    )
}
