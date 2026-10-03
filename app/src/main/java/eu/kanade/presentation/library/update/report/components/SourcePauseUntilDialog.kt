package eu.kanade.presentation.library.update.report.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Asks for the day and then the time a source's pause ends, which has to be in the future.
 *
 * The date picker counts days in UTC, so the picked day is combined with the time in the device's time zone.
 *
 * @param onConfirm receives the end in epoch milliseconds.
 */
@Composable
internal fun SourcePauseUntilDialog(onConfirm: (Long) -> Unit, onDismissRequest: () -> Unit) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val now = remember { Clock.System.now().toLocalDateTime(timeZone) }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }

    val day = pickedDay
    if (day == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = now.date.plus(1, DateTimeUnit.DAY).utcMillis(),
            selectableDates = remember(now) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= now.date.utcMillis()
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = { pickedDay = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null,
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        ) {
            DatePicker(
                state = dateState,
                // The padding Material gives its default title, which this one replaces.
                title = {
                    Text(
                        text = stringResource(MR.strings.library_update_pause_until_title),
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    )
                },
                showModeToggle = false,
            )
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        val date = Instant.fromEpochMilliseconds(day).toLocalDateTime(TimeZone.UTC).date
        val until = LocalDateTime(date, LocalTime(timeState.hour, timeState.minute)).toInstant(timeZone)
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(MR.strings.library_update_pause_until_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(until.toEpochMilliseconds()) },
                    enabled = until > Clock.System.now(),
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickedDay = null }) {
                    Text(stringResource(MR.strings.action_back))
                }
            },
        )
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
