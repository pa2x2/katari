package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun DateYearGrid(
    filter: EntryDateFilter,
    state: PartialDateEditorState,
    onChange: (PartialDateEditorState) -> Unit,
) {
    var jumping by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val jumpYear = input.toIntOrNull()?.takeIf { it in 1..9999 }
    val jump = {
        jumpYear?.let { onChange(state.copy(browsingYear = it)) }
        keyboard?.hide()
        jumping = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    MR.strings.filter_year_page,
                    state.yearPageStart,
                    (state.yearPageStart + 11).coerceAtMost(9999),
                ),
                modifier = Modifier.weight(1f),
                role = TextRole.Label,
            )
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowLeft,
                contentDescription = stringResource(MR.strings.filter_previous_years),
                onClick = { onChange(state.browseYears(-12)) },
                enabled = state.yearPageStart > (filter.minimum?.year ?: 1),
            )
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
                contentDescription = stringResource(MR.strings.filter_next_years),
                onClick = { onChange(state.browseYears(12)) },
                enabled = state.yearPageStart + 12 <= (filter.maximum?.year ?: 9999),
            )
        }
        Button(
            label = stringResource(MR.strings.filter_jump_year),
            onClick = { jumping = !jumping },
            style = ButtonStyle.Text,
        )
        if (jumping) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    label = stringResource(MR.strings.filter_precision_year),
                    isError = input.isNotBlank() && jumpYear == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (jumpYear != null) jump() }),
                )
                Button(
                    label = stringResource(MR.strings.filter_jump_go),
                    onClick = jump,
                    style = ButtonStyle.Text,
                    enabled = jumpYear != null,
                )
            }
        }
        (state.yearPageStart..(state.yearPageStart + 11).coerceAtMost(9999)).toList().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                row.forEach { year ->
                    DatePickerCell(
                        label = year.toString(),
                        selected = state.year == year,
                        enabled = filter.containsPeriod(EntryPartialDate(year)),
                        modifier = Modifier.weight(1f),
                        onClick = { onChange(state.chooseYear(year)) },
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
