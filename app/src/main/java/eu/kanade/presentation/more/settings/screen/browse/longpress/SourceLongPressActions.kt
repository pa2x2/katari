package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import mihon.core.common.CustomPreferences
import mihon.core.common.sanitizeBrowseLongPressActionPriority
import mihon.entry.interactions.media.EntryImmersiveSourceAvailability
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.KatariTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

/** One source's long-press priority: the profile default until customized, then its own reorderable list. */
@Composable
internal fun SourceLongPressActions(
    sourceId: Long,
    defaultPriority: List<CustomPreferences.BrowseLongPressAction>,
    overridePriority: List<CustomPreferences.BrowseLongPressAction>?,
    sourceManager: SourceManager,
    onPriorityChange: (List<CustomPreferences.BrowseLongPressAction>) -> Unit,
    onUseDefault: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val displayedPriority = overridePriority ?: defaultPriority
    val priority = remember(sourceId, displayedPriority) {
        sanitizeBrowseLongPressActionPriority(displayedPriority).toMutableStateList()
    }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, contentPadding) { from, to ->
        val fromIndex = priority.indexOfFirst { actionKey(it) == from.key }
        val toIndex = priority.indexOfFirst { actionKey(it) == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        priority.add(toIndex, priority.removeAt(fromIndex))
        onPriorityChange(priority.toList())
    }
    val immersiveFeature = remember { context.appGraph.entryImmersiveFeature }
    val immersiveAvailable = immersiveFeature.sourceAvailability(sourceManager.get(sourceId)) is
        EntryImmersiveSourceAvailability.Available

    ScrollbarLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        item {
            RowGroupHeader(
                stringResource(
                    if (overridePriority == null) {
                        MR.strings.pref_browse_long_press_action_using_profile_default
                    } else {
                        MR.strings.pref_browse_long_press_action_custom_for_source
                    },
                ),
            )
        }
        item { LongPressActionsExplanation() }
        itemsIndexed(priority, key = { _, action -> actionKey(action) }) { index, action ->
            val position = RowGroupPosition.of(index, priority.size)
            if (overridePriority != null) {
                ReorderableItem(reorderableState, actionKey(action)) {
                    RowGroupItem(position) {
                        LongPressActionRow(
                            action = action,
                            supportingText = sourceActionSupportingText(action, immersiveAvailable),
                            handleModifier = Modifier.draggableHandle(),
                            showDragHandle = true,
                        )
                    }
                }
            } else {
                RowGroupItem(position) {
                    LongPressActionRow(
                        action = action,
                        supportingText = sourceActionSupportingText(action, immersiveAvailable),
                    )
                }
            }
        }
        item {
            Button(
                label = stringResource(
                    if (overridePriority == null) {
                        MR.strings.pref_browse_long_press_action_customize_source
                    } else {
                        MR.strings.pref_browse_long_press_action_use_profile_default
                    },
                ),
                onClick = {
                    if (overridePriority == null) {
                        onPriorityChange(defaultPriority)
                    } else {
                        onUseDefault()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                style = ButtonStyle.Text,
            )
        }
    }
}
