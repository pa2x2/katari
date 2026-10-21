package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.PickerCell
import mihon.presentation.kit.field.PickerCellKind
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
internal fun DateDayCalendar(
    filter: EntryDateFilter,
    state: PartialDateEditorState,
    onChange: (PartialDateEditorState) -> Unit,
) {
    val year = state.year ?: return
    val month = state.month ?: return
    val locale = datePickerLocale()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val offset = (LocalDate.of(year, month, 1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = List(offset) { null } + (1..EntryPartialDate.daysInMonth(year, month)).toList()
    BoxWithConstraints {
        // Preserve non-overlapping touch targets even on unusually narrow windows.
        val width = maxWidth.coerceAtLeast(KatariTheme.sizes.touchTarget * DAYS_PER_WEEK)
        Column(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            Text(EntryPartialDate(year, month).displayDate(locale), role = TextRole.Label)
            Row(Modifier.width(width)) {
                repeat(DAYS_PER_WEEK) { index ->
                    val weekday = firstDay.plus(index.toLong())
                    val fullName = weekday.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    Text(
                        weekday.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                        modifier = Modifier.weight(1f).semantics { contentDescription = fullName },
                        role = TextRole.Supporting,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            cells.chunked(DAYS_PER_WEEK).forEach { row ->
                Row(Modifier.width(width)) {
                    row.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val date = EntryPartialDate(year, month, day)
                            PickerCell(
                                label = day.toString(),
                                kind = PickerCellKind.Day,
                                description = date.displayDate(locale),
                                selected = state.day == day,
                                enabled = filter.containsPeriod(date),
                                modifier = Modifier.weight(1f),
                                onClick = { onChange(state.chooseDay(day)) },
                            )
                        }
                    }
                    repeat(DAYS_PER_WEEK - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7
