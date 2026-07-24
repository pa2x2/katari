package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType

internal data class EntryDownloadActionTarget(
    val type: EntryType,
    val sourceAccess: EntryDownloadSourceAccess,
)

internal enum class EntryDownloadSourceAccess {
    REMOTE,
    LOCAL_OR_STUB,
}
