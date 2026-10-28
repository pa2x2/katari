package eu.kanade.tachiyomi.ui.entry.search

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.BrowseDestination
import mihon.entry.interactions.catalogue.EntryCatalogueFeature

/**
 * Perform a search using the provided query. A global search opens on top where sources can be browsed; any other goes
 * back to the screen the entry was opened from, when that screen searches.
 */
suspend fun performSearch(navigator: Navigator, browseDestination: BrowseDestination, query: String, global: Boolean) {
    if (global) {
        if (browseDestination.isAvailable) navigator.push(browseDestination.globalSearch(query))
        return
    }

    if (navigator.size < 2) {
        return
    }

    val previousController = navigator.items[navigator.size - 2]
    if (previousController is EntrySearchOrigin) {
        navigator.pop()
        previousController.search(query)
    }
}

/**
 * Performs a genre search using the provided genre name.
 */
suspend fun performGenreSearch(
    navigator: Navigator,
    browseDestination: BrowseDestination,
    catalogueFeature: EntryCatalogueFeature,
    genreName: String,
    source: UnifiedSource?,
) {
    if (navigator.size < 2) {
        return
    }

    val previousController = navigator.items[navigator.size - 2]
    val hasCatalogue = source?.let {
        catalogueFeature.description(it.id).catalogue != null
    } == true
    if (previousController is EntryGenreSearchOrigin && hasCatalogue) {
        navigator.pop()
        previousController.searchGenre(genreName)
    } else {
        performSearch(navigator, browseDestination, genreName, global = false)
    }
}
