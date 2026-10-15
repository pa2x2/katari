package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Verified
import mihon.icons.materialsymbols.roundedfilled.Favorite
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The catalogue's listings (popular, latest and, when the source can filter, a filtered search) under its bar. */
@Composable
internal fun CatalogListingChips(
    listing: CatalogViewModel.Listing,
    supportsLatest: Boolean,
    showFilterChip: Boolean,
    changedFilterCount: Int,
    onListingChange: (CatalogViewModel.Listing) -> Unit,
    onFilterChipClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        FilterChip(
            label = stringResource(MR.strings.popular),
            selected = listing == CatalogViewModel.Listing.Popular,
            onClick = { onListingChange(CatalogViewModel.Listing.Popular) },
            icon = MaterialSymbols.RoundedFilled.Favorite,
        )
        if (supportsLatest) {
            FilterChip(
                label = stringResource(MR.strings.latest),
                selected = listing == CatalogViewModel.Listing.Latest,
                onClick = { onListingChange(CatalogViewModel.Listing.Latest) },
                icon = MaterialSymbols.Rounded.Verified,
            )
        }
        if (showFilterChip) {
            CatalogFilterChip(
                selected = listing is CatalogViewModel.Listing.Search,
                changedCount = changedFilterCount,
                onClick = onFilterChipClick,
            )
        }
    }
}
