package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import tachiyomi.i18n.MR
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    MR.strings.filter_year_page,
                    state.yearPageStart,
                    (state.yearPageStart + 11).coerceAtMost(9999),
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(
                enabled = state.yearPageStart > (filter.minimum?.year ?: 1),
                onClick = { onChange(state.browseYears(-12)) },
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(MR.strings.filter_previous_years))
            }
            IconButton(
                enabled = state.yearPageStart + 12 <= (filter.maximum?.year ?: 9999),
                onClick = { onChange(state.browseYears(12)) },
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(MR.strings.filter_next_years))
            }
        }
        TextButton(onClick = { jumping = !jumping }) { Text(stringResource(MR.strings.filter_jump_year)) }
        if (jumping) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(MR.strings.filter_precision_year)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (jumpYear != null) jump() }),
                    isError = input.isNotBlank() && jumpYear == null,
                )
                TextButton(enabled = jumpYear != null, onClick = jump) {
                    Text(stringResource(MR.strings.filter_jump_go))
                }
            }
        }
        (state.yearPageStart..(state.yearPageStart + 11).coerceAtMost(9999)).toList().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
