package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.screen.browseLongPressPrioritySummary
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
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

    ScrollbarLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        item {
            SectionHeader(stringResource(MR.strings.pref_browse_long_press_action_default_profile))
        }
        item { LongPressActionsExplanation() }
        items(priority, key = ::actionKey) { action ->
            ReorderableItem(reorderableState, actionKey(action)) {
                LongPressActionRow(
                    action = action,
                    supportingText = defaultActionSupportingText(action),
                    handleModifier = Modifier.draggableHandle(),
                    showDragHandle = true,
                )
            }
        }
        item { HorizontalDivider() }
        item {
            SectionHeader(stringResource(MR.strings.pref_browse_long_press_action_source_overrides))
        }
        if (overrides.isEmpty()) {
            item {
                Text(
                    text = stringResource(MR.strings.pref_browse_long_press_action_no_source_overrides),
                    modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
                    role = TextRole.Body,
                    tone = ContentTone.Secondary,
                )
            }
        } else {
            items(overriddenSourceIds, key = { "source:$it" }) { overriddenSourceId ->
                SettingsRow(
                    title =
                    sourceById[overriddenSourceId]?.name ?: sourceManager.getDisplayInfo(overriddenSourceId).name,
                    subtitle = browseLongPressPrioritySummary(overrides.getValue(overriddenSourceId)),
                    onClick = { onOpenOverride(overriddenSourceId) },
                )
            }
        }
        item {
            SettingsRow(
                title = stringResource(MR.strings.pref_browse_long_press_action_add_source_override),
                icon = MaterialSymbols.Rounded.Add,
                onClick = { showSourcePicker = true },
            )
        }
    }

    if (showSourcePicker) {
        SourceOverridePicker(
            sources = sources.filterNot { it.id in overrides },
            onSelect = {
                showSourcePicker = false
                onAddOverride(it.id)
            },
            onDismissRequest = { showSourcePicker = false },
        )
    }
}
