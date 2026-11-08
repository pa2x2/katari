package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ArrowDownward
import app.katari.icons.materialsymbols.rounded.ArrowUpward
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.icons.materialsymbols.rounded.Remove
import app.katari.presentation.kit.chip.InputChip
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeKind
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeLabel
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.description
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.displayText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The applied filter values of the current search, each removable on its own.
 *
 * Tapping a chip opens the filter sheet at that filter; its close icon resets that value and re-runs the search.
 * [unused] when the source searched the query without them: the chips are then dimmed behind a note saying so.
 */
@Composable
internal fun CatalogAppliedFilterChips(
    labels: List<FilterChangeLabel>,
    unused: Boolean,
    onOpenFilters: (FilterChangeLabel) -> Unit,
    onRemove: (FilterChangeLabel) -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (unused) {
            Text(
                text = stringResource(MR.strings.filter_chips_unused_with_query),
                role = TextRole.Supporting,
                tone = ContentTone.Secondary,
            )
        }
        labels.forEach { label -> AppliedFilterChip(label, enabled = !unused, onOpenFilters, onRemove) }
    }
}

@Composable
private fun AppliedFilterChip(
    label: FilterChangeLabel,
    enabled: Boolean,
    onOpenFilters: (FilterChangeLabel) -> Unit,
    onRemove: (FilterChangeLabel) -> Unit,
) {
    val description = label.description()
    InputChip(
        label = label.displayText(),
        onClick = { onOpenFilters(label) },
        modifier = Modifier.semantics { contentDescription = description },
        selected = true,
        icon = when (label.kind) {
            FilterChangeKind.Included -> MaterialSymbols.Rounded.Check
            FilterChangeKind.Excluded -> MaterialSymbols.Rounded.Remove
            FilterChangeKind.Ascending -> MaterialSymbols.Rounded.ArrowUpward
            FilterChangeKind.Descending -> MaterialSymbols.Rounded.ArrowDownward
            FilterChangeKind.Value, FilterChangeKind.SelectedCount -> null
        },
        onRemove = { onRemove(label) },
        enabled = enabled,
        excluded = label.kind == FilterChangeKind.Excluded,
    )
}
