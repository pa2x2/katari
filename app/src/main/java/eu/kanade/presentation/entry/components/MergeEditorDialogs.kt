package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MergeEditorDialog(
    title: String,
    entries: ImmutableList<MergeEditorEntry>,
    targetId: Long,
    targetLocked: Boolean,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = entries.size >= 2,
    confirmText: String = stringResource(MR.strings.action_ok),
    summaryText: String = stringResource(MR.strings.merge_editor_summary),
    removableIds: Set<Long> = emptySet(),
    libraryRemovalIds: Set<Long> = emptySet(),
    onSelectTarget: ((Long) -> Unit)? = null,
    onToggleRemove: ((Long) -> Unit)? = null,
    onToggleLibraryRemove: ((Long) -> Unit)? = null,
    onOpenManga: ((Long) -> Unit)? = null,
    confirmContent: (@Composable (() -> Unit) -> Unit)? = null,
    dismissContent: @Composable (() -> Unit) -> Unit = { dismiss ->
        TextButton(onClick = dismiss) {
            Text(text = stringResource(MR.strings.action_cancel))
        }
    },
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, PaddingValues()) { from, to ->
        val fromIndex = entries.indexOfFirst { it.id == from.key }
        val toIndex = entries.indexOfFirst { it.id == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        onMove(fromIndex, toIndex)
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ReorderableItem(reorderableState, entry.id, enabled = entries.size > 1) {
                            MergeEditorItem(
                                entry = entry,
                                isTarget = entry.id == targetId,
                                targetLocked = targetLocked,
                                markedForRemoval = entry.id in removableIds,
                                markedForLibraryRemoval = entry.id in libraryRemovalIds,
                                onSelectTarget = onSelectTarget,
                                onToggleRemove = onToggleRemove,
                                onToggleLibraryRemove = onToggleLibraryRemove,
                                onOpenManga = onOpenManga,
                            )
                        }
                    }
                }
                if (targetLocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(MR.strings.merge_existing_target_locked))
                    }
                }
            }
        },
        confirmButton = {
            confirmContent?.invoke(onConfirm) ?: TextButton(enabled = confirmEnabled, onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = { dismissContent(onDismissRequest) },
    )
}

@Composable
fun MergeTargetPickerSheet(
    title: String,
    query: String,
    entries: ImmutableList<MergeEditorEntry>,
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectTarget: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length),
            ),
        )
    }

    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length),
            )
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    eu.kanade.presentation.components.AdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_close),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 520.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                items(entries, key = { it.id }) { entry ->
                    ElevatedCard(onClick = { onSelectTarget(entry.id) }) {
                        StaticMergeEditorItem(
                            entry = entry,
                        )
                    }
                }
            }
            androidx.compose.material3.OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onQueryChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .focusRequester(focusRequester),
                label = { Text(text = stringResource(MR.strings.action_search)) },
                trailingIcon = {
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue("")
                                onQueryChange("")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = stringResource(MR.strings.action_reset),
                            )
                        }
                    }
                },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun StaticMergeEditorItem(
    entry: MergeEditorEntry,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.small, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            EntryCover.Square(
                data = entry.coverData,
                modifier = Modifier.size(64.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                Text(
                    text = entry.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (entry.isMember) {
                        buildString {
                            append(entry.subtitle)
                            append(" • ")
                            append(stringResource(MR.strings.label_member))
                        }
                    } else {
                        entry.subtitle
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.MergeEditorItem(
    entry: MergeEditorEntry,
    isTarget: Boolean,
    targetLocked: Boolean,
    markedForRemoval: Boolean,
    markedForLibraryRemoval: Boolean,
    onSelectTarget: ((Long) -> Unit)?,
    onToggleRemove: ((Long) -> Unit)?,
    onToggleLibraryRemove: ((Long) -> Unit)?,
    onOpenManga: ((Long) -> Unit)?,
    showDragHandle: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val isMarkedForMergeRemoval = markedForRemoval
    val isMarkedForLibraryRemoval = markedForLibraryRemoval
    val canRemoveEntry = entry.isRemovable && !isTarget
    val hasOverflowActions =
        (onSelectTarget != null && !targetLocked && !isTarget) ||
            (canRemoveEntry && onToggleRemove != null) ||
            (canRemoveEntry && onToggleLibraryRemove != null) ||
            onOpenManga != null
    val containerColor = when {
        isMarkedForLibraryRemoval -> MaterialTheme.colorScheme.errorContainer
        isMarkedForMergeRemoval -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val secondaryTextColor = when {
        isMarkedForLibraryRemoval -> MaterialTheme.colorScheme.onErrorContainer
        isMarkedForMergeRemoval -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor,
        ),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.small, vertical = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                EntryCover.Square(
                    data = entry.coverData,
                    modifier = Modifier.size(64.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    Text(
                        text = entry.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (entry.isMember) {
                            buildString {
                                append(entry.subtitle)
                                append(" • ")
                                append(stringResource(MR.strings.label_member))
                            }
                        } else {
                            entry.subtitle
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                    )
                    if (isMarkedForMergeRemoval) {
                        Text(
                            text = stringResource(MR.strings.action_remove),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMarkedForLibraryRemoval) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                    if (isMarkedForLibraryRemoval) {
                        Text(
                            text = stringResource(MR.strings.remove_from_library),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                when {
                    hasOverflowActions -> {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(MR.strings.label_more),
                                )
                            }
                            eu.kanade.presentation.components.DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                if (onSelectTarget != null && !targetLocked && !isTarget) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(MR.strings.action_set_as_root))
                                        },
                                        onClick = {
                                            onSelectTarget(entry.id)
                                            expanded = false
                                        },
                                    )
                                }
                                if (canRemoveEntry && onToggleRemove != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (markedForRemoval) {
                                                        MR.strings.action_keep
                                                    } else {
                                                        MR.strings.action_remove
                                                    },
                                                ),
                                            )
                                        },
                                        onClick = {
                                            onToggleRemove(entry.id)
                                            expanded = false
                                        },
                                    )
                                }
                                if (canRemoveEntry && onToggleLibraryRemove != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (markedForLibraryRemoval) {
                                                        MR.strings.action_keep_in_library
                                                    } else {
                                                        MR.strings.remove_from_library
                                                    },
                                                ),
                                            )
                                        },
                                        onClick = {
                                            onToggleLibraryRemove(entry.id)
                                            expanded = false
                                        },
                                    )
                                }
                                if (onOpenManga != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (isTarget) {
                                                        MR.strings.action_open_merged_entry
                                                    } else {
                                                        MR.strings.action_open
                                                    },
                                                ),
                                            )
                                        },
                                        onClick = {
                                            onOpenManga(entry.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    onSelectTarget != null && !targetLocked -> {
                        RadioButton(
                            selected = isTarget,
                            onClick = { onSelectTarget(entry.id) },
                        )
                    }
                    isTarget -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (showDragHandle) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = null,
                        modifier = Modifier.draggableHandle(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isTarget) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.padding.extraSmall),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = stringResource(MR.strings.label_target),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

data class MergeEditorEntry(
    val id: Long,
    val entry: Entry,
    val subtitle: String,
    val isRemovable: Boolean = false,
    val isMember: Boolean = false,
    val titleOverride: String? = null,
    val coverData: Any? = entry,
) {
    val title: String
        get() = titleOverride ?: entry.displayTitle
}
