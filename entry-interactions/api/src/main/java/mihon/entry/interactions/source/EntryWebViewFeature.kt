package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryWebViewFeature {
    fun resolveEntry(entry: Entry): EntryWebViewResolution
    fun resolveChild(owner: Entry, child: EntryChapter): EntryChildWebViewResolution
    fun resolveHeaders(sourceId: Long): EntryWebViewHeadersResolution
}

sealed interface EntryWebViewResolution {
    val sourceId: Long

    data class Available(
        override val sourceId: Long,
        val url: String,
        val headers: Map<String, String>,
    ) : EntryWebViewResolution

    data class Missing(override val sourceId: Long) : EntryWebViewResolution
    data class Unsupported(override val sourceId: Long) : EntryWebViewResolution
    data class Failed(override val sourceId: Long, val cause: Throwable) : EntryWebViewResolution
}

sealed interface EntryChildWebViewResolution {
    val sourceId: Long

    data class Available(
        override val sourceId: Long,
        val url: String,
    ) : EntryChildWebViewResolution

    data class Missing(override val sourceId: Long) : EntryChildWebViewResolution
    data class Unsupported(override val sourceId: Long) : EntryChildWebViewResolution
    data class Failed(override val sourceId: Long, val cause: Throwable) : EntryChildWebViewResolution
}

sealed interface EntryWebViewHeadersResolution {
    data class Available(val headers: Map<String, String>) : EntryWebViewHeadersResolution
    data object Missing : EntryWebViewHeadersResolution
    data object Unsupported : EntryWebViewHeadersResolution
    data class Failed(val cause: Throwable) : EntryWebViewHeadersResolution
}
