package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SelectRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

private const val MAX_CHIP_OPTIONS = 5
private const val MAX_CHIP_LABEL_LENGTH = 16

/**
 * A single-choice filter: a row of chips when every option fits at a glance, otherwise a dropdown field that hides
 * the options behind the current value.
 */
@Composable
internal fun FilterSelectControl(
    label: String,
    options: Array<out Any?>,
    selectedIndex: Int,
    isError: Boolean,
    onSelect: (Int) -> Unit,
) {
    val labels = options.map { it.toString() }
    if (labels.size in 2..MAX_CHIP_OPTIONS && labels.all { it.length <= MAX_CHIP_LABEL_LENGTH }) {
        FilterChoiceChips(label, labels, selectedIndex, isError, onSelect)
    } else {
        SelectRow(
            label = label,
            options = labels,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            isError = isError,
        )
    }
}

@Composable
private fun FilterChoiceChips(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    isError: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
    ) {
        Text(
            text = label,
            role = TextRole.Supporting,
            tone = if (isError) ContentTone.Error else ContentTone.Secondary,
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    label = option,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}
