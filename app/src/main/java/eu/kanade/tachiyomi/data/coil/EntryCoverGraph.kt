package eu.kanade.tachiyomi.data.coil

import eu.kanade.tachiyomi.data.cache.CoverCache
import mihon.entry.interactions.source.EntryCoverNetworkFeature

/** What the entry cover fetchers and keyer, which the application's image loader creates, read from the graph. */
interface EntryCoverGraph {
    val coverCache: CoverCache
    val entryCoverNetworkFeature: EntryCoverNetworkFeature
}
