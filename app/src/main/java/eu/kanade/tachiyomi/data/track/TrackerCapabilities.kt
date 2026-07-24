package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.source.entry.EntryType

interface TrackerCapabilities {
    val supportedEntryTypes: Set<EntryType>
        get() = setOf(EntryType.MANGA)
}

fun TrackerCapabilities.supportsEntryType(entryType: EntryType): Boolean {
    return entryType in supportedEntryTypes
}
