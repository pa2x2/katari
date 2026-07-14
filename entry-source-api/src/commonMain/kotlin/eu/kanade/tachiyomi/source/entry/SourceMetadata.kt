package eu.kanade.tachiyomi.source.entry

/**
 * Optional descriptive metadata advertised by a source.
 *
 * Metadata helps Katari describe a source before loading its catalogue. It does not constrain the
 * entries returned by the source; each [SEntry.type] remains authoritative.
 */
interface SourceMetadata {

    /**
     * Entry types this source may return.
     *
     * Implementations should include every type the source can supply and return a stable set. An
     * empty set is treated the same as unavailable metadata.
     */
    val supportedEntryTypes: Set<EntryType>
}

/** Returns the source-advertised entry types, or `null` when no useful metadata is available. */
fun UnifiedSource.supportedEntryTypes(): Set<EntryType>? {
    return (this as? SourceMetadata)
        ?.supportedEntryTypes
        ?.takeIf { it.isNotEmpty() }
}
