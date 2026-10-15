package mihon.presentation.kit.field

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

typealias TimePickerState = androidx.compose.material3.TimePickerState

/** Uses the device's 12- or 24-hour clock. */
@Composable
fun rememberTimePickerState(initialHour: Int, initialMinute: Int): TimePickerState {
    return androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
}

@Composable
fun TimePicker(state: TimePickerState, modifier: Modifier = Modifier) {
    androidx.compose.material3.TimePicker(state = state, modifier = modifier)
}
