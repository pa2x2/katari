package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources that can provide entries related to another entry.
 *
 * Related entries are source-defined and may represent recommendations, similar content, sequels,
 * adaptations, or another relationship exposed by the provider. Each returned [SEntry.type] is
 * authoritative, so a source may return entries of different types.
 */
interface RelatedEntriesSource : UnifiedSource {

    /**
     * Returns entries related to [entry] in groups, both in source-defined display order. A source that doesn't tell
     * kinds of relation apart returns one group with a null title.
     */
    suspend fun getRelatedEntries(entry: SEntry): List<RelatedEntryGroup>
}
