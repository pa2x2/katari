package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = MaterialSymbols.Rounded.CollectionsBookmark,
        )
    }
}

@Composable
fun CatalogBadges(isFavorite: Boolean, entryType: EntryType) {
    InLibraryBadge(enabled = isFavorite)
    CatalogTypeBadge(entryType = entryType)
}
