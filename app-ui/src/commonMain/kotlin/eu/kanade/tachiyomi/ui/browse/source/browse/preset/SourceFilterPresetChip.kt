package eu.kanade.tachiyomi.ui.browse.source.browse.preset

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ArrowDropDown
import app.katari.icons.materialsymbols.rounded.Bookmark
import app.katari.icons.materialsymbols.rounded.BookmarkAdd
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.Edit
import app.katari.icons.materialsymbols.rounded.Save
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.chip.AssistChip
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuDivider
import app.katari.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Names the preset the draft came from and opens the preset menu: apply, edit, or delete a preset, update the
 * current one, or save the draft as a new one. Saving is offered only while the draft can be saved.
 */
@Composable
internal fun SourceFilterPresetChip(actions: SourceFilterPresetActions, canSave: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            label = actions.currentPresetName ?: stringResource(MR.strings.filter_no_preset),
            onClick = { expanded = true },
            icon = MaterialSymbols.Rounded.Bookmark,
            trailingIcon = MaterialSymbols.Rounded.ArrowDropDown,
            enabled = actions.hasMenu,
            onClickLabel = stringResource(MR.strings.browse_filter_presets),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            wide = true,
        ) {
            fun closeThen(action: () -> Unit) {
                expanded = false
                action()
            }
            actions.presets.forEach { preset ->
                DropdownMenuItem(
                    label = preset.displayName(),
                    onClick = { closeThen { actions.onApply(preset.id) } },
                    icon = if (preset.id == actions.currentPresetId) {
                        MaterialSymbols.Rounded.Check
                    } else {
                        MaterialSymbols.Rounded.Bookmark
                    },
                    trailing = if (actions.canDelete(preset.id)) {
                        {
                            Row {
                                IconButton(
                                    icon = MaterialSymbols.Rounded.Edit,
                                    contentDescription = stringResource(MR.strings.action_edit),
                                    onClick = { closeThen { actions.onEdit(preset.id) } },
                                )
                                IconButton(
                                    icon = MaterialSymbols.Rounded.Delete,
                                    contentDescription = stringResource(MR.strings.action_delete),
                                    onClick = { closeThen { actions.onDelete(preset.id) } },
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }
            val onUpdateCurrent = actions.onUpdateCurrent
            val onSaveAsNew = actions.onSaveAsNew
            val currentName = actions.currentPresetName
            if (actions.presets.isNotEmpty() && (onSaveAsNew != null || onUpdateCurrent != null)) {
                DropdownMenuDivider()
            }
            if (onUpdateCurrent != null && currentName != null) {
                DropdownMenuItem(
                    label = stringResource(MR.strings.browse_feed_update_current_preset, currentName),
                    onClick = { closeThen { onUpdateCurrent() } },
                    icon = MaterialSymbols.Rounded.Save,
                    enabled = canSave,
                )
            }
            if (onSaveAsNew != null) {
                DropdownMenuItem(
                    label = stringResource(MR.strings.browse_feed_save_as_new_preset),
                    onClick = { closeThen { onSaveAsNew() } },
                    icon = MaterialSymbols.Rounded.BookmarkAdd,
                    enabled = canSave,
                )
            }
        }
    }
}
