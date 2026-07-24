package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryDeepLinkFeature {
    suspend fun resolve(uri: String): EntryDeepLinkResolution
}

sealed interface EntryDeepLinkResolution {
    data object NoMatch : EntryDeepLinkResolution
    data class Resolved(val entry: Entry, val childId: Long? = null) : EntryDeepLinkResolution
    data class Failed(val cause: Throwable) : EntryDeepLinkResolution
}
