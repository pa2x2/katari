package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.SegmentedButtons
import mihon.presentation.kit.surface.SelectableCard
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Month
import java.time.format.TextStyle

@Composable
internal fun DateEditorHeader(
    filter: EntryDateFilter,
    state: PartialDateEditorState,
    onChange: (PartialDateEditorState) -> Unit,
    onCancel: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = filter.name,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            role = TextRole.SheetTitle,
        )
        IconButton(
            icon = MaterialSymbols.Rounded.Close,
            contentDescription = stringResource(MR.strings.action_cancel),
            onClick = onCancel,
        )
    }
    filter.filterMetadata.description?.takeIf { it.isNotBlank() }?.let {
        Text(it, role = TextRole.Body, tone = ContentTone.Secondary)
    }
    if (filter.allowedPrecisions.size > 1) {
        SegmentedButtons(
            options = EntryDatePrecision.entries.filter { it in filter.allowedPrecisions },
            selected = state.precision,
            onSelect = { onChange(state.choosePrecision(it)) },
            label = { precisionLabel(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (!state.typing) {
        val locale = datePickerLocale()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            EntryDatePrecision.entries.take(state.precision.ordinal + 1).forEach { component ->
                val value = when (component) {
                    EntryDatePrecision.YEAR -> state.year?.toString()
                    EntryDatePrecision.MONTH -> state.month?.let {
                        Month.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, locale)
                    }
                    EntryDatePrecision.DAY -> state.day?.toString()
                }
                SelectableCard(
                    selected = state.step == component,
                    onClick = { onChange(state.editComponent(component)) },
                    modifier = Modifier.weight(1f),
                    enabled = when (component) {
                        EntryDatePrecision.YEAR -> true
                        EntryDatePrecision.MONTH -> state.year != null
                        EntryDatePrecision.DAY -> state.year != null && state.month != null
                    },
                ) {
                    Text(
                        text = if (component == EntryDatePrecision.DAY) {
                            stringResource(MR.strings.filter_day)
                        } else {
                            precisionLabel(component)
                        },
                        role = TextRole.Badge,
                        tone = ContentTone.Secondary,
                    )
                    Text(value ?: stringResource(MR.strings.filter_choose), role = TextRole.SectionTitle)
                }
            }
        }
    }
}
