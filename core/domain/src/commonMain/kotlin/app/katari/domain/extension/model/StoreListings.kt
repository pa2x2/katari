package app.katari.domain.extension.model

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * What a set of stores list: the extensions of the stores that could be read, and the stores that couldn't, so an
 * extension missing from [available] isn't taken to be in no store while a store was down (D113).
 */
data class StoreListings(val available: List<Extension.Available>, val unreadable: List<ExtensionStore>)
