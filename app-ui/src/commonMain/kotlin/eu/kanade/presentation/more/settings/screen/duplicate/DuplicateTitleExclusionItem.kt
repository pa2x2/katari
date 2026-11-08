package eu.kanade.presentation.more.settings.screen.duplicate

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.Edit
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.list.SettingsRow
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
