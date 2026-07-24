package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import mihon.entry.interactions.EntryCatalogueFeature
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun ContentTypeFilter.matches(extension: Extension): Boolean {
    return matches(extension.supportedEntryTypesForDisplay())
}

internal fun Extension.supportedEntryTypesForDisplay(
    installedSourceTypes: (UnifiedSource) -> Set<EntryType>? = { source ->
        Injekt.get<EntryCatalogueFeature>().description(source.id).supportedEntryTypes
    },
): Set<EntryType>? {
    val advertisedTypes = when (this) {
        is Extension.Installed ->
            sources
                .flatMapTo(linkedSetOf()) { installedSourceTypes(it).orEmpty() }
        is Extension.Available ->
            sources
                .flatMapTo(linkedSetOf()) { it.supportedEntryTypes.orEmpty() }
        is Extension.Untrusted -> emptySet()
    }

    return advertisedTypes.takeIf { it.isNotEmpty() }
}

internal fun Extension.languagesForDisplay(): Set<String> {
    return when (this) {
        is Extension.Installed -> setOf(this.lang)
        is Extension.Available -> sources.mapTo(linkedSetOf()) { it.lang }
        is Extension.Untrusted -> setOfNotNull(this.lang)
    }
}
