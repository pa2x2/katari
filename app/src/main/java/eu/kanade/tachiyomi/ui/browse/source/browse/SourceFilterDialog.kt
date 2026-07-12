package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: EntryFilterList,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    presets: List<SourceFeedPreset>,
    onReset: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onEditPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    canDeletePreset: (String) -> Boolean,
    onSaveAsNewPreset: (() -> Unit)? = null,
    currentPresetName: String? = null,
    onUpdateCurrentPreset: (() -> Unit)? = null,
    onFilter: () -> Unit,
    onUpdate: (EntryFilterList) -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val updateFilters = { onUpdate(filters) }
    var presetMenuExpanded by remember { mutableStateOf(false) }
    var saveMenuExpanded by remember { mutableStateOf(false) }
    val isError = errorMessage != null

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        LazyColumn {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                ) {
                    TextButton(onClick = onReset, enabled = !isLoading) {
                        Text(
                            text = stringResource(MR.strings.action_reset),
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
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
                                expanded = presetMenuExpanded,
                                onDismissRequest = { presetMenuExpanded = false },
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(text = preset.name) },
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
                                                            contentDescription = stringResource(
                                                                MR.strings.action_delete,
                                                            ),
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
                                    expanded = saveMenuExpanded,
                                    onDismissRequest = { saveMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(MR.strings.browse_feed_save_as_new_preset))
                                        },
                                        onClick = {
                                            saveMenuExpanded = false
                                            onSaveAsNewPreset()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(
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

                    Button(onClick = {
                        onFilter()
                        onDismissRequest()
                    }, enabled = !isLoading && !isError) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                }
                HorizontalDivider()
            }

            if (isLoading) {
                item {
                    HeadingItem(stringResource(MR.strings.loading))
                }
            } else if (isError) {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(text = errorMessage)
                        if (onRetry != null) {
                            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                                Text(stringResource(MR.strings.action_retry))
                            }
                        }
                    }
                }
            } else {
                items(filters) {
                    FilterItem(it, updateFilters)
                }
            }
        }
    }
}

@Composable
private fun FilterItem(filter: EntryFilter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is EntryFilter.Header -> {
            HeadingItem(filter.name)
        }
        is EntryFilter.Separator -> {
            HorizontalDivider()
        }
        is EntryFilter.CheckBox -> {
            CheckboxItem(
                label = filter.name,
                checked = filter.state,
            ) {
                filter.state = !filter.state
                onUpdate()
            }
        }
        is EntryFilter.TriState -> {
            TriStateItem(
                label = filter.name,
                state = filter.state.toTriStateFilter(),
            ) {
                filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            }
        }
        is EntryFilter.Text -> {
            TextItem(
                label = filter.name,
                value = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is EntryFilter.Select<*> -> {
            SelectItem(
                label = filter.name,
                options = filter.values,
                selectedIndex = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is EntryFilter.Sort -> {
            CollapsibleBox(
                heading = filter.name,
            ) {
                Column {
                    filter.values.mapIndexed { index, item ->
                        val sortAscending = filter.state?.ascending
                            ?.takeIf { index == filter.state?.index }
                        SortItem(
                            label = item,
                            sortDescending = if (sortAscending != null) !sortAscending else null,
                            onClick = {
                                val ascending = if (index == filter.state?.index) {
                                    !filter.state!!.ascending
                                } else {
                                    filter.state?.ascending ?: true
                                }
                                filter.state = EntryFilter.Sort.Selection(
                                    index = index,
                                    ascending = ascending,
                                )
                                onUpdate()
                            },
                        )
                    }
                }
            }
        }
        is EntryFilter.Group<*> -> {
            CollapsibleBox(
                heading = filter.name,
            ) {
                Column {
                    filter.state
                        .filterIsInstance<EntryFilter<*>>()
                        .map { FilterItem(filter = it, onUpdate = onUpdate) }
                }
            }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        EntryFilter.TriState.STATE_IGNORE -> TriState.DISABLED
        EntryFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        EntryFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> EntryFilter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> EntryFilter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> EntryFilter.TriState.STATE_EXCLUDE
    }
}
