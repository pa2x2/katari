package eu.kanade.presentation.more.settings.screen.duplicate

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One title exclusion pattern, with controls to reorder, edit and delete it. */
@Composable
internal fun DuplicateTitleExclusionItem(
    pattern: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SettingsRow(
        title = pattern,
        trailing = {
            Row {
                IconButton(
                    icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(MR.strings.action_move_to_top),
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(MR.strings.action_move_to_bottom),
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Edit,
                    contentDescription = stringResource(MR.strings.action_edit),
                    onClick = onEdit,
                )
                IconButton(
                    icon = MaterialSymbols.Rounded.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    onClick = onDelete,
                )
            }
        },
        onClick = onEdit,
    )
}
