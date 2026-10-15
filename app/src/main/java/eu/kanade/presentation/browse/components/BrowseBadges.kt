package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.presentation.kit.content.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
        )
    }
}

@Composable
fun CatalogBadges(isFavorite: Boolean, entryType: EntryType) {
    InLibraryBadge(enabled = isFavorite)
    CatalogTypeBadge(entryType = entryType)
}
