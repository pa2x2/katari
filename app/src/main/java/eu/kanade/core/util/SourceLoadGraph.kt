package eu.kanade.core.util

import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import tachiyomi.domain.source.service.SourceManager

/** Graph entry points of the composables that wait for sources to load. */
interface SourceLoadGraph {
    val sourceManager: SourceManager

    val entryCatalogueFeature: EntryCatalogueFeature
}
