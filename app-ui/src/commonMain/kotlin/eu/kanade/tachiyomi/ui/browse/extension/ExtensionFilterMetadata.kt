package eu.kanade.tachiyomi.ui.browse.extension

import app.katari.entry.interactions.catalogue.EntryCatalogueFeature
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter

internal fun ContentTypeFilter.matches(extension: Extension, catalogue: EntryCatalogueFeature): Boolean {
    return matches(extension.supportedEntryTypesForDisplay(catalogue))
}

internal fun Extension.supportedEntryTypesForDisplay(catalogue: EntryCatalogueFeature): Set<EntryType>? {
    val advertisedTypes = when (this) {
        is Extension.Loaded ->
            sources
                .flatMapTo(linkedSetOf()) { catalogue.description(it.id).supportedEntryTypes.orEmpty() }
        is Extension.Available ->
            sources
                .flatMapTo(linkedSetOf()) { it.supportedEntryTypes.orEmpty() }
        is Extension.NotLoaded -> emptySet()
    }

    return advertisedTypes.takeIf { it.isNotEmpty() }
}

internal fun Extension.languagesForDisplay(): Set<String> {
    return when (this) {
        is Extension.Loaded -> setOf(this.lang)
        is Extension.Available -> sources.mapTo(linkedSetOf()) { it.lang }
        is Extension.NotLoaded -> setOfNotNull(this.lang)
    }
}
