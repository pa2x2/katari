package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.displayMessage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun DateEditorFooter(
    filter: EntryDateFilter,
    state: PartialDateEditorState,
    onToggleTyping: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val value = state.value
    val valid = state.canConfirm(filter)
    val issue = state.validationIssues(filter).firstOrNull()?.takeIf {
        state.showErrors ||
            (!state.typing && value != null)
    }
    val feedback = when {
        issue != null -> issue.displayMessage()
        !state.typing && state.missingStep() != null -> chooseComponentLabel(state.missingStep()!!)
        value != null -> stringResource(
            when (value.precision) {
                EntryDatePrecision.YEAR -> MR.strings.filter_year_selected
                EntryDatePrecision.MONTH -> MR.strings.filter_month_selected
                EntryDatePrecision.DAY -> MR.strings.filter_day_selected
            },
        )
        else -> stringResource(MR.strings.filter_date_confirm_help)
    }
    HorizontalDivider()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            feedback,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = if (issue == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onToggleTyping) {
                Text(stringResource(if (state.typing) MR.strings.filter_use_picker else MR.strings.filter_type_date))
            }
            if (!filter.required) {
                TextButton(onClick = { onConfirm("") }) { Text(stringResource(MR.strings.filter_clear_date)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(MR.strings.action_cancel))
            }
            Button(enabled = valid, onClick = {
                onConfirm(value?.toString().orEmpty())
            }, modifier = Modifier.weight(1.6f)) {
                Text(
                    when {
                        value != null -> stringResource(
                            MR.strings.filter_use_date_value,
                            value.displayDate(datePickerLocale()),
                        )
                        valid -> stringResource(MR.strings.filter_clear_date)
                        else -> stringResource(MR.strings.filter_use_date)
                    },
                )
            }
        }
    }
}
