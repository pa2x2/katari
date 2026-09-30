package eu.kanade.tachiyomi.ui.browse.source.browse.preset

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            onClick = { expanded = true },
            enabled = actions.hasMenu,
            label = {
                Text(
                    text = actions.currentPresetName ?: stringResource(MR.strings.filter_no_preset),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = stringResource(MR.strings.browse_filter_presets),
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(
            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            fun closeThen(action: () -> Unit) {
                expanded = false
                action()
            }
            actions.presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(text = preset.displayName(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (preset.id == actions.currentPresetId) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        } else {
                            Spacer(Modifier.width(24.dp))
                        }
                    },
                    trailingIcon = {
                        if (actions.canDelete(preset.id)) {
                            Row {
                                IconButton(onClick = { closeThen { actions.onEdit(preset.id) } }) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = stringResource(MR.strings.action_edit),
                                    )
                                }
                                IconButton(onClick = { closeThen { actions.onDelete(preset.id) } }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(MR.strings.action_delete),
                                    )
                                }
                            }
                        }
                    },
                    onClick = { closeThen { actions.onApply(preset.id) } },
                )
            }
            val onUpdateCurrent = actions.onUpdateCurrent
            val onSaveAsNew = actions.onSaveAsNew
            val currentName = actions.currentPresetName
            if (actions.presets.isNotEmpty() && (onSaveAsNew != null || onUpdateCurrent != null)) {
                HorizontalDivider()
            }
            if (onUpdateCurrent != null && currentName != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.browse_feed_update_current_preset, currentName)) },
                    leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                    enabled = canSave,
                    onClick = { closeThen { onUpdateCurrent() } },
                )
            }
            if (onSaveAsNew != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.browse_feed_save_as_new_preset)) },
                    leadingIcon = { Icon(Icons.Outlined.BookmarkAdd, contentDescription = null) },
                    enabled = canSave,
                    onClick = { closeThen { onSaveAsNew() } },
                )
            }
        }
    }
}
