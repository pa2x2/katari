package mihon.presentation.kit.field

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

typealias TimePickerState = androidx.compose.material3.TimePickerState

@Composable
fun rememberTimePickerState(initialHour: Int, initialMinute: Int, is24Hour: Boolean): TimePickerState {
    return androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )
}

@Composable
fun TimePicker(state: TimePickerState, modifier: Modifier = Modifier) {
    androidx.compose.material3.TimePicker(state = state, modifier = modifier)
}

/** A clock in its own dialog. [dismiss] defaults to Cancel; a later step of a picker can make it go back instead. */
@Composable
fun TimePickerDialog(
    title: String,
    state: TimePickerState,
    confirm: DialogAction,
    onDismissRequest: () -> Unit,
    dismiss: DialogAction = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
) {
    Dialog(onDismissRequest = onDismissRequest, title = title, confirm = confirm, dismiss = dismiss) {
        TimePicker(state = state, modifier = Modifier.fillMaxWidth().wrapContentWidth())
    }
}
