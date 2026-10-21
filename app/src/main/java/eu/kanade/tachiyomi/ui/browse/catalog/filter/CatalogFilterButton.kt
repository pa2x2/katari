package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Opens the filtered search, counting the filters that differ from the source defaults. It is filled while the
 * filtered results show, as the listing in effect, and tonal otherwise.
 */
@Composable
internal fun CatalogFilterButton(
    selected: Boolean,
    changedCount: Int,
    onClick: () -> Unit,
) {
    val description = stringResource(MR.strings.filter_changed_count_description, changedCount)
    Button(
        label = if (changedCount > 0) {
            stringResource(MR.strings.action_filter_count, changedCount)
        } else {
            stringResource(MR.strings.action_filter)
        },
        onClick = onClick,
        modifier = if (changedCount > 0) Modifier.semantics { stateDescription = description } else Modifier,
        style = if (selected) ButtonStyle.Filled else ButtonStyle.Tonal,
        icon = MaterialSymbols.Rounded.FilterList,
    )
}
