package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.displayMessage
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun DateFilterItem(filter: EntryDateFilter, isError: Boolean, onUpdate: () -> Unit) {
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
    SettingsRow(
        title = filter.name,
        subtitle = EntryPartialDate.parse(filter.state)?.displayDate(locale) ?: filter.state.ifBlank {
            stringResource(MR.strings.filter_any_date)
        },
        leading = {
            Icon(
                MaterialSymbols.Rounded.CalendarMonth,
                contentDescription = null,
                tone = if (isError) ContentTone.Error else ContentTone.Primary,
            )
        },
        trailing = if (filter.state.isNotBlank() && !filter.required) {
            {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.filter_clear_date),
                    onClick = {
                        filter.dateValue = null
                        onUpdate()
                    },
                )
            }
        } else {
            null
        },
        onClick = { editing = true },
        below = issue?.let {
            { Text(it.displayMessage(), role = TextRole.Supporting, tone = ContentTone.Error) }
        },
    )
}
