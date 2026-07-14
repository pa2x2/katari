package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes

/** Resolves source metadata used by the app's presentation models. */
fun UnifiedSource.resolvedSupportedEntryTypes(): Set<EntryType>? {
    return supportedEntryTypes()?.toSet()
}
