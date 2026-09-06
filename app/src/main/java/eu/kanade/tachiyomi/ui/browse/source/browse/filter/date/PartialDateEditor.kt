package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PartialDateEditor(filter: EntryDateFilter, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var state by rememberSaveable(filter, stateSaver = PartialDateEditorSaver) {
        mutableStateOf(PartialDateEditorState.initial(filter))
    }
    val onChange: (PartialDateEditorState) -> Unit = { state = it }
    val focus = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var inputFocused by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.step, state.typing) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxHeight(0.9f).imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateEditorHeader(filter, state, onChange, onCancel)
            if (state.typing) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                OutlinedTextField(
                    value = state.raw,
                    onValueChange = { raw ->
                        state = state.copy(
                            raw = raw,
                            precision = EntryPartialDate.parse(raw)?.precision
                                ?.takeIf { it in filter.allowedPrecisions } ?: state.precision,
                        )
                    },
                    label = { Text(stringResource(MR.strings.filter_type_date)) },
                    placeholder = {
                        Text(
                            EntryDatePrecision.entries.filter { it in filter.allowedPrecisions }.joinToString(" / ") {
                                when (it) {
                                    EntryDatePrecision.YEAR -> "YYYY"
                                    EntryDatePrecision.MONTH -> "YYYY-MM"
                                    EntryDatePrecision.DAY -> "YYYY-MM-DD"
                                }
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged {
                        if (inputFocused && !it.isFocused) state = state.copy(showErrors = true)
                        inputFocused = it.isFocused
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        state = state.copy(showErrors = true)
                        focus.clearFocus()
                    }),
                    isError = state.showErrors && state.validationIssues(filter).isNotEmpty(),
                )
            } else {
                when (state.step) {
                    EntryDatePrecision.YEAR -> DateYearGrid(filter, state, onChange)
                    EntryDatePrecision.MONTH -> DateMonthGrid(filter, state, onChange)
                    EntryDatePrecision.DAY -> DateDayCalendar(filter, state, onChange)
                }
            }
        }
        DateEditorFooter(
            filter = filter,
            state = state,
            onToggleTyping = {
                focus.clearFocus()
                state = state.toggleTyping(filter.allowedPrecisions)
            },
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
    }
}

private val PartialDateEditorSaver = listSaver<PartialDateEditorState, Any>(
    save = {
        listOf(
            it.precision.name, it.year ?: 0, it.month ?: 0, it.day ?: 0, it.step.name,
            it.browsingYear, it.typing, it.raw, it.showErrors,
        )
    },
    restore = {
        PartialDateEditorState(
            precision = EntryDatePrecision.valueOf(it[0] as String),
            year = (it[1] as Int).takeIf { value -> value != 0 },
            month = (it[2] as Int).takeIf { value -> value != 0 },
            day = (it[3] as Int).takeIf { value -> value != 0 },
            step = EntryDatePrecision.valueOf(it[4] as String),
            browsingYear = it[5] as Int,
            typing = it[6] as Boolean,
            raw = it[7] as String,
            showErrors = it[8] as Boolean,
        )
    },
)
