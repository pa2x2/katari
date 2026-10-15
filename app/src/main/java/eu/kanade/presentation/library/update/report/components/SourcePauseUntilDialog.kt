package eu.kanade.presentation.library.update.report.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.DatePickerDialog
import mihon.presentation.kit.field.SelectableDates
import mihon.presentation.kit.field.TimePicker
import mihon.presentation.kit.field.rememberDatePickerState
import mihon.presentation.kit.field.rememberTimePickerState
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
            title = stringResource(MR.strings.library_update_pause_until_title),
            state = dateState,
            onConfirm = { pickedDay = dateState.selectedDateMillis },
            onDismissRequest = onDismissRequest,
        )
    } else {
        val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute)
        val date = Instant.fromEpochMilliseconds(day).toLocalDateTime(TimeZone.UTC).date
        val until = LocalDateTime(date, LocalTime(timeState.hour, timeState.minute)).toInstant(timeZone)
        Dialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(MR.strings.library_update_pause_until_title),
            confirm = DialogAction(
                label = stringResource(MR.strings.action_ok),
                onClick = { onConfirm(until.toEpochMilliseconds()) },
                enabled = until > Clock.System.now(),
            ),
            dismiss = DialogAction(label = stringResource(MR.strings.action_back), onClick = { pickedDay = null }),
        ) {
            TimePicker(state = timeState)
        }
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
