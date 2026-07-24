package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType

/** Factual source description projected by the catalogue Feature. */
data class EntrySourceDescription(
    val language: String,
    val supportedEntryTypes: Set<EntryType>?,
    val itemOrientation: EntryItemOrientation,
    val catalogue: EntryCatalogueDescription?,
)

/** Catalogue-specific facts. Absence means that the source does not currently provide a catalogue. */
data class EntryCatalogueDescription(
    val supportsLatest: Boolean,
)
