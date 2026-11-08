package app.katari.upcoming.components.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEach
import app.katari.presentation.kit.content.CalendarMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.WindowWidth
import app.katari.presentation.kit.window.currentWindowLayout
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toJavaDayOfWeek
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private const val DAYS_OF_WEEK = 7

@Composable
fun Calendar(
    selectedYearMonth: YearMonth,
    events: Map<LocalDate, Int>,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (day: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        CalenderHeader(
            yearMonth = selectedYearMonth,
            onPreviousClick = { setSelectedYearMonth(selectedYearMonth.minusMonth()) },
            onNextClick = { setSelectedYearMonth(selectedYearMonth.plusMonth()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KatariTheme.spacing.s)
                .padding(start = KatariTheme.spacing.l),
        )
        CalendarGrid(
            selectedYearMonth = selectedYearMonth,
            events = events,
            onClickDay = onClickDay,
        )
    }
}

@Composable
private fun CalendarGrid(
    selectedYearMonth: YearMonth,
    events: Map<LocalDate, Int>,
    onClickDay: (day: LocalDate) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val localeFirstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
    val weekDays = remember(locale) {
        (0 until DAYS_OF_WEEK)
            .map { DayOfWeek.of((localeFirstDayOfWeek - 1 + it) % DAYS_OF_WEEK + 1) }
    }

    val emptyFieldCount = weekDays.indexOf(selectedYearMonth.firstDay.dayOfWeek.toJavaDayOfWeek())
    val daysInMonth = selectedYearMonth.numberOfDays

    VerticalGrid(
        columns = SimpleGridCells.Fixed(DAYS_OF_WEEK),
        modifier = if (currentWindowLayout().width == WindowWidth.Medium) {
            Modifier.widthIn(max = CalendarMetrics.gridMaxWidth)
        } else {
            Modifier
        },
    ) {
        weekDays.fastForEach { item ->
            Text(
                text = item.getDisplayName(
                    TextStyle.NARROW,
                    locale,
                ),
                role = TextRole.SectionTitle,
                textAlign = TextAlign.Center,
            )
        }
        repeat(emptyFieldCount) { Box { } }
        repeat(daysInMonth) { dayIndex ->
            val localDate = LocalDate(selectedYearMonth.year, selectedYearMonth.month, dayIndex + 1)
            CalendarDay(
                date = localDate,
                onDayClick = { onClickDay(localDate) },
                events = events[localDate] ?: 0,
            )
        }
    }
}
