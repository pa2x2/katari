package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.control.ConnectedToggleGroup
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The catalogue's listings under its bar: popular and latest as a connected pair and, when the source can filter, a
 * button for the filtered search at the end. While filtered results show, the button is the selected listing.
 */
@Composable
internal fun CatalogListingControls(
    listing: CatalogViewModel.Listing,
    supportsLatest: Boolean,
    showFilterButton: Boolean,
    changedFilterCount: Int,
    onListingChange: (CatalogViewModel.Listing) -> Unit,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectedToggleGroup(
            options = listOfNotNull(
                CatalogViewModel.Listing.Popular,
                CatalogViewModel.Listing.Latest.takeIf { supportsLatest },
            ),
            selected = { it == listing },
            onClick = onListingChange,
            label = {
                stringResource(if (it == CatalogViewModel.Listing.Latest) MR.strings.latest else MR.strings.popular)
            },
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showFilterButton) {
            CatalogFilterButton(
                selected = listing is CatalogViewModel.Listing.Search,
                changedCount = changedFilterCount,
                onClick = onFilterClick,
            )
        }
    }
}
