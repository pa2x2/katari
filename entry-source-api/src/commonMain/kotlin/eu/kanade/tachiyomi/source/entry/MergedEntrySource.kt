package eu.kanade.tachiyomi.source.entry

/** Optional capability for sources with merged entries, those whose [SEntry.merged] is set. */
interface MergedEntrySource : UnifiedSource {

    /**
     * Returns the entries [entry] merges, in source order.
     *
     * Each one must be an entry of this source with [entry]'s type, must not be [entry] and must
     * not be a merged entry itself; the host skips any that is. The host keeps them as locked
     * members of [entry]'s merge: users can't remove or reorder them, and the next call decides
     * which members are added or dropped. An empty list is treated as a failure, so a parser
     * failure can't empty a merge.
     */
    suspend fun getMergedEntries(entry: SEntry): List<SEntry>
}
