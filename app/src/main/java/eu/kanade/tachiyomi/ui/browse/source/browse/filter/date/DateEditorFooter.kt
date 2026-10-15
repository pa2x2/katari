package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.displayMessage
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
    ) {
        Text(
            text = feedback,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            role = TextRole.Supporting,
            tone = if (issue == null) ContentTone.Secondary else ContentTone.Error,
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                label = stringResource(if (state.typing) MR.strings.filter_use_picker else MR.strings.filter_type_date),
                onClick = onToggleTyping,
                style = ButtonStyle.Text,
            )
            if (!filter.required) {
                Button(
                    label = stringResource(MR.strings.filter_clear_date),
                    onClick = { onConfirm("") },
                    style = ButtonStyle.Text,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            Button(
                label = stringResource(MR.strings.action_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Outlined,
            )
            Button(
                label = when {
                    value != null -> stringResource(
                        MR.strings.filter_use_date_value,
                        value.displayDate(datePickerLocale()),
                    )
                    valid -> stringResource(MR.strings.filter_clear_date)
                    else -> stringResource(MR.strings.filter_use_date)
                },
                onClick = { onConfirm(value?.toString().orEmpty()) },
                modifier = Modifier.weight(CONFIRM_BUTTON_WEIGHT),
                enabled = valid,
            )
        }
    }
}

// The confirm button names the date it applies, so it gets more of the row than Cancel.
private const val CONFIRM_BUTTON_WEIGHT = 1.6f
