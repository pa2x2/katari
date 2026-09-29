package eu.kanade.tachiyomi.ui.browse.source.browse.filter.change

import androidx.compose.runtime.Composable
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Text for a changed value; direction and inclusion are conveyed by the caller's icon and styling. */
@Composable
internal fun FilterChangeLabel.displayText(): String = when (kind) {
    FilterChangeKind.Included, FilterChangeKind.Excluded -> value
    FilterChangeKind.Ascending, FilterChangeKind.Descending -> value
    FilterChangeKind.SelectedCount -> stringResource(
        MR.strings.filter_change_value,
        name.orEmpty(),
        stringResource(MR.strings.browse_filter_selected_count, value.toInt()),
    )
    FilterChangeKind.Value -> if (name == null) value else stringResource(MR.strings.filter_change_value, name, value)
}

/** Accessible description that states the change, including direction and exclusion. */
@Composable
internal fun FilterChangeLabel.description(): String = when (kind) {
    FilterChangeKind.Included -> stringResource(MR.strings.filter_change_included, value)
    FilterChangeKind.Excluded -> stringResource(MR.strings.filter_change_excluded, value)
    FilterChangeKind.Ascending -> stringResource(MR.strings.filter_change_ascending, value)
    FilterChangeKind.Descending -> stringResource(MR.strings.filter_change_descending, value)
    FilterChangeKind.SelectedCount, FilterChangeKind.Value -> displayText()
}
