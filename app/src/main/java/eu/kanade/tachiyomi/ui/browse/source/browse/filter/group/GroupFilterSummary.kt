package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeLabel
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.changeLabel
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.displayText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What a group currently selects, for its collapsed header.
 *
 * On/off and include/exclude options are listed by their current state, so options selected by default still show;
 * every other value is listed only when it differs from the source default.
 */
internal data class GroupSelectionSummary(
    val included: List<String>,
    val excluded: List<String>,
    val values: List<FilterChangeLabel>,
) {
    val isEmpty: Boolean get() = included.isEmpty() && excluded.isEmpty() && values.isEmpty()
}

internal fun EntryFilter.Group<*>.selectionSummary(changes: FilterChanges): GroupSelectionSummary {
    val included = mutableListOf<String>()
    val excluded = mutableListOf<String>()
    val values = mutableListOf<FilterChangeLabel>()
    fun visit(filter: EntryFilter<*>) {
        when (filter) {
            is EntryFilter.Group<*> -> filter.state.filterIsInstance<EntryFilter<*>>().forEach(::visit)
            is EntryFilter.CheckBox -> if (filter.state) included += filter.name
            is EntryFilter.TriState -> when {
                filter.isIncluded() -> included += filter.name
                filter.isExcluded() -> excluded += filter.name
            }
            else -> if (changes[filter].isChanged) filter.changeLabel(emptyList())?.let(values::add)
        }
    }
    visit(this)
    return GroupSelectionSummary(included, excluded, values)
}

/** Included names in the accent color, then excluded names in the error color, then the remaining values. */
@Composable
internal fun GroupSelectionSummary.text(): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    val notText = excluded.takeIf { it.isNotEmpty() }?.let {
        stringResource(MR.strings.filter_summary_excluded, it.joinToString())
    }
    val valueTexts = values.map { it.displayText() }
    return buildAnnotatedString {
        fun separate() {
            if (length > 0) append(" · ")
        }
        if (included.isNotEmpty()) withStyle(SpanStyle(color = colors.primary)) { append(included.joinToString()) }
        if (notText != null) {
            separate()
            withStyle(SpanStyle(color = colors.error)) { append(notText) }
        }
        valueTexts.forEach {
            separate()
            append(it)
        }
    }
}
