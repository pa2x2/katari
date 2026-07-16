package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import tachiyomi.domain.source.service.resolvedSupportedEntryTypes

internal fun ContentTypeFilter.matches(extension: Extension): Boolean {
    return matches(extension.supportedEntryTypesForDisplay())
}

internal fun Extension.supportedEntryTypesForDisplay(): Set<EntryType>? {
    val advertisedTypes = when (this) {
        is Extension.Installed ->
            sources
                .flatMapTo(linkedSetOf()) { it.resolvedSupportedEntryTypes().orEmpty() }
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
