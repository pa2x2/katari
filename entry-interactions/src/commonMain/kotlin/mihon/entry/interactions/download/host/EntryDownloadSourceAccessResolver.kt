package mihon.entry.interactions.download.host

import mihon.entry.interactions.download.runtime.EntryDownloadSourceAccess

internal fun interface EntryDownloadSourceAccessResolver {
    fun resolve(sourceIds: Set<Long>): EntryDownloadSourceAccess

    /** [resolve] for an operation that acts on the answer, which mustn't mistake a source still loading for a stub. */
    suspend fun resolveLoaded(sourceIds: Set<Long>): EntryDownloadSourceAccess = resolve(sourceIds)
}
