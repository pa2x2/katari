package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeedPreset
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceFilterRootFooter(
    presets: List<SourceFeedPreset>,
    onReset: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onEditPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    canDeletePreset: (String) -> Boolean,
    onSaveAsNewPreset: (() -> Unit)?,
    currentPresetName: String?,
    onUpdateCurrentPreset: (() -> Unit)?,
    onFilter: () -> Unit,
    resetEnabled: Boolean,
    filterEnabled: Boolean,
) {
    var presetMenuExpanded by remember { mutableStateOf(false) }
    var saveMenuExpanded by remember { mutableStateOf(false) }

    SourceFilterSheetActionRow {
        TextButton(onClick = onReset, enabled = resetEnabled) {
            Text(
                text = stringResource(MR.strings.filter_reset_defaults),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.primary),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (presets.isNotEmpty()) {
            Box {
                IconButton(onClick = { presetMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = stringResource(MR.strings.browse_filter_presets),
                    )
                }

                DropdownMenu(
                    modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                    expanded = presetMenuExpanded,
                    onDismissRequest = { presetMenuExpanded = false },
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(text = preset.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = {
                                if (canDeletePreset(preset.id)) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                presetMenuExpanded = false
                                                onEditPreset(preset.id)
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = stringResource(MR.strings.action_edit),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                presetMenuExpanded = false
                                                onDeletePreset(preset.id)
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(MR.strings.action_delete),
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                presetMenuExpanded = false
                                onApplyPreset(preset.id)
                            },
                        )
                    }
                }
            }
        }

        if (onSaveAsNewPreset != null) {
            if (currentPresetName != null && onUpdateCurrentPreset != null) {
                Box {
                    IconButton(onClick = { saveMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = stringResource(MR.strings.action_save),
                        )
                    }

                    DropdownMenu(
                        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                        expanded = saveMenuExpanded,
                        onDismissRequest = { saveMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.browse_feed_save_as_new_preset)) },
                            onClick = {
                                saveMenuExpanded = false
                                onSaveAsNewPreset()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        MR.strings.browse_feed_update_current_preset,
                                        currentPresetName,
                                    ),
                                )
                            },
                            onClick = {
                                saveMenuExpanded = false
                                onUpdateCurrentPreset()
                            },
                        )
                    }
                }
            } else {
                IconButton(onClick = onSaveAsNewPreset) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = stringResource(MR.strings.action_save),
                    )
                }
            }
        }

        Button(onClick = onFilter, enabled = filterEnabled) {
            Text(stringResource(MR.strings.action_apply))
        }
    }
}
