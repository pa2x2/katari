package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import tachiyomi.i18n.MR
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
            filter.name,
            Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, stringResource(MR.strings.action_cancel)) }
    }
    filter.filterMetadata.description?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (filter.allowedPrecisions.size > 1) {
        val precisions = EntryDatePrecision.entries.filter { it in filter.allowedPrecisions }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            precisions.forEachIndexed { index, precision ->
                SegmentedButton(
                    modifier = Modifier.fillMaxHeight(),
                    selected = state.precision == precision,
                    onClick = { onChange(state.choosePrecision(precision)) },
                    shape = SegmentedButtonDefaults.itemShape(index, precisions.size),
                    icon = {},
                ) { Text(precisionLabel(precision)) }
            }
        }
    }

    if (!state.typing) {
        val locale = datePickerLocale()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryDatePrecision.entries.take(state.precision.ordinal + 1).forEach { component ->
                val value = when (component) {
                    EntryDatePrecision.YEAR -> state.year?.toString()
                    EntryDatePrecision.MONTH -> state.month?.let {
                        Month.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, locale)
                    }
                    EntryDatePrecision.DAY -> state.day?.toString()
                }
                OutlinedButton(
                    onClick = { onChange(state.editComponent(component)) },
                    enabled = when (component) {
                        EntryDatePrecision.YEAR -> true
                        EntryDatePrecision.MONTH -> state.year != null
                        EntryDatePrecision.DAY -> state.year != null && state.month != null
                    },
                    modifier = Modifier.weight(1f).semantics { selected = state.step == component },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (state.step == component) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            Color.Transparent
                        },
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            if (component ==
                                EntryDatePrecision.DAY
                            ) {
                                stringResource(MR.strings.filter_day)
                            } else {
                                precisionLabel(component)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            value ?: stringResource(MR.strings.filter_choose),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
