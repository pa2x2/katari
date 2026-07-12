package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.source.entry.EntryType

enum class TrackMediaType {
    MANGA,
    ANIME,
}

interface TrackerCapabilities {
    val supportedEntryTypes: Set<EntryType>
        get() = setOf(EntryType.MANGA)
}

fun TrackerCapabilities.supportsEntryType(entryType: EntryType): Boolean {
    return entryType in supportedEntryTypes
}

fun Iterable<TrackerCapabilities>.supportedEntryTypes(): Set<EntryType> {
    return flatMapTo(mutableSetOf(), TrackerCapabilities::supportedEntryTypes)
}
