package mihon.presentation.kit.field

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.dialog.DialogActions
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

typealias DatePickerState = androidx.compose.material3.DatePickerState
typealias SelectableDates = androidx.compose.material3.SelectableDates

/** Dates are UTC midnight epoch milliseconds, as Material's picker reports them. */
@Composable
fun rememberDatePickerState(
    initialSelectedDateMillis: Long? = null,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
): DatePickerState {
    return androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = selectableDates,
    )
}

/**
 * A calendar. With a [title] it shows the title and the selected date above the calendar; without one it is just the
 * calendar, for a dialog that already has its own title.
 */
@Composable
fun DatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    if (title == null) {
        androidx.compose.material3.DatePicker(
            state = state,
            modifier = modifier,
            title = null,
            headline = null,
            showModeToggle = false,
        )
    } else {
        androidx.compose.material3.DatePicker(
            state = state,
            modifier = modifier,
            title = {
                // The padding Material gives its default title, which this one replaces.
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = KatariTheme.spacing.xl,
                        end = KatariTheme.spacing.m,
                        top = KatariTheme.spacing.l,
                    ),
                )
            },
            showModeToggle = false,
        )
    }
}

/**
 * A calendar in its own dialog, closed by the paired actions every dialog has; [confirmLabel] names what picking the
 * date does, and stays disabled until a date is selected. Material's own dialog frames the calendar, which needs a
 * wider window than the kit [mihon.presentation.kit.dialog.Dialog] gives.
 */
@Composable
fun DatePickerDialog(
    title: String,
    state: DatePickerState,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DialogActions(
                confirm = DialogAction(confirmLabel, onConfirm, enabled = state.selectedDateMillis != null),
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
                // Material's button row is already inset by a little at the end; this lines the actions up with the
                // calendar's edges.
                modifier = Modifier.padding(start = KatariTheme.spacing.m, end = KatariTheme.spacing.xs),
            )
        },
    ) {
        DatePicker(state = state, title = title)
    }
}
