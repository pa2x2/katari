package app.katari.sourcemigration.config.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.DragHandle
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.LogoSize
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.surface.Pill
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.sourcemigration.config.MigrationSource
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.util.system.LocaleHelper
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
    val sourceRows = LocalAppUiGraph.current.sourceRows
    ReorderableItem(
        state = state,
        key = key,
        enabled = dragEnabled,
    ) { isDragging ->
        RowGroupItem(position = position, modifier = Modifier.animateItem(), lifted = isDragging) {
            SettingsRow(
                title = source.name,
                leading = { sourceRows.Icon(source = source.source, size = LogoSize.Row, modifier = Modifier) },
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
