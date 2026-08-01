package mihon.entry.interactions.catalogue

import eu.kanade.tachiyomi.source.entry.EntryFilterNavigation

/** Normalized result of loading optional source-defined paged-filter navigation. */
sealed interface EntryCatalogueFilterNavigationResult {
    data class Available(
        val navigation: EntryFilterNavigation,
    ) : EntryCatalogueFilterNavigationResult

    data class Unavailable(
        val reason: EntryCatalogueUnavailableReason,
    ) : EntryCatalogueFilterNavigationResult

    data class Failed(
        val cause: Throwable,
    ) : EntryCatalogueFilterNavigationResult
}
