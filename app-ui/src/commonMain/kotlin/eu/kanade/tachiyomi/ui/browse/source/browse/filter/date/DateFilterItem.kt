package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CalendarMonth
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.displayMessage
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
