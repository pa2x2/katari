package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Month
import java.time.format.TextStyle

@Composable
internal fun DateMonthGrid(
    filter: EntryDateFilter,
    state: PartialDateEditorState,
    onChange: (PartialDateEditorState) -> Unit,
) {
    val year = state.year ?: return
    val locale = datePickerLocale()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(year.toString(), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { onChange(state.editComponent(EntryDatePrecision.YEAR)) }) {
                Text(stringResource(MR.strings.filter_change_year))
            }
        }
        Month.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { month ->
                    DatePickerCell(
                        label = month.getDisplayName(TextStyle.SHORT_STANDALONE, locale),
                        description = month.getDisplayName(TextStyle.FULL_STANDALONE, locale),
                        selected = state.month == month.value,
                        enabled = filter.containsPeriod(EntryPartialDate(year, month.value)),
                        modifier = Modifier.weight(1f),
                        onClick = { onChange(state.chooseMonth(month.value)) },
                    )
                }
            }
        }
    }
}
