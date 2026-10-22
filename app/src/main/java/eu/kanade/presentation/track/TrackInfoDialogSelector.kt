package eu.kanade.presentation.track

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.DatePicker
import mihon.presentation.kit.field.SelectableDates
import mihon.presentation.kit.field.rememberDatePickerState
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.WheelNumberPicker
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackStatusSelector(
    selection: Long,
    onSelectionChange: (Long) -> Unit,
    selections: Map<Long, StringResource?>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(MR.strings.status),
        content = {
            ScrollbarLazyColumn {
                rowGroupItems(items = selections.entries.toList(), key = { it.key }) { (key, value) ->
                    ChoiceRow(
                        title = value?.let { stringResource(it) } ?: "",
                        selected = selection == key,
                        onClick = { onSelectionChange(key) },
                    )
                }
            }
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackChapterSelector(
    entryType: EntryType,
    selection: Int,
    onSelectionChange: (Int) -> Unit,
    range: Iterable<Int>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(entryType.entryTypePresentation().childListTitle),
        content = {
            WheelNumberPicker(
                items = range.toList(),
                modifier = Modifier.align(Alignment.Center),
                startIndex = selection,
                onSelectionChanged = { onSelectionChange(it) },
            )
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackScoreSelector(
    selection: String,
    onSelectionChange: (String) -> Unit,
    selections: List<String>,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BaseSelector(
        title = stringResource(MR.strings.score),
        content = {
            WheelTextPicker(
                items = selections,
                modifier = Modifier.align(Alignment.Center),
                startIndex = selections.indexOf(selection).takeIf { it > 0 } ?: (selections.size / 2),
                onSelectionChanged = { onSelectionChange(selections[it]) },
            )
        },
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun TrackDateSelector(
    title: String,
    initialSelectedDateMillis: Long,
    selectableDates: SelectableDates,
    onConfirm: (Long) -> Unit,
    onRemove: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = selectableDates,
    )
    Column {
        SheetHeader(title = title)
        DatePicker(state = pickerState)
        SheetActions(
            confirm = DialogAction(
                label = stringResource(MR.strings.action_save),
                onClick = { onConfirm(pickerState.selectedDateMillis!!) },
            ),
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
            neutral = onRemove?.let { DialogAction(stringResource(MR.strings.action_remove), it) },
        )
    }
}

/** A tracking value picker shown as a page of the tracking sheet: the picker between a title and OK and Cancel. */
@Composable
private fun BaseSelector(
    title: String,
    content: @Composable BoxScope.() -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Column {
        SheetHeader(title = title)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = KatariTheme.spacing.s),
            content = content,
        )
        SheetActions(
            confirm = DialogAction(stringResource(MR.strings.action_save), onConfirm),
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackStatusSelectorPreviews() {
    KatariPreviewTheme {
        Surface {
            TrackStatusSelector(
                selection = 1,
                onSelectionChange = {},
                selections = mapOf(
                    // Anilist values
                    1L to MR.strings.reading,
                    2L to MR.strings.plan_to_read,
                    3L to MR.strings.completed,
                    4L to MR.strings.on_hold,
                    5L to MR.strings.dropped,
                    6L to MR.strings.repeating,
                ),
                onConfirm = {},
                onDismissRequest = {},
            )
        }
    }
}
