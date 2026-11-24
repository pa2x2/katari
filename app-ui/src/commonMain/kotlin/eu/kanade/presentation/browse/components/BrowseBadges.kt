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

/** A source's cover badges: whether it's in the library, and [entryType] unless null, as [catalogShowsTypeBadge] says. */
@Composable
fun CatalogBadges(isFavorite: Boolean, entryType: EntryType?) {
    InLibraryBadge(enabled = isFavorite)
    entryType?.let { CatalogTypeBadge(entryType = it) }
}

/**
 * Whether the covers of a source offering [entryTypes] (null when it doesn't say) show their entry type: not when the
 * source has one type, which every cover would repeat, nor while the person hides type badges ([typeBadges]).
 */
fun catalogShowsTypeBadge(entryTypes: Set<EntryType>?, typeBadges: Boolean): Boolean =
    typeBadges && (entryTypes == null || entryTypes.size > 1)
