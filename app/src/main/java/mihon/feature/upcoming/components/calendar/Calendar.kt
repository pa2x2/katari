package mihon.feature.upcoming.components.calendar

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEach
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toJavaDayOfWeek
import mihon.presentation.kit.content.CalendarMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.WindowWidth
import mihon.presentation.kit.theme.currentWindowWidth
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
    val locale = LocalConfiguration.current.locales[0]
    val localeFirstDayOfWeek = WeekFields.of(locale).firstDayOfWeek.value
    val weekDays = remember(locale) {
        (0 until DAYS_OF_WEEK)
            .map { DayOfWeek.of((localeFirstDayOfWeek - 1 + it) % DAYS_OF_WEEK + 1) }
    }

    val emptyFieldCount = weekDays.indexOf(selectedYearMonth.firstDay.dayOfWeek.toJavaDayOfWeek())
    val daysInMonth = selectedYearMonth.numberOfDays

    VerticalGrid(
        columns = SimpleGridCells.Fixed(DAYS_OF_WEEK),
        modifier = if (currentWindowWidth() == WindowWidth.Medium) {
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
