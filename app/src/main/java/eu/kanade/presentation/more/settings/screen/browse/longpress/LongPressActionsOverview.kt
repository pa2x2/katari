package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.SourcePickerSheet
import eu.kanade.presentation.more.settings.screen.browseLongPressPrioritySummary
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The profile's default long-press priority, which can be reordered, and the sources that override it. */
@Composable
internal fun LongPressActionsOverview(
    defaultPriority: List<CustomPreferences.BrowseLongPressAction>,
    overrides: Map<Long, List<CustomPreferences.BrowseLongPressAction>>,
    sources: List<Source>,
    sourceManager: SourceManager,
    onDefaultPriorityChange: (List<CustomPreferences.BrowseLongPressAction>) -> Unit,
    onAddOverride: (Long) -> Unit,
    onOpenOverride: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val priority = remember(defaultPriority) {
        sanitizeBrowseLongPressActionPriority(defaultPriority).toMutableStateList()
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, contentPadding) { from, to ->
        val fromIndex = priority.indexOfFirst { actionKey(it) == from.key }
        val toIndex = priority.indexOfFirst { actionKey(it) == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        priority.add(toIndex, priority.removeAt(fromIndex))
        onDefaultPriorityChange(priority.toList())
    }
    var showSourcePicker by rememberSaveable { mutableStateOf(false) }
    val sourceById = remember(sources) { sources.associateBy(Source::id) }
    val overriddenSourceIds = overrides.keys.sortedBy { sourceId ->
        sourceById[sourceId]?.visualName ?: sourceManager.getDisplayInfo(sourceId).name
    }

    RowList(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        item {
            RowGroupHeader(stringResource(MR.strings.pref_browse_long_press_action_default_profile))
        }
        item { LongPressActionsExplanation() }
        itemsIndexed(priority, key = { _, action -> actionKey(action) }) { index, action ->
            ReorderableItem(reorderableState, actionKey(action)) { isDragging ->
                RowGroupItem(RowGroupPosition.of(index, priority.size), lifted = isDragging) {
                    LongPressActionRow(
                        action = action,
                        supportingText = defaultActionSupportingText(action),
                        handleModifier = Modifier.draggableHandle(),
                        showDragHandle = true,
                    )
                }
            }
        }
        item { RowGroupGap() }
        item {
            RowGroupHeader(stringResource(MR.strings.pref_browse_long_press_action_source_overrides))
        }
        if (overrides.isEmpty()) {
            item {
                RowGroupNote(stringResource(MR.strings.pref_browse_long_press_action_no_source_overrides))
            }
        }
        // The overriding sources and the row that adds one share a card.
        val overrideCount = overriddenSourceIds.size + 1
        itemsIndexed(overriddenSourceIds, key = { _, sourceId -> "source:$sourceId" }) { index, overriddenSourceId ->
            RowGroupItem(RowGroupPosition.of(index, overrideCount)) {
                SettingsRow(
                    title =
                    sourceById[overriddenSourceId]?.name ?: sourceManager.getDisplayInfo(overriddenSourceId).name,
                    subtitle = browseLongPressPrioritySummary(overrides.getValue(overriddenSourceId)),
                    onClick = { onOpenOverride(overriddenSourceId) },
                )
            }
        }
        item {
            RowGroupItem(RowGroupPosition.of(overrideCount - 1, overrideCount)) {
                SettingsRow(
                    title = stringResource(MR.strings.pref_browse_long_press_action_add_source_override),
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = { showSourcePicker = true },
                )
            }
        }
    }

    if (showSourcePicker) {
        SourcePickerSheet(
            title = stringResource(MR.strings.pref_browse_long_press_action_select_source),
            sources = sources.filterNot { it.id in overrides },
            onSelect = {
                showSourcePicker = false
                onAddOverride(it.id)
            },
            onDismissRequest = { showSourcePicker = false },
        )
    }
}
