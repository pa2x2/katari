package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CollectionsBookmark
import app.katari.presentation.kit.content.Badge
import eu.kanade.tachiyomi.source.entry.EntryType

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
