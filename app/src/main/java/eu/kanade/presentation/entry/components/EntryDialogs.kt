package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.entry.EntryScreenModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Composable
fun DeleteChaptersDialog(
    entryType: EntryType,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.are_you_sure))
        },
        text = {
            Text(text = stringResource(entryType.entryTypePresentation().deleteChildrenConfirmationLabel))
        },
    )
}

@Composable
fun SetIntervalDialog(
    interval: Int,
    nextUpdate: Instant?,
    entryType: EntryType? = null,
    onDismissRequest: () -> Unit,
    onValueChanged: ((Int) -> Unit)? = null,
) {
    var selectedInterval by rememberSaveable { mutableIntStateOf(if (interval < 0) -interval else 0) }
    val presentation = entryType.entryTypePresentation()

    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.pref_library_update_smart_update)) },
        text = {
            Column {
                if (nextUpdateDays != null && nextUpdateDays >= 0 && interval >= 0) {
                    Text(
                        stringResource(
                            presentation.intervalExpectedUpdateLabel,
                            pluralStringResource(
                                MR.plurals.day,
                                count = nextUpdateDays,
                                nextUpdateDays,
                            ),
                            pluralStringResource(
                                MR.plurals.day,
                                count = interval.absoluteValue,
                                interval.absoluteValue,
                            ),
                        ),
                    )
                } else {
                    Text(
                        stringResource(presentation.intervalExpectedUpdateNullLabel),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.padding.small))

                if (onValueChanged != null && (!isReleaseBuildType)) {
                    Text(stringResource(MR.strings.manga_interval_custom_amount))

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val size = DpSize(width = maxWidth / 2, height = 128.dp)
                        val items = (0..FetchInterval.MAX_INTERVAL)
                            .map {
                                if (it == 0) {
                                    stringResource(MR.strings.label_default)
                                } else {
                                    it.toString()
                                }
                            }

                        WheelTextPicker(
                            items = items,
                            size = size,
                            startIndex = selectedInterval,
                            onSelectionChanged = { selectedInterval = it },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueChanged?.invoke(selectedInterval)
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
fun EditDisplayNameDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length),
            ),
        )
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.action_set_display_name)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                TextButton(onClick = { onConfirm("") }) {
                    Text(text = stringResource(MR.strings.action_reset))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
fun ManageMergeDialog(
    targetId: Long,
    members: List<EntryScreenModel.MergeMember>,
    removableIds: List<Long>,
    libraryRemovalIds: List<Long>,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onSelectTarget: (Long) -> Unit,
    onToggleRemoveMember: (Long) -> Unit,
    onToggleRemoveMemberFromLibrary: (Long) -> Unit,
    onRemoveMembers: (List<Long>) -> Unit,
    onUnmergeAll: () -> Unit,
) {
    MergeEditorDialog(
        title = stringResource(MR.strings.action_manage_merge),
        entries = members.map(EntryScreenModel.MergeMember::toMergeEditorEntry).toPersistentList(),
        targetId = targetId,
        targetLocked = false,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onSaveOrder,
        removableIds = removableIds.toSet(),
        libraryRemovalIds = libraryRemovalIds.toSet(),
        onSelectTarget = onSelectTarget,
        onToggleRemove = onToggleRemoveMember,
        onToggleLibraryRemove = onToggleRemoveMemberFromLibrary,
        onOpenManga = onOpenManga,
        confirmContent = {
            TextButton(onClick = onSaveOrder) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                TextButton(onClick = onUnmergeAll) {
                    Text(text = stringResource(MR.strings.action_unmerge))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        },
    )
}

private fun EntryScreenModel.MergeMember.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isRemovable = true,
    )
}
