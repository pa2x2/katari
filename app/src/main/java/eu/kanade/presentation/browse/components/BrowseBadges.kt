package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}

@Composable
fun CatalogBadges(isFavorite: Boolean, entryType: EntryType) {
    InLibraryBadge(enabled = isFavorite)
    CatalogTypeBadge(entryType = entryType)
}
