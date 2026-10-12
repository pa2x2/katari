package mihon.entry.interactions.download.host

import mihon.entry.interactions.download.runtime.EntryDownloadSourceAccess
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import tachiyomi.source.local.LocalSource

internal fun interface EntryDownloadSourceAccessResolver {
    fun resolve(sourceIds: Set<Long>): EntryDownloadSourceAccess

    /** [resolve] for an operation that acts on the answer, which mustn't mistake a source still loading for a stub. */
    suspend fun resolveLoaded(sourceIds: Set<Long>): EntryDownloadSourceAccess = resolve(sourceIds)
}

internal class SourceManagerEntryDownloadSourceAccessResolver(
    private val sourceManager: SourceManager,
) : EntryDownloadSourceAccessResolver {
    override fun resolve(sourceIds: Set<Long>): EntryDownloadSourceAccess {
        val hasLocalOrStub = sourceIds.any { sourceId ->
            sourceId == LocalSource.ID || sourceManager.get(sourceId).let { it == null || it is UnifiedStubSource }
        }
        return if (hasLocalOrStub) {
            EntryDownloadSourceAccess.LOCAL_OR_STUB
        } else {
            EntryDownloadSourceAccess.REMOTE
        }
    }

    // A download from a library update notification can start the process, before the sources have loaded
    override suspend fun resolveLoaded(sourceIds: Set<Long>): EntryDownloadSourceAccess {
        sourceManager.awaitInitialized()
        return resolve(sourceIds)
    }
}
