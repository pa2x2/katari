package app.katari.upcoming.components.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.CalendarDayCell
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
fun CalendarDay(
    date: LocalDate,
    events: Int,
    onDayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    CalendarDayCell(
        day = date.day.toString(),
        today = date == today,
        past = date < today,
        events = events,
        onClick = onDayClick,
        modifier = modifier,
    )
}
