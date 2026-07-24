package mihon.entry.interactions

import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource

internal fun interface EntryDownloadSourceAccessResolver {
    fun resolve(sourceIds: Set<Long>): EntryDownloadSourceAccess
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
}
