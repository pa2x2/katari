package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.kit.chip.FilterChip
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Listing chip for the filtered search, showing how many filters differ from the source defaults. */
@Composable
internal fun CatalogFilterChip(
    selected: Boolean,
    changedCount: Int,
    onClick: () -> Unit,
) {
    val description = stringResource(MR.strings.filter_changed_count_description, changedCount)
    FilterChip(
        label = stringResource(MR.strings.action_filter),
        selected = selected,
        onClick = onClick,
        modifier = if (changedCount > 0) Modifier.semantics { stateDescription = description } else Modifier,
        icon = MaterialSymbols.Rounded.FilterList,
        count = changedCount.takeIf { it > 0 },
    )
}
