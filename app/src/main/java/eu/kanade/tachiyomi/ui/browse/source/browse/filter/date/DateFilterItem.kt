package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.displayMessage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun DateFilterItem(filter: EntryDateFilter, onUpdate: () -> Unit) {
    val editor = LocalDateFilterEditor.current
    val locale = datePickerLocale()
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (editing) {
            editor.open(filter, onCancel = { editing = false }) { value ->
                filter.state = value
                onUpdate()
            }
        }
    }
    val issue = filter.validateFilter().firstOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            editing = true
        }.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(filter.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                EntryPartialDate.parse(filter.state)?.displayDate(locale) ?: filter.state.ifBlank {
                    stringResource(MR.strings.filter_any_date)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            issue?.let {
                Text(
                    it.displayMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (filter.state.isNotBlank() && !filter.required) {
            IconButton(onClick = {
                filter.dateValue = null
                onUpdate()
            }) {
                Icon(Icons.Outlined.Clear, stringResource(MR.strings.filter_clear_date))
            }
        }
    }
}
