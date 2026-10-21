package mihon.feature.migration.config.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.feature.migration.config.MigrationSource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.KatariTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

/**
 * A source the migration may search, tapped to move it between the selected and available lists. Selected sources
 * are searched in order, so with [dragEnabled] the row carries a handle to reorder it.
 */
@Composable
internal fun LazyItemScope.MigrationConfigSourceRow(
    source: MigrationSource,
    position: RowGroupPosition,
    showLanguage: Boolean,
    dragEnabled: Boolean,
    state: ReorderableLazyListState,
    key: Any,
    onClick: () -> Unit,
) {
    ReorderableItem(
        state = state,
        key = key,
        enabled = dragEnabled,
    ) { _ ->
        RowGroupItem(position = position, modifier = Modifier.animateItem()) {
            SettingsRow(
                title = source.name,
                leading = { SourceIcon(source = source.source) },
                trailing = if (showLanguage || dragEnabled) {
                    {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showLanguage) {
                                Pill(text = LocaleHelper.getShortDisplayName(source.shortLanguage, uppercase = true))
                            }
                            if (dragEnabled) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.DragHandle,
                                    contentDescription = null,
                                    modifier = Modifier.draggableHandle(),
                                )
                            }
                        }
                    }
                } else {
                    null
                },
                onClick = onClick,
            )
        }
    }
}
